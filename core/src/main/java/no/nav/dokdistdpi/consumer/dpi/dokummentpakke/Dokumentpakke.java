package no.nav.dokdistdpi.consumer.dpi.dokummentpakke;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class Dokumentpakke {
	private final DigitalPostDokument hoveddokument;
	@Builder.Default
	private final List<DigitalPostDokument> vedlegg = new ArrayList<>();
}
