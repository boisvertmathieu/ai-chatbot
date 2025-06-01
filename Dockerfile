# Étape de build
FROM gradle:8.14-jdk21-alpine AS builder

WORKDIR /app
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle

# Télécharger les dépendances (mise en cache)
RUN gradle build -x test --no-daemon || return 0

# Copier le code source et construire
COPY src ./src
RUN gradle build -x test --no-daemon

# Étape de runtime
FROM openjdk:21-jre-slim

# Installer les outils de surveillance et sécurité
RUN apt-get update && apt-get install -y \
    curl \
    dumb-init \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r spring \
    && useradd -r -g spring spring

# Configuration JVM pour les conteneurs
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom"

# Configuration de l'application
ENV SPRING_PROFILES_ACTIVE=prod
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics

WORKDIR /app

# Copier l'artefact depuis l'étape de build
COPY --from=builder /app/build/libs/*.jar app.jar

# Créer un utilisateur non-root pour la sécurité
RUN chown spring:spring app.jar

USER spring:spring

# Port d'exposition
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Point d'entrée avec dumb-init pour la gestion des signaux
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 