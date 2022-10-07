#!/usr/bin/env sh

JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.keyStore=${DOKDISTDPICERT_KEYSTORE}"
JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.keyStoreType=jks"
JAVA_OPTS="${JAVA_OPTS} -Xmx1024m -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=nais"

export JAVA_OPTS
