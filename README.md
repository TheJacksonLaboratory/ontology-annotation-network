## Ontology Annotation Network

*A graph to maintain our ontology annotation connections.*

Java 17

Micronaut 4.1.2
- [User Guide](https://docs.micronaut.io/4.1.2/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.1.2/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.1.2/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

### Modules

Pre-package modules using
```
    ./mvnw clean package -pl <module> -am
```

#### OAN-ETL
    A ontologyModule to load our graph data which includes phenotypes, diseases, genes, assays. There
    are multiple loaders for the different ontologies that are supported

Prequisite
```
 docker pull neo4j:community-bullseye
 docker run -d -p7474:7474 -p7687:7687 -v ./neo4j/data:/data  --env NEO4J_AUTH=neo4j/password neo4j:community-bullseye
```

Running (~4min)

```
    # Create data
    bash update.sh
    # Load data into graph
    java -jar oan-etl/target/<jar>
```

Testing

```
    ./mvnw clean test -pl oan-etl -am
```

#### OAN-REST
    A ontologyModule that exposes our graph via a REST-API. This will be used for the hpo web application
    and deployed to google cloud.

Running

```
     java -jar oan-rest/target/<jar>
```

Testing

```
     ./mvnw clean test -pl oan-rest -am
```
