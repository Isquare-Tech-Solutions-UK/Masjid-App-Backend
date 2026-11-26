# One image for every environment. Nothing environment-specific is baked in:
# secrets come from Infisical at container start (see entrypoint.sh) or, for
# the legacy masjid-app compose stack, as plain environment variables. The
# Spring profile comes from SPRING_PROFILES_ACTIVE on each stack.

# ---- Build stage ------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies in their own layer so source-only changes reuse it.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package \
 && cp target/masjid-app-backend-*.jar /build/app.jar

# ---- Runtime stage ----------------------------------------------------------
FROM eclipse-temurin:21-jre

# curl: healthcheck of the new Portainer stacks.
# wget: healthcheck of the legacy masjid-app compose stack. Keep both until
#       that stack is retired, or masjid-backend goes unhealthy after a deploy.
# infisical: fetches secrets at startup. Installed from Infisical's signed apt
#       repository (https://artifacts-cli.infisical.com) rather than by piping
#       their setup script into a shell.
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends ca-certificates curl wget gnupg; \
    curl -fsSL https://artifacts-cli.infisical.com/infisical.gpg \
      | gpg --dearmor -o /usr/share/keyrings/infisical-archive-keyring.gpg; \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/infisical-archive-keyring.gpg] https://artifacts-cli.infisical.com/deb stable main" \
      > /etc/apt/sources.list.d/infisical.list; \
    apt-get update; \
    apt-get install -y --no-install-recommends infisical; \
    apt-get purge -y --auto-remove gnupg; \
    rm -rf /var/lib/apt/lists/*; \
    infisical --version

WORKDIR /app
COPY --from=build /build/app.jar /app/app.jar
COPY entrypoint.sh /entrypoint.sh
RUN chmod 0755 /entrypoint.sh

# Run as a non-root user. The Infisical CLI does not need root, but it does
# want a writable home for its config, so give the user one.
RUN groupadd --system spring \
 && useradd --system --gid spring --create-home --home-dir /home/spring spring
USER spring:spring
ENV HOME=/home/spring

# Heap is sized from the container's memory limit rather than the host's.
# Override per stack with JAVA_TOOL_OPTIONS if needed.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"

EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
