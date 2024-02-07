package org.jax.oan.ontology;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import java.util.Collection;

public class GraphDatabaseWriter implements GraphWriter, SessionAware {
	private final Driver driver;

	public GraphDatabaseWriter(Driver driver){
		this.driver = driver;
	}

	public void truncate(){
		int count = 1;
		while (count > 0) {
			driver.session().executeWriteWithoutResult(tx -> tx.run("MATCH (x) WITH x LIMIT 1000 DETACH DELETE x"));
			count = driver.session().executeWrite(tx -> tx.run("MATCH (x) RETURN COUNT(x)").single().values().get(0).asInt());
		}
	}

	@Override
	public void write(Collection<Query> queries) {
		try(Session session = session()){
			Transaction tx = session.beginTransaction();
			int i = 0;
			for (Query query: queries){
				tx.run(query);
				if (i % 500 == 0){
					tx.commit();
					tx = session.beginTransaction();
				}
				i++;
			}
			if (tx.isOpen()) {
				tx.commit();
			}
			tx.close();
		}
	}

	@Override
	public Session session() {
		return this.driver.session();
	}
}
