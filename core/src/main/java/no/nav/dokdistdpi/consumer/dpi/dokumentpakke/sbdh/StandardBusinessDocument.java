package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize(using = StandardBusinessDocumentSerializer.class)
public class StandardBusinessDocument {

	@NonNull
	@JsonProperty
	private StandardBusinessDocumentHeader standardBusinessDocumentHeader;

	@JsonAlias({"digital", "varslingfeiletkvittering", "leveringskvittering", "kvittering", "feil"})
	@NonNull
	private Object any;

}
