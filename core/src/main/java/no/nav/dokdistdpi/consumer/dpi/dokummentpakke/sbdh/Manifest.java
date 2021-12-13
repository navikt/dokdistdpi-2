package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
public class Manifest {
	protected Long numberOfItems;
	protected Set<ManifestItem> manifestItem;

	@JsonProperty
	public Long getNumberOfItems() {
		return numberOfItems != null ? numberOfItems : getManifestItem().size();
	}

	public Set<ManifestItem> getManifestItem() {
		if (manifestItem == null) {
			manifestItem = new HashSet<>();
		}
		return this.manifestItem;
	}
}
