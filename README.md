# Dokdistdpi-2
Dokdistdpi-2 er en java-applikasjon for distribusjon av digital post til innbyggerene (DPI). Dokdistdpi-2 bruker den nye 
[transportinfrastrukturen](https://docs.digdir.no/dpi_nyinfrastruktur.html) for DPI som erstattes med en standard-infrastruktur for meldingsutvekling i det offentlige.

Appen har følgende moduler og tilhørende funksjonalitet:
- qdist011 - mottar distribusjonsbestillinger og distribuerer til DPI
- qdist014 - behandler kvitteringer fra DPI
- sdist003 - regelmessig jobb som henter kvitteringer på DPI-bestillingene
- sdist005 - regelmessig jobb som henter status på forsendelser som mangler kvittering

For å kunne sende digital post til innbygger må følgende være på plass:
* Avsender må være registert hos [KRR](https://confluence.adeo.no/display/BOA/QDIST011+-+DistribuerForsendelseTilDPI-2.+Testing).
* Avsender må være registert hos postkassene.
* Avsender må ha et gyldig virksomhetssertifikat.

Mer informasjon om applikasjonen finner du i [Confluence-dokumentasjonen for dokdistdpi](https://confluence.adeo.no/display/BOA/dokdistdpi).

## Kom i gang
### Kompilering, bygging og kjøring av tester
`mvn clean install`

### Lokal kjøring
Kjøring lokalt mot testmiljøer kan f.eks. gjøres ved å bruke [environment variables for Q2-miljøet](https://vault.adeo.no/ui/vault/secrets/secret/show/dokument/dokdistdpi-2).

### Kontakt oss
Interne henvendelser kan sendes via Slack i kanalen [Team Dokumentløsninger](https://nav-it.slack.com/archives/C6W9E5GPJ).

### Lisens
MIT License