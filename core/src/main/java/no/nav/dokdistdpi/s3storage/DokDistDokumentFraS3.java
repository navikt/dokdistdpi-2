package no.nav.dokdistdpi.s3storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DokDistDokumentFraS3 {
	private byte[] pdf;
	private String dokumentObjektReferanse;
	private String dokumentInfoId;
}
