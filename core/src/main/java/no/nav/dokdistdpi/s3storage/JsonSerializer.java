package no.nav.dokdistdpi.s3storage;

import com.amazonaws.util.json.Jackson;

public class JsonSerializer {
	private JsonSerializer() {
	}

	public static String serialize(Object object) {
		return Jackson.toJsonString(object);
	}
	public static <T> T deserialize(String jsonPayload, Class<T> tClass) {
		return Jackson.fromJsonString(jsonPayload, tClass);
	}
}
