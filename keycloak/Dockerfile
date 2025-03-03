
# Stage 1: Build the custom Service Provider Interfaces
FROM maven:latest AS builder
WORKDIR /workspace
COPY providers /workspace
RUN ./build_and_collect_jars.sh

# Stage 2: Build Keycloak with the SPIs and themes
FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION} AS keycloak
COPY --from=builder /workspace/.jars/*.jar /opt/keycloak/providers/
COPY themes /opt/keycloak/themes
RUN /opt/keycloak/bin/kc.sh build --features=health --optimized