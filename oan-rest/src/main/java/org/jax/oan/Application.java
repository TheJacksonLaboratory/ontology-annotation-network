package org.jax.oan;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
    info = @Info(
            title = "ontology-annotation-network",
            version = "1.0.2-SNAPSHOT",
            description = "A restful service for access to the ontology annotation network.",
            contact = @Contact(name = "Michael Gargano", email = "Michael.Gargano@jax.org")
    ), servers = {@Server(url = "https://ontology.jax.org/api/network", description = "Production Server URL"),
        @Server(url = "http://localhost:8080/api/network", description = "Development Server URL")}
)
public class Application {
    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
