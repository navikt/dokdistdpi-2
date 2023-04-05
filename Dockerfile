FROM ghcr.io/navikt/baseimages/temurin:17-appdynamics
ENV APPD_ENABLED=true

COPY app/target/app.jar app.jar
COPY export-vault-secrets.sh /init-scripts/10-export-vault-secrets.sh
COPY dokdistdpi-java-opts.sh /init-scripts/20-dokdistdpi-java-opts.sh

USER root
# Brukes for å hente config fra json filer
RUN apt-get install -y --no-install-recommends jq
USER apprunner

ENV MAIN_CLASS="org.springframework.boot.loader.JarLauncher"
