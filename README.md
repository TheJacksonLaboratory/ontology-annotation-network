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

#### OAN-ETL
    A module to load our graph data which includes phenotypes, diseases, genes, assays. There
    are multiple loaders for the different ontologies that are supported

Running

```
    ./gradlew oan-etl:run
```

Testing

```
    ./gradlew oan-etl:test
```

#### OAN-REST
    A module that exposes our graph via a REST-API. This will be used for the hpo web application
    and deployed to google cloud.

Running

```
    ./gradlew oan-rest:run
```

Testing

```
    ./gradlew oan-rest:test
```
