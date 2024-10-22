ARG KEYCLOAK_VERSION="26.0.0"

# Stage 1: Build the custom GitHub identity provider
FROM maven:latest AS builder

WORKDIR /workspace

COPY pom.xml /workspace/
COPY src /workspace/src/

RUN mvn clean package "-Dkeycloak.version=$KEYCLOAK_VERSION"

# Stage 2: Build Keycloak with the custom identity provider
FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION} AS keycloak

COPY --from=builder /workspace/target/github-org-identity-provider-1.0.0.jar /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build