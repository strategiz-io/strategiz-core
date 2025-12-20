package io.strategiz.client.base.llm.model;

/**
 * Information about an available LLM model.
 */
public class ModelInfo {

	private String id;

	private String name;

	private String provider;

	private String description;

	private boolean available;

	public ModelInfo() {
		this.available = true;
	}

	public ModelInfo(String id, String name, String provider, String description) {
		this();
		this.id = id;
		this.name = name;
		this.provider = provider;
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

}
