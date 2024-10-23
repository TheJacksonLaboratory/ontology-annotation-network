package org.jax.oan.utils;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.neo4j.driver.Driver;


@Singleton
@Requires(notEnv = "test")
public class DriverSessionChecker {

	private final Driver driver;

	private final ApplicationContext context;

	public DriverSessionChecker(Driver driver, ApplicationContext context) {
		this.driver = driver;
		this.context = context;
	}

	@EventListener
	public void on(StartupEvent startupEvent) {
			driver.verifyConnectivity();
	}
}
