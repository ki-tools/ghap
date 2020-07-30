package io.ghap.reporting.data;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.UUID;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {

	private UUID id;
	private String externalId;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}
}
