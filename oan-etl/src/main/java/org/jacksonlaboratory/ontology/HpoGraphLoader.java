package org.jacksonlaboratory.ontology;

import io.micronaut.context.annotation.Context;
import org.jacksonlaboratory.graph.Operations;
import org.jacksonlaboratory.model.OntologyModule;
import org.monarchinitiative.phenol.annotations.assoc.MissingPhenolResourceException;
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
import java.io.File;
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
	public void load(String folder) throws IOException, MissingPhenolResourceException {
		final File ontologyFile = new File(String.format("data/%s-simple-non-classified.json", "hp"));
		final Ontology hpoOntology = OntologyLoader.loadOntology(ontologyFile);
		final Path hgncPath = new File("data/hgnc_complete_set.txt").toPath();
		final Path omimToGenePath = new File("data/mim2gene_medgen").toPath();
		final Path hpoaFilePath =  new File("data/phenotype.hpoa").toPath();
		final Path orphaToGenePath = new File("data/en_product6.xml").toPath();
		final File loincPath = new File("data/loinc2hpo-annotations-merged.tsv");
		final HpoaDiseaseDataContainer diseases = HpoaDiseaseDataLoader.of(Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.ORPHANET)).loadDiseaseData(hpoaFilePath);
		final HpoAssociationData associations = HpoAssociationData.builder(hpoOntology).orphaToGenePath(orphaToGenePath).mim2GeneMedgen(omimToGenePath)
				.hpoDiseases(diseases).hgncCompleteSetArchive(hgncPath).build();
		List<TermId> phenotypes = diseases.stream().flatMap(d -> d.annotationLines().stream().map(HpoAnnotationLine::phenotypeTermId)).distinct().toList();
		operations.dropIndexes(OntologyModule.HPO);
		try (Session session = driver.session()) {
			phenotypes(session, phenotypes, hpoOntology);
			diseases(session, diseases);
			genes(session, associations);
			operations.createIndexes(OntologyModule.HPO);
			diseaseToPhenotype(session, diseases, hpoOntology);
			diseaseToGene(session, associations);
			assayToPhenotype(session, loincPath);
		}
	}

	protected void assayToPhenotype(Session session, File loinc){
		logger.info("Loading Assay Relationships...");
		Transaction tx = session.beginTransaction();
		try (BufferedReader reader = new BufferedReader(new FileReader(loinc))) {
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
			// TODO: throw oan-error
			e.printStackTrace();
		}
	}

	protected void diseaseToGene(Session session, HpoAssociationData associations){
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

	protected void diseaseToPhenotype(Session session, HpoaDiseaseDataContainer diseases, Ontology ontology){
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

	protected void phenotypes(Session session, List<TermId> termIds, Ontology ontology){
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

	protected void genes(Session session, HpoAssociationData associations){
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

	protected void diseases(Session session, HpoaDiseaseDataContainer diseases){
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

	protected String formatSources(List<AnnotationReference> sources){
		final String joinedSources = sources.stream().map(AnnotationReference::id).map(TermId::toString).collect(Collectors.joining(","));
		return  joinedSources.length() > 1 ? joinedSources : "UNKNOWN";
	}

	protected String formatFrequency(String frequency, Ontology ontology){
		if(frequency.startsWith("HP:")){
			return ontology.getTermLabel(TermId.of(frequency)).orElse("");
		} else if(frequency.equals("n/a") || frequency.equals("")) {
			return "";
		}
		return frequency;
	}
}
