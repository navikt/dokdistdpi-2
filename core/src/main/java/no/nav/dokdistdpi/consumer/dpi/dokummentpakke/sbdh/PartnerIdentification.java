package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
public class PartnerIdentification implements Serializable {
	private Partner partner;
	protected String value;
	protected String authority;

}
