package org.jax.oan.graph;

import jakarta.inject.Singleton;
import org.jax.oan.core.OntologyModule;
import org.jax.oan.ontology.GraphWriter;
import org.jax.oan.ontology.SessionAware;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;

@Singleton
public class GraphDatabaseOperations implements GraphOperations {
	private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseOperations.class);

	private final GraphWriter graphWriter;

	public GraphDatabaseOperations(GraphWriter graphWriter) {
		this.graphWriter = graphWriter;
	}

	@Override
	public GraphWriter graphWriter() {
		return graphWriter;
	}

	public void createIndexes(OntologyModule ontologyModule){
		ArrayList<Query> queries = new ArrayList<>(Collections.emptyList());
		if (ontologyModule.equals(OntologyModule.HPO)){
			logger.info("Creating Indexes...");
			queries.add(new Query("CREATE INDEX phenotype_id FOR (n:Phenotype) ON (n.id)"));
			queries.add(new Query("CREATE INDEX disease_id FOR (n:Disease) ON (n.id)"));
			queries.add(new Query("CREATE INDEX gene_id FOR (n:Gene) ON (n.id)"));
			queries.add(new Query("CREATE INDEX assay_id FOR (n:Assay) ON (n.id)"));
			queries.add(new Query("CREATE INDEX p_annotation_id FOR (n:PhenotypeAnnotation) ON (n.onset, n.frequency, n.sex, n.sources)"));
			graphWriter().write(queries);
			logger.info("Done.");
		}
	}

	public void dropIndexes(OntologyModule ontologyModule){
		ArrayList<Query> queries = new ArrayList<>(Collections.emptyList());
		if(ontologyModule.equals(OntologyModule.HPO)){
			logger.info("Dropping Indexes...");
			queries.add(new Query("DROP INDEX phenotype_id IF EXISTS"));
			queries.add(new Query("DROP INDEX disease_id IF EXISTS"));
			queries.add(new Query("DROP INDEX gene_id IF EXISTS"));
			queries.add(new Query("DROP INDEX assay_id IF EXISTS"));
			queries.add(new Query("DROP INDEX p_annotation_id IF EXISTS"));
			graphWriter().write(queries);
			logger.info("Done.");
		}
	}
}
