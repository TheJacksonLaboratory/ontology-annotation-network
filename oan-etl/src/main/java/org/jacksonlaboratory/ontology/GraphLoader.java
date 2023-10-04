package org.jacksonlaboratory.ontology;

import org.monarchinitiative.phenol.annotations.assoc.MissingPhenolResourceException;

import java.io.IOException;

public interface GraphLoader {
	void load(String folder) throws IOException, MissingPhenolResourceException;
}
