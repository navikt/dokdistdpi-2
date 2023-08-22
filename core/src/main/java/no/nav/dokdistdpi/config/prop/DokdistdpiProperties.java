package no.nav.dokdistdpi.config.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

@Data
@ConfigurationProperties("dokdistdpi")
@Validated
public class DokdistdpiProperties {

	private final Qdist011 qdist011 = new Qdist011();
	private final Qdist014 qdist014 = new Qdist014();
	private final Sdist003 sdist003 = new Sdist003();
	private final Sdist005 sdist005 = new Sdist005();
	private final Endpoints endpoints = new Endpoints();

	@Data
	@Validated
	public static class Qdist014 {
		private boolean autostartup;
	}

	@Data
	@Validated
	public static class Qdist011 {
		private boolean autostartup;
	}

	@Data
	@Validated
	public static class Sdist003 {
		private boolean autostartup;
		/**
		 * Tid mellom hver poll av kvitteringer fra DPI. Eksempel: 10m
		 */
		@NotNull
		private Duration polldelay;

		/**
		 * https://camel.apache.org/components/3.16.x/scheduler-component.html
		 *
		 * @return camel URI som konfigurerer sdist003
		 */
		public String camelUri() {
			return "scheduler://sdist003?timeUnit=SECONDS" +
					"&initialDelay=5" +
					"&delay=" + polldelay.toSeconds() +
					"&backoffMultiplier=2" +
					"&backoffIdleThreshold=2";
		}
	}

	@Data
	@Validated
	public static class Sdist005 {
		private boolean autostartup;
		/**
		 * Tid mellom hver poll av uvkitterte meldinger.
		 */
		@NotNull
		private Duration polldelay;

		/**
		 * https://camel.apache.org/components/3.16.x/scheduler-component.html
		 *
		 * @return camel URI som konfigurerer sdist005
		 */
		public String camelUri() {
			return "scheduler://sdist005?timeUnit=SECONDS" +
					"&initialDelay=5" +
					"&delay=" + polldelay.toSeconds();
		}
	}

	@Data
	@Validated
	public static class Endpoints {
		/**
		 * URL til dokdistadmin journalpost api.
		 */
		@NotNull
		private AppEndpoint dokdistadmin;

	}

	@Data
	@Validated
	public static class AppEndpoint {
		/**
		 * Url til tjeneste som har azure autorisasjon
		 */
		@NotEmpty
		private String url;

		/**
		 * Scope til azure client credential flow
		 */
		@NotEmpty
		private String scope;
	}
}
