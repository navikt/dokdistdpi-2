package no.nav.dokdistdpi.consumer.dpi.dokumentpakke;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class Dokumentpakke {
	private final DpiDokument hoveddokument;
	@Builder.Default
	private final List<DpiDokument> vedlegg = new ArrayList<>();
}
