package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class CorrelationInformation {
	protected OffsetDateTime requestingDocumentCreationDateTime;
	protected String requestingDocumentInstanceIdentifier;
	protected OffsetDateTime expectedResponseDateTime;
}
