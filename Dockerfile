FROM adoptopenjdk:11-jre as builder
WORKDIR build
COPY app/target/app.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM navikt/java:17-appdynamics
WORKDIR app
COPY --from=builder build/dependencies/ ./
COPY --from=builder build/snapshot-dependencies/ ./
COPY --from=builder build/spring-boot-loader/ ./
COPY --from=builder build/application/ ./
COPY export-vault-secrets.sh /init-scripts/10-export-vault-secrets.sh
COPY run-java.sh /

USER root
RUN chmod +x /run-java.sh
# Brukes for å hente config fra json filer
RUN export "http_proxy=http://webproxy-utvikler.nav.no:8088/" \
    && export "https_proxy=http://webproxy-utvikler.nav.no:8088/" \
    && apt-get install -y --no-install-recommends jq
USER apprunner

ENV APPD_ENABLED=true
ENV MAIN_CLASS="org.springframework.boot.loader.JarLauncher"
ENV JAVA_OPTS="-Xmx1024m \ -Djava.security.egd=file:/dev/./urandom \ -Dspring.profiles.active=nais"