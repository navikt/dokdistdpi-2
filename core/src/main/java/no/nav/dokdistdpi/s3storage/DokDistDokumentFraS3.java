package no.nav.dokdistdpi.s3storage;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DokDistDokumentFraS3 {
	private byte[] pdf;
	private String dokumentObjektReferanse;
	private String dokumentInfoId;
}
