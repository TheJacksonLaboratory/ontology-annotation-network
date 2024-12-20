#!/bin/bash
# Exit if command fails
set -e

# OMIM 2 GENE
wget -P $1 --no-use-server-timestamps ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/mim2gene_medgen

# HPO OBO
wget -P $1 --no-use-server-timestamps http://purl.obolibrary.org/obo/hp/hp-simple-non-classified.json

# HPO OBO
wget -P $1 --no-use-server-timestamps http://purl.obolibrary.org/obo/mondo/mondo-base.json

# MAXO OBO
wget -P $1 --no-use-server-timestamps https://raw.githubusercontent.com/monarch-initiative/MAxO/master/maxo.json

# ORPHANET 2 GENE
wget -P $1 --no-use-server-timestamps http://www.orphadata.org/data/xml/en_product6.xml

# GENE INFO
wget -P $1 --no-use-server-timestamps https://storage.googleapis.com/public-download-files/hgnc/tsv/tsv/hgnc_complete_set.txt

# PHENOTYPE HPOA
wget https://github.com/obophenotype/human-phenotype-ontology/releases/latest/download/phenotype.hpoa -O $1phenotype.hpoa

# EXIT
exit 0;
