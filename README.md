# DOKDISTDPI-2

Dokumentasjon: [https://confluence.adeo.no/display/BOA/dokdistdpi](https://confluence.adeo.no/display/BOA/dokdistdpi)

Dokdistdpi-2 er en java-applikasjon for distribusjon av DigitalPost til Innbyggerene(DPI). Dokdistdpi-2 bruker den nye 
[transportinfrastrukturen](https://docs.digdir.no/dpi_nyinfrastruktur.html) for DPI som erstattes med en standard-infrastruktur for meldingsutvekling i det offentlige.

For å starte sending av digital post må:
* Avsender må være registert hos [KRR](https://confluence.adeo.no/display/BOA/QDIST011+-+DistribuerForsendelseTilDPI-2.+Testing).
* Avsender må være registert hos postkassene
* Avsender må ha et gyldig virksomhetssertifikat

## Technologies & Tools

* [Spring Boot](https://spring.io/projects/spring-boot)
* [Camel](https://camel.apache.org/)
* [Maven](http://maven.apache.org/)
* [docker]()
* [Java 17]()
* [Kubectl](https://kubernetes.io/)

## Getting started

### Compile, build and run tests
`mvn clean install`

### Kibana
For [dev-fss](https://logs.adeo.no/goto/6f574b7302e801c1f0f3f5015140b956)
For [prod-fss](https://logs.adeo.no/goto/9c98feb93c3368429b57e7b70ab8c6bd)

### Kubectl
For dev-fss:
```shell script
kubectl config use-context dev-fss
kubectl get pods -n teamdokumenthandtering | grep dokdistdpi-2
kubectl logs -f [dokdistdpi pod] -n teamdokumenthandtering -c dokdistdpi-2
```

For prod-fss:
```shell script
kubectl config use-context prod-fss
kubectl get pods -n teamdokumenthandtering | grep dokdistdpi-2
kubectl logs -f [dokdistdpi pod] -n teamdokumenthandtering -c dokdistdpi-2
```

## Metrics
* [Grafana](https://grafana.nais.io/d/ejWLRux7z/dokdistdpi-2)

### Running locally
Kjøring lokalt mot testmiljøer (fe.eks bruke q2 env variables fra vault)
* [Environment variables for Q2-miljø](https://vault.adeo.no/ui/vault/secrets/secret/show/dokument/dokdistdpi-2)

### Contact us
Interne henvendelser kan sendes via Slack i kanalen [Team Dokumentløsninger](https://nav-it.slack.com/archives/C6W9E5GPJ).


### License

MIT License