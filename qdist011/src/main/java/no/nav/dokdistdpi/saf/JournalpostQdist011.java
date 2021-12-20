package no.nav.dokdistdpi.saf;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class JournalpostQdist011 {
	@Builder.Default
	List<DokumentInfo> dokumenter = new ArrayList<>();

	@Value
	@Builder
	public static class DokumentInfo {
		String dokumentInfoId;
		String tittel;
	}
}
