package org.jax.oan.ontology;

import io.micronaut.context.annotation.Context;
import org.jax.oan.exception.OntologyAnnotationNetworkException;
import org.jax.oan.exception.OntologyAnnotationNetworkRuntimeException;
import org.jax.oan.graph.Operations;
import org.jax.oan.core.OntologyModule;
import org.monarchinitiative.phenol.annotations.formats.AnnotationReference;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoOnset;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoAnnotationLine;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoaDiseaseDataContainer;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoaDiseaseDataLoader;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.neo4j.driver.Values.parameters;

@Context
public class HpoGraphLoader implements GraphLoader {

	private final Driver driver;
	private final Operations operations;
	private static final Logger logger = LoggerFactory.getLogger(HpoGraphLoader.class);

	public HpoGraphLoader(Driver driver, Operations operations) {
		this.driver = driver;
		this.operations = operations;
	}
	@Override
	public void load(Path hpoDataDirectory) throws IOException, OntologyAnnotationNetworkException {
		final HpoDataResolver dataResolver = HpoDataResolver.of(hpoDataDirectory);
		final Ontology hpoOntology = OntologyLoader.loadOntology(dataResolver.hpJson().toFile());
		final HpoaDiseaseDataContainer diseases = HpoaDiseaseDataLoader.of(Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.ORPHANET)).loadDiseaseData(dataResolver.phenotypeAnnotations());
		final HpoAssociationData associations = HpoAssociationData.builder(hpoOntology).orphaToGenePath(dataResolver.orpha2Gene()).mim2GeneMedgen(dataResolver.mim2geneMedgen())
				.hpoDiseases(diseases).hgncCompleteSetArchive(dataResolver.hgncCompleteSet()).build();
		List<TermId> phenotypes = diseases.stream().flatMap(d -> d.annotationLines().stream().map(HpoAnnotationLine::phenotypeTermId)).distinct().toList();
		operations.dropIndexes(OntologyModule.HPO);
		try (Session session = driver.session()) {
			phenotypes(session, phenotypes, hpoOntology);
			diseases(session, diseases);
			genes(session, associations);
			operations.createIndexes(OntologyModule.HPO);
			diseaseToPhenotype(session, diseases, hpoOntology);
			diseaseToGene(session, associations);
			assayToPhenotype(session, dataResolver.loinc());
		}
	}

	static void assayToPhenotype(Session session, Path loinc){
		logger.info("Loading Assay Relationships...");
		Transaction tx = session.beginTransaction();
		try (BufferedReader reader = new BufferedReader(new FileReader(loinc.toFile()))) {
			String line;
			reader.readLine();
			while ((line = reader.readLine()) != null) {
				String[] fields = line.split("\t");
				tx.run("MERGE (a:Assay {id: $id, name: $name, scale: $scale})",
						parameters("id", fields[1], "name",fields[0], "scale", fields[2]));
				tx.run("MATCH (p:Phenotype {id: $phenotypeId}), (a:Assay {id: $assayId}) " +
								"CREATE (a)-[:MEASURES {outcome: $outcome}]->(p)",
						parameters("assayId", fields[1], "outcome", fields[3], "phenotypeId", fields[4]));
			}
			logger.info("Done.");
			tx.commit();
			tx.close();
		} catch (IOException e) {
			throw new OntologyAnnotationNetworkRuntimeException("There was a problem with the required assay file format.");
		}
	}

	static void diseaseToGene(Session session, HpoAssociationData associations){
		Transaction tx = session.beginTransaction();
		logger.info("Loading Disease to Gene Relationships...");
		associations.associations().diseaseIdToGeneAssociations().forEach((key, value) -> value.forEach(x ->
				tx.run("MATCH (d:Disease {id: $diseaseId}), (g:Gene {id: $geneId}) " +
								"CREATE (d)-[:EXPRESSES]->(g)",
						parameters("diseaseId", key.toString(),
								"geneId", x.geneIdentifier().id().toString()))
		));
		logger.info("Done.");
		tx.commit();
		tx.close();
	}

	static void diseaseToPhenotype(Session session, HpoaDiseaseDataContainer diseases, Ontology ontology){
		Transaction tx = session.beginTransaction();
		logger.info("Loading Disease to Phenotype Relationships...");
		diseases.diseaseData().stream().flatMap(d -> d.annotationLines().stream()).forEach(line -> {
			String onset = line.onset().map(HpoOnset::id).map(TermId::toString).orElse("");
			String frequency = formatFrequency(line.frequency(), ontology);
			String sources = formatSources(line.annotationReferences());
			String sex;
			if (line.sex() != null) {
				sex = line.sex().toString();
			} else {
				sex = "";
			}
			tx.run("MATCH (d:Disease {id: $diseaseId}), (p:Phenotype {id: $phenotypeId})" +
							"MERGE (d)-[:MANIFESTS]->(p)<-[:WITH_METADATA {context: $diseaseId}]-(pm: PhenotypeMetadata {onset: $onset, frequency: $frequency, sex: $sex," +
							"sources: $sources})",
					parameters(
							"diseaseId", line.diseaseId().toString(),
							"phenotypeId", line.phenotypeTermId().toString(),
							"onset", onset, "frequency", frequency, "sex", sex,
							"sources", sources
					)
			);
		});
		logger.info("Done.");
		tx.commit();
		tx.close();
	}

	static void phenotypes(Session session, List<TermId> termIds, Ontology ontology){
		Transaction tx = session.beginTransaction();
		logger.info("Loading Phenotypes...");
		for (TermId termId: termIds){
			Optional<String> label = ontology.getTermLabel(termId);
			label.ifPresent(s -> tx.run("CREATE (p:Phenotype {id: $id, name: $name})",
					parameters("id", termId.toString(), "name", s)));
		}
		logger.info("Done.");
		tx.commit();
		tx.close();
	}

	static void genes(Session session, HpoAssociationData associations){
		Transaction tx = session.beginTransaction();
		logger.info("Loading Genes...");
		associations.getGeneIdentifiers().forEach( g ->
				tx.run("CREATE (g: Gene {id: $id, name: $name})",
						parameters("id", g.id().toString(), "name", g.symbol()))
		);
		logger.info("Done.");
		tx.commit();
		tx.close();
	}

	static void diseases(Session session, HpoaDiseaseDataContainer diseases){
		Transaction tx = session.beginTransaction();
		logger.info("Loading Diseases...");
		diseases.diseaseData().forEach(d ->
				tx.run("CREATE (d:Disease {id: $id, name: $name})",
						parameters("name", d.name(), "id", d.id().toString()))
		);
		logger.info("Done.");
		tx.commit();
		tx.close();
	}

	static String formatSources(List<AnnotationReference> sources){
		final String joinedSources = sources.stream().map(AnnotationReference::id).map(TermId::toString).collect(Collectors.joining(","));
		return  joinedSources.length() > 1 ? joinedSources : "UNKNOWN";
	}

	static String formatFrequency(String frequency, Ontology ontology){
		if(frequency.startsWith("HP:")){
			return ontology.getTermLabel(TermId.of(frequency)).orElse("");
		} else if(frequency.equals("n/a") || frequency.equals("")) {
			return "";
		}
		return frequency;
	}
}
