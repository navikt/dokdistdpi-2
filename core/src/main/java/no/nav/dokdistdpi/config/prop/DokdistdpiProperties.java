package no.nav.dokdistdpi.config.prop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@ConfigurationProperties("dokdistdpi")
@Validated
public class DokdistdpiProperties {

	@Valid
	private final Qdist011 qdist011 = new Qdist011();
	@Valid
	private final Qdist014 qdist014 = new Qdist014();
	@Valid
	private final Sdist003 sdist003 = new Sdist003();
	@Valid
	private final Sdist005 sdist005 = new Sdist005();
	@Valid
	private final SlackProperties slack = new SlackProperties();
	@Valid
	private final Endpoints endpoints = new Endpoints();

	@Data
	public static class Qdist014 {
		private boolean autostartup;
		@Positive
		private int concurrency;
	}

	@Data
	public static class Qdist011 {
		private boolean autostartup;
	}

	@Data
	public static class Sdist003 {
		private boolean autostartup;
		/// Tid mellom hver poll av kvitteringer fra DPI. Eksempel: 10m
		@NotNull
		private Duration polldelay;
	}

	@Data
	public static class Sdist005 {
		private boolean autostartup;
		/// Tid mellom hver poll av ukvitterte meldinger. Eksempel: 10m
		@NotNull
		private Duration polldelay;

		/// [camel scheduler](https://camel.apache.org/components/3.16.x/scheduler-component.html)
		///
		/// @return camel URI som konfigurerer sdist005 polling
		public String camelUri() {
			return "scheduler://sdist005?timeUnit=SECONDS" +
					"&initialDelay=5" +
					"&delay=" + polldelay.toSeconds();
		}
	}

	@Data
	public static class Endpoints {
		@Valid
		private final Dpi dpi = new Dpi();

		@NotEmpty
		String dokmetUrl;

		@NotEmpty
		String juridisklogg;

		@NotNull
		private AppEndpoint dokdistadmin;

		@NotNull
		private AppEndpoint saf;

		@NotNull
		private AppEndpoint digdir;
	}

	@Data
	public static class AppEndpoint {
		/// Url til tjeneste som har azure autorisasjon
		@NotEmpty
		private String url;

		/// Scope til azure client credential flow
		@NotEmpty
		private String scope;
	}

	@Data
	public static class SlackProperties {
		@NotEmpty
		@ToString.Exclude
		private String token;
		@NotEmpty
		private String channel;
		private boolean enabled;

		@Positive
		private int minimumAntallSekunderMellomSlackvarsel;
	}

	@Data
	public static class Dpi {
		@NotEmpty
		private String url;

		@NotEmpty
		private String mpckanal;

		@Positive
		private int pagesize;
	}
}