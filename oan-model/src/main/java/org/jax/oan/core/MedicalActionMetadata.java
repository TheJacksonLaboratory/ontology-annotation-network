package org.jax.oan.core;

public record MedicalActionMetadata(String sourceId, Evidence evidence, Extension extension, MedicalActionRelation medicalActionRelation, String author) {
}
