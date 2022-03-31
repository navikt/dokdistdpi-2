package no.nav.dokdistdpi.cloudstorage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DokDistDokumentFraBucket {
	private byte[] pdf;
	private String dokumentObjektReferanse;
	private String dokumentInfoId;
}
