# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-noble AS deps
WORKDIR /build
COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=bind,source=catalog-service/pom.xml,target=catalog-service/pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw -pl catalog-service dependency:go-offline -DskipTests

FROM deps AS package
WORKDIR /build
COPY catalog-service/src catalog-service/src/
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=bind,source=catalog-service/pom.xml,target=catalog-service/pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw -pl catalog-service package -DskipTests && \
    mkdir -p target && \
    mv catalog-service/target/$(./mvnw -pl catalog-service help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(./mvnw -pl catalog-service help:evaluate -Dexpression=project.version -q -DforceStdout).jar target/app.jar

FROM package AS extract
WORKDIR /build
RUN java -Djarmode=tools -jar target/app.jar extract --layers --launcher --destination target/extracted

FROM extract AS development
WORKDIR /build
RUN cp -r /build/target/extracted/dependencies/. ./
RUN cp -r /build/target/extracted/spring-boot-loader/. ./
RUN cp -r /build/target/extracted/snapshot-dependencies/. ./
RUN cp -r /build/target/extracted/application/. ./
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000
CMD [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]

FROM eclipse-temurin:25-jre-noble AS final
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser
USER appuser
COPY --from=extract build/target/extracted/dependencies/ ./
COPY --from=extract build/target/extracted/spring-boot-loader/ ./
COPY --from=extract build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract build/target/extracted/application/ ./
EXPOSE 8081
ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]
