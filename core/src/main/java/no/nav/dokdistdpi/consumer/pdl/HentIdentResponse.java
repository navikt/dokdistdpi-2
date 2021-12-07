package no.nav.dokdistdpi.consumer.pdl;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HentIdentResponse {

	private HentIdenter data;
	private List<PdlError> errors;

	@Data
	public static class HentIdenter {
		private List<IdentInfo> identer;
	}

	@Data
	public static class IdentInfo {
		private String ident;
		private Gruppe gruppe;
	}

	@Data
	public static class PdlError {
		private String message;
		private PdlErrorExtensionTo extensions;
	}

	@Data
	public static class PdlErrorExtensionTo {
		private String code;
		private ErrorDetails details;
		private String classification;
	}

	@Data
	public static class ErrorDetails {
		private String type;
		private String cause;
		private String policy;
	}

	public enum Gruppe {
		AKTORID, FOLKEREGISTERIDENT, NPID
	}
}
