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
A CLI module that loads phenotypes, diseases, genes, and assays into a SQLite database --
no database server required.

Running the load
```
    # Fetch input data
    bash update.sh data/

    # Build the SQLite artifact (-t writes a fresh file, deleting any existing one at
    # the output path first)
    java -jar <etl-jar> -d data/ -o oan.db -t
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
