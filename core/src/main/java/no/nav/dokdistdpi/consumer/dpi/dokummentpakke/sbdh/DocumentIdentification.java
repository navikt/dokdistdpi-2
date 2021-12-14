package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class DocumentIdentification {
	protected String standard;
	protected String typeVersion;
	protected String instanceIdentifier;
	protected String type;
	protected Boolean multipleType;
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	protected OffsetDateTime creationDateAndTime;
}
