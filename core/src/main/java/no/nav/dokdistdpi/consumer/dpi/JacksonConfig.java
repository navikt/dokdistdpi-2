package no.nav.dokdistdpi.consumer.dpi;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DEFAULT_ZONE_ID;

@Configuration
public class JacksonConfig {

	@Bean
	@Qualifier("dpiObjectMapper")
	public ObjectMapper eformidlingObjectMapper() {
		return new Jackson2ObjectMapperBuilder()
				.deserializerByType(OffsetDateTime.class, new IsoDateTimeDeserializer())
				.modulesToInstall(new JavaTimeModule())
				.serializationInclusion(NON_NULL)
				.featuresToEnable(
						SerializationFeature.INDENT_OUTPUT,
						MapperFeature.DEFAULT_VIEW_INCLUSION)
				.featuresToDisable(
						SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
						SerializationFeature.CLOSE_CLOSEABLE,
						JsonGenerator.Feature.AUTO_CLOSE_TARGET).build();
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
					true // yes, replace +0000 with Z
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
