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

if test -f "$NAV_VIRKSOMHETSSERTIFIKAT_CREDENTIALS"
then
    echo "Setting virksomhetssertifikat_alias"
    export virksomhetssertifikat_alias="$(cat $NAV_VIRKSOMHETSSERTIFIKAT_CREDENTIALS | jq -r '.alias')"
    echo "Setting virksomhetssertifikat_password"
    export virksomhetssertifikat_password="$(cat $NAV_VIRKSOMHETSSERTIFIKAT_CREDENTIALS | jq -r '.password')"
    echo "Setting virksomhetssertifikat_type"
    export virksomhetssertifikat_type="$(cat $NAV_VIRKSOMHETSSERTIFIKAT_CREDENTIALS | jq -r '.type')"
fi

if test -f "$NAV_VIRKSOMHETSSERTIFIKAT_KEY"
then
    echo "Setting virksomhetssertifikat_path"
    export virksomhetssertifikat_path="file://$NAV_VIRKSOMHETSSERTIFIKAT_KEY"
fi

if test -f /var/run/secrets/nais.io/vault/gcloud_serviceaccount
then
    echo "Setting GOOGLE_APPLICATION_CREDENTIALS"
    export GOOGLE_APPLICATION_CREDENTIALS=/var/run/secrets/nais.io/vault/gcloud_serviceaccount
fi
