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

export prod_credentials_2023_path=/secrets/virksomhetssertifikat/sdp/credentials_2023.json
export test_credentials_2024_path=/secrets/virksomhetssertifikat/sdp/credentials_2024-03.json

if test -f $prod_credentials_2023_path
then
    echo "Setting virksomhetssertifikat_alias"
    export virksomhetssertifikat_alias="$(cat $prod_credentials_2023_path | jq -r '.alias')"
    echo "Setting virksomhetssertifikat_password"
    export virksomhetssertifikat_password="$(cat $prod_credentials_2023_path | jq -r '.password')"
    echo "Setting virksomhetssertifikat_type"
    export virksomhetssertifikat_type="$(cat $prod_credentials_2023_path | jq -r '.type')"
else
    echo "Setting virksomhetssertifikat_alias"
    export virksomhetssertifikat_alias="$(cat $test_credentials_2024_path | jq -r '.alias')"
    echo "Setting virksomhetssertifikat_password"
    export virksomhetssertifikat_password="$(cat $test_credentials_2024_path | jq -r '.password')"
    echo "Setting virksomhetssertifikat_type"
    export virksomhetssertifikat_type="$(cat $test_credentials_2024_path | jq -r '.type')"
fi

if test -f /secrets/virksomhetssertifikat/sdp/274258896775237957919470-2023-10-11.p12.b64
then
    echo "Setting virksomhetssertifikat_path"
    export virksomhetssertifikat_path="file:///secrets/virksomhetssertifikat/sdp/274258896775237957919470-2023-10-11.p12.b64"
else
    echo "Setting virksomhetssertifikat_path"
    export virksomhetssertifikat_path="file:///secrets/virksomhetssertifikat/sdp/1956923288254923191157769-2024-03-19.p12.b64"
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
