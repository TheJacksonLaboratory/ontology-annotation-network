package org.jax.oan;

import org.jax.oan.core.Assay;
import org.jax.oan.core.Disease;
import org.jax.oan.core.Gene;
import org.jax.oan.core.Phenotype;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public class TestData {
	public static List<Disease> diseases(){
		return List.of(
				new Disease(TermId.of("MONDO:099233"),"Really bad one"),
				new Disease(TermId.of("DECIPHER:434444"),"Kinda bad one")
		);
	}

	public static List<Phenotype> phenotypes(){
		return List.of(
				new Phenotype(TermId.of("HP:099233"),"Long legs"),
				new Phenotype(TermId.of("HP:434444"),"Big bicep small arm")
		);
	}

	public static List<Assay> assays(){
		return List.of(
				new Assay(TermId.of("LOINC:55555"),"Special bicep test")
		);
	}

	public static List<Gene> genes(){
		return List.of(
				new Gene(TermId.of("NCBIGene:00093"),"TP4"),
				new Gene(TermId.of("NCBIGene:02002"),"YZ")
		);
	}
}
