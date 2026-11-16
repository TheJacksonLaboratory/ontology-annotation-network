# Set up running image
FROM eclipse-temurin:17-jre

ARG MICRONAUT_ENV_ARG
ARG VERSION_ARG
ENV MICRONAUT_ENV=${MICRONAUT_ENV_ARG}
ENV build_version=${VERSION_ARG}

WORKDIR /app
COPY ./target/oan-rest-${build_version}.jar .
COPY ./oan.db .
CMD ["sh", "-c", "java -XX:MaxRAMPercentage=75 -Dmicronaut.environments=${MICRONAUT_ENV} -Dsqlite.path=/app/oan.db -jar oan-rest-${build_version}.jar"]
EXPOSE 8080/tcp
