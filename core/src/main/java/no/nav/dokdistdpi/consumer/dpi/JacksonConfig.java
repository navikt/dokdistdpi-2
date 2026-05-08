package no.nav.dokdistdpi.consumer.dpi;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ext.javatime.deser.InstantDeserializer;
import tools.jackson.databind.module.SimpleModule;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DEFAULT_ZONE_ID;

@Configuration
public class JacksonConfig {

	@Bean
	public JacksonModule dpiOffsetDateTimeModule() {
		var module = new SimpleModule("DpiOffsetDateTimeModule");
		module.addDeserializer(OffsetDateTime.class, new IsoDateTimeDeserializer());
		return module;
	}

	@Bean
	public JsonMapperBuilderCustomizer dpiJsonMapperCustomizer() {
		return builder -> builder
				.changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
				.enable(SerializationFeature.INDENT_OUTPUT)
				.enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
				.disable(SerializationFeature.CLOSE_CLOSEABLE)
				.disable(StreamWriteFeature.AUTO_CLOSE_TARGET);
	}

	private static final class IsoDateTimeDeserializer extends InstantDeserializer<OffsetDateTime> {

		IsoDateTimeDeserializer() {
			super(
					OffsetDateTime.class,
					DateTimeFormatter.ISO_DATE_TIME,
					IsoDateTimeDeserializer::getOffsetDateTime,
					a -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(a.value), a.zoneId),
					a -> OffsetDateTime.ofInstant(Instant.ofEpochSecond(a.integer, a.fraction), a.zoneId),
					(d, z) -> d.withOffsetSameInstant(z.getRules().getOffset(d.toLocalDateTime())),
					true, // replace +0000 with Z
					false, // normalizeZoneId
					false  // adjustToContextTZOverride
			);
		}

		private static OffsetDateTime getOffsetDateTime(TemporalAccessor temporal) {
			ZoneId obj = temporal.query(TemporalQueries.zone());
			if (obj != null) {
				return OffsetDateTime.from(temporal);
			}
			return LocalDateTime.from(temporal)
					.atOffset(DEFAULT_ZONE_ID.getRules().getOffset(LocalDateTime.now()));
		}
	}
}
