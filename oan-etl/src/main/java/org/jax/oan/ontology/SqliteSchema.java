package org.jax.oan.ontology;

import java.util.List;

public final class SqliteSchema {

	private static final List<String> TABLES = List.of(
			"CREATE TABLE phenotype (id TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL)",
			"CREATE TABLE disease (id TEXT PRIMARY KEY, name TEXT NOT NULL, mondo_id TEXT NOT NULL DEFAULT '', description TEXT NOT NULL DEFAULT 'No disease description found.')",
			"CREATE TABLE gene (id TEXT PRIMARY KEY, name TEXT NOT NULL)",
			"CREATE TABLE assay (id TEXT PRIMARY KEY, name TEXT NOT NULL, scale TEXT)",
			"CREATE TABLE medical_action (id TEXT PRIMARY KEY, name TEXT NOT NULL)",
			"CREATE TABLE disease_phenotype (disease_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, sex TEXT NOT NULL DEFAULT '', onset TEXT NOT NULL DEFAULT '', frequency TEXT NOT NULL DEFAULT '', sources TEXT NOT NULL)",
			"CREATE TABLE disease_gene (disease_id TEXT NOT NULL, gene_id TEXT NOT NULL, PRIMARY KEY (disease_id, gene_id))",
			"CREATE TABLE gene_phenotype (gene_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, PRIMARY KEY (gene_id, phenotype_id))",
			"CREATE TABLE assay_phenotype (assay_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, outcome TEXT, PRIMARY KEY (assay_id, phenotype_id))",
			"CREATE TABLE medical_action_target (medical_action_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, disease_id TEXT NOT NULL, relation TEXT NOT NULL, PRIMARY KEY (medical_action_id, phenotype_id, disease_id, relation))",
			"CREATE TABLE medical_action_annotation (medical_action_id TEXT NOT NULL, disease_id TEXT NOT NULL, phenotype_id TEXT NOT NULL, evidence TEXT NOT NULL, author TEXT, source TEXT, extension_id TEXT, extension_name TEXT)",
			"CREATE TABLE phenotype_child (parent_id TEXT NOT NULL, child_id TEXT NOT NULL, PRIMARY KEY (parent_id, child_id))"
	);

	private static final List<String> INDEXES = List.of(
			"CREATE INDEX ix_disease_mondo ON disease(mondo_id)",
			"CREATE INDEX ix_dp_disease ON disease_phenotype(disease_id)",
			"CREATE INDEX ix_dp_phenotype ON disease_phenotype(phenotype_id)",
			"CREATE INDEX ix_dg_gene ON disease_gene(gene_id)",
			"CREATE INDEX ix_gp_phenotype ON gene_phenotype(phenotype_id)",
			"CREATE INDEX ix_ap_phenotype ON assay_phenotype(phenotype_id)",
			"CREATE INDEX ix_mat_disease ON medical_action_target(disease_id)",
			"CREATE INDEX ix_mat_phenotype ON medical_action_target(phenotype_id)",
			"CREATE INDEX ix_maa_action_pheno ON medical_action_annotation(medical_action_id, phenotype_id)"
	);

	private SqliteSchema() {}

	public static void createTables(SqliteWriter writer) {
		TABLES.forEach(writer::execute);
	}

	public static void createIndexes(SqliteWriter writer) {
		INDEXES.forEach(writer::execute);
	}
}
