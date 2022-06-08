package no.nav.dokdistdpi.config.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Konfigurasjon for appen. Endres i vault
 *
 * @author Joakim Bjørnstad, Jbit AS
 */
@Data
@ConfigurationProperties("dokdistdpi")
@Validated
public class DokdistDpiProperties {

	private final Qdist011 qdist011 = new Qdist011();
	private final Qdist014 qdist014 = new Qdist014();
	private final Sdist003 sdist003 = new Sdist003();
	private final Sdist005 sdist005 = new Sdist005();
	private final Proxy proxy = new Proxy();

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
	public static class Proxy {
		private String host;
		private int port;

		public boolean isSet() {
			return (host!=null && !host.equals(""));
		}
	}
}
