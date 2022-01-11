package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class PartnerIdentification implements Serializable {
	protected String value;
	protected String authority;

}
