FROM navikt/java:17-appdynamics
ENV APPD_ENABLED=true

COPY app/target/app.jar /app/app.jar
COPY export-vault-secrets.sh /init-scripts/10-export-vault-secrets.sh

USER root
# Brukes for å hente config fra json filer
RUN export "http_proxy=http://webproxy-utvikler.nav.no:8088/" \
    && export "https_proxy=http://webproxy-utvikler.nav.no:8088/" \
    && apt-get install -y --no-install-recommends jq
USER apprunner

ENV JAVA_OPTS="-Xmx1024m \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=nais"