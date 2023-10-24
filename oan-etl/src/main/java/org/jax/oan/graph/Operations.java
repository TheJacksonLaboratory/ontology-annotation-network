package org.jax.oan.graph;

import jakarta.inject.Singleton;
import org.jax.oan.core.OntologyModule;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Operations {
	private static final Logger logger = LoggerFactory.getLogger(Operations.class);
	private final Driver driver;

	public Operations(Driver driver) {
		this.driver = driver;
	}

	public void truncate(){
		logger.info("Truncating Neo4J Graph...");
		int count = 1;
		while (count > 0) {
			driver.session().executeWriteWithoutResult(tx -> tx.run("MATCH (x) WITH x LIMIT 1000 DETACH DELETE x"));
			count = driver.session().executeWrite(tx -> tx.run("MATCH (x) RETURN COUNT(x)").single().values().get(0).asInt());
		}
	}

	public void createIndexes(OntologyModule ontologyModule){
		Transaction tx = driver.session().beginTransaction();
		if (ontologyModule.equals(OntologyModule.HPO)){
			logger.info("Creating Indexes...");
			tx.run("CREATE INDEX phenotype_id FOR (n:Phenotype) ON (n.id)");
			tx.run("CREATE INDEX disease_id FOR (n:Disease) ON (n.id)");
			tx.run("CREATE INDEX gene_id FOR (n:Gene) ON (n.id)");
			tx.run("CREATE INDEX assay_id FOR (n:Assay) ON (n.id)");
			logger.info("Done...");
		}
		tx.commit();
		tx.close();
	}

	public void dropIndexes(OntologyModule ontologyModule){
		Transaction tx = driver.session().beginTransaction();
		if(ontologyModule.equals(OntologyModule.HPO)){
			logger.info("Dropping Indexes...");
			tx.run("DROP INDEX phenotype_id IF EXISTS");
			tx.run("DROP INDEX disease_id IF EXISTS");
			tx.run("DROP INDEX gene_id IF EXISTS");
			tx.run("DROP INDEX assay_id IF EXISTS");
			logger.info("Done...");
		}
		tx.commit();
		tx.close();
	}
}
