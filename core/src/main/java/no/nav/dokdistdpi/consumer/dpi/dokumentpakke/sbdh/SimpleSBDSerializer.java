package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;

public class SimpleSBDSerializer extends StdSerializer<SimpleStandardBusinessDocument> {

	protected SimpleSBDSerializer() {
		super(SimpleStandardBusinessDocument.class);
	}

	@Override
	public void serialize(SimpleStandardBusinessDocument value, JsonGenerator gen, SerializationContext serializationContext) {
		gen.writeStartObject();
		gen.writeObjectPropertyStart("standardBusinessDocument");
		gen.writeName("standardBusinessDocumentHeader");
		gen.writePOJO(value.getStandardBusinessDocumentHeader());
		if (value.getStandardBusinessDocument().getAny() instanceof DigitalPost) {
			gen.writeName("digital");
			gen.writePOJO(value.getStandardBusinessDocument().getAny());
		} else {
			throw new UnsupportedOperationException("Kun digitalmelding er støttet.");
		}
		gen.writeEndObject();
	}
}
