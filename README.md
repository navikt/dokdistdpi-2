# Dokdistdpi-2

Dokdistdpi-2 er en java-applikasjon for distribusjon av digital post til innbyggerene (DPI). 

Dokdistdpi-2 bruker [transportinfrastrukturen](https://docs.digdir.no/dpi_nyinfrastruktur.html) for DPI som erstattes med en standard-infrastruktur for meldingsutveksling i det offentlige.

Appen har følgende moduler og tilhørende funksjonalitet:
- qdist011 - mottar distribusjonsbestillinger og distribuerer til DPI
- qdist014 - behandler kvitteringer fra DPI
- sdist003 - regelmessig jobb som henter kvitteringer på DPI-bestillingene
- sdist005 - regelmessig jobb som henter status på forsendelser som mangler kvittering

For å kunne sende digital post til innbygger må følgende være på plass:
* Avsender må være registert hos [KRR (Nav-internt)](https://confluence.adeo.no/display/BOA/QDIST011+-+DistribuerForsendelseTilDPI-2.+Testing).
* Avsender må være registert hos postkassene.
* Avsender må ha et gyldig virksomhetssertifikat.

Mer informasjon om applikasjonen finner du i [Confluence-dokumentasjonen for dokdistdpi (Nav-internt)](https://confluence.adeo.no/display/BOA/dokdistdpi).

## Komme i gang

Kjør tester og bygg appen

```
mvn clean verify
```

---

## Henvendelser

Lag en issue i repository.

### For Nav-ansatte

Spørsmål om appen kan stilles på [#team_dokumentløsninger](https://nav-it.slack.com/archives/C6W9E5GPJ)

## Lisens

[MIT](LICENSE.md)
