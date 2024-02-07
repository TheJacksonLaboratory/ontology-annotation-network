package org.jax.oan.core;

import java.util.*;

public class AlternativePrefix {
	public static String from(String prefix){
		Map<String, String> prefixMap = new HashMap<>();
		prefixMap.put("ORPHA", "Orphanet");
		if (prefixMap.containsKey(prefix)){
			return prefixMap.get(prefix);
		}
		return "NULL";
	}
}
