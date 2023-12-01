package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardBusinessDocument {

	@NonNull
	@JsonProperty
	private StandardBusinessDocumentHeader standardBusinessDocumentHeader;

	@JsonAlias({"digital", "varslingfeiletkvittering", "leveringskvittering", "kvittering", "feil", "mottakskvittering", "aapningskvittering"})
	@NonNull
	private Object any;

}
