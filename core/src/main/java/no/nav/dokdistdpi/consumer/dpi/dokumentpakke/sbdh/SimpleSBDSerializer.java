package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;

import java.io.IOException;

public class SimpleSBDSerializer extends StdSerializer<SimpleStandardBusinessDocument> {

	protected SimpleSBDSerializer() {
		super(SimpleStandardBusinessDocument.class);
	}

	@Override
	public void serialize(SimpleStandardBusinessDocument value, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
		gen.writeStartObject();
		gen.writeObjectFieldStart("standardBusinessDocument");
		gen.writeFieldName("standardBusinessDocumentHeader");
		gen.writeObject(value.getStandardBusinessDocumentHeader());
		if (value.getStandardBusinessDocument().getAny() instanceof DigitalPost) {
			gen.writeFieldName("digital");
			gen.writeObject(value.getStandardBusinessDocument().getAny());
		} else {
			throw new UnsupportedOperationException("Kun digitalmelding er støttet.");
		}
		gen.writeEndObject();
	}
}
