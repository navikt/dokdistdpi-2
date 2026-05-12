package no.nav.dokdistdpi.cloudstorage;

import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

public class JsonSerializer {
	private static final JsonMapper jsonMapper = JsonMapper.builder()
			.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();

	private static final ObjectWriter writer = jsonMapper.writer();

	public static String serialize(Object object) {
		try {
			return writer.writeValueAsString(object);
		} catch (JacksonException e) {
			throw new IllegalStateException(e);
		}
	}

	public static <T> T deserialize(String jsonPayload, Class<T> tClass) {
		try {
			return jsonMapper.readValue(jsonPayload, tClass);
		} catch (JacksonException e) {
			throw new IllegalStateException(e);
		}
	}
}
