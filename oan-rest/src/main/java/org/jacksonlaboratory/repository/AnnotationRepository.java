package org.jacksonlaboratory.repository;


import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Singleton
public class AnnotationRepository {

	private final Driver driver;

	public AnnotationRepository(Driver driver) {
		this.driver = driver;
	}

//	public void findDiseasesByTerm(TermId termId){
//		try (Session session = driver.session()) {
//			List<String> names = new ArrayList<>();
//			var result = session.run("MATCH (a:Person) RETURN a.name ORDER BY a.name");
//			while (result.hasNext()) {
//				names.add(result.next().get(0).asString());
//			}
//			return names;
//
//		}
//	}
}
