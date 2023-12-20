package no.nav.dokdistdpi.sdist003;

import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;

public record Kvittering(SimpleStandardBusinessDocument simpleSbd, String forretningsmelding) {
}
