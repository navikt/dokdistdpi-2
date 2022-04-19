#!/usr/bin/env sh

if test -f /secrets/serviceuser/srvdokdistdpi-2/username;
then
    echo "Setting serviceuser_username"
    export  serviceuser_username=$(cat /secrets/serviceuser/srvdokdistdpi-2/username)
fi
if test -f /secrets/serviceuser/srvdokdistdpi-2/password;
then
    echo "Setting serviceuser_password"
    export  serviceuser_password=$(cat /secrets/serviceuser/srvdokdistdpi-2/password)
fi

if test -f /secrets/virksomhetssertifikat/sdp/credentials.json
then
    echo "Setting virksomhetssertifikat_alias"
    export virksomhetssertifikat_alias="$(cat /secrets/virksomhetssertifikat/sdp/credentials.json | jq -r '.alias')"
    echo "Setting virksomhetssertifikat_password"
    export virksomhetssertifikat_password="$(cat /secrets/virksomhetssertifikat/sdp/credentials.json | jq -r '.password')"
    echo "Setting virksomhetssertifikat_type"
    export virksomhetssertifikat_type="$(cat /secrets/virksomhetssertifikat/sdp/credentials.json | jq -r '.type')"
fi
if test -f /secrets/virksomhetssertifikat/sdp/key.p12.b64
then
    echo "Setting virksomhetssertifikat_path"
    export virksomhetssertifikat_path="file:///secrets/virksomhetssertifikat/sdp/key.p12.b64"
fi

echo "Exporting appdynamics environment variables"
if test -f /var/run/secrets/nais.io/appdynamics/appdynamics.env;
then
    export $(cat /var/run/secrets/nais.io/appdynamics/appdynamics.env)
    export APPDYNAMICS_AGENT_BASE_DIR=/tmp/appdynamics
    echo "Appdynamics environment variables exported"
else
    echo "No such file or directory found at /var/run/secrets/nais.io/appdynamics/appdynamics.env"
fi

if test -f /var/run/secrets/nais.io/vault/gcloud_serviceaccount
then
    echo "Setting GOOGLE_APPLICATION_CREDENTIALS"
    export GOOGLE_APPLICATION_CREDENTIALS=/var/run/secrets/nais.io/vault/gcloud_serviceaccount
fi
