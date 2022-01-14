package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import com.fasterxml.jackson.core.JsonGenerator;
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
		gen.writeFieldName("standardBusinessDocument");
		gen.writeObject(value.getSbd());
		gen.writeFieldName("standardBusinessDocumentHeader");
		gen.writeObject(value.getStandardBusinessDocumentHeader());
		if (value.getSbd().getAny() instanceof DigitalPost) {
			gen.writeFieldName("digital");
		} else {
			throw new UnsupportedOperationException("Kun avtaltmelding er støttet.");
		}
		gen.writeObject(value.getSbd().getAny());
		gen.writeEndObject();
	}
}
