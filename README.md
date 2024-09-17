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

[Get the latest artifacts](https://github.com/TheJacksonLaboratory/ontology-annotation-network/releases/latest)

or

Package modules using
```
    ./mvnw clean package -pl <module> -am
```

#### OAN-ETL
An ontology module to load our graph data which includes phenotypes, diseases, genes, assays.

Start neo4j
```
 docker pull neo4j:community-bullseye
 docker run -d -p7474:7474 -p7687:7687 -v ./neo4j/data:/data  --env NEO4J_AUTH=neo4j/password neo4j:community-bullseye
```

Running data load (~4min)
```
    # Create data
    bash update.sh
    
    # Load data into graph
    java -jar <etl-jar> -d=data/
```

Testing

```
    ./mvnw clean test -pl oan-etl -am
```

#### OAN-REST

An ontology module that exposes our graph via a REST-API.

Running

```
     java -jar <rest-jar>
```

Testing

```
     ./mvnw clean test -pl oan-rest -am
```

Test Reporting

```
    ./mvnw jacoco:report
```
