package org.testcontainers.workshop;

class PropertiesGHConnectionDetails implements GHConnectionDetails {

	private final GHProperties properties;

	public PropertiesGHConnectionDetails(GHProperties properties) {
		this.properties = properties;
	}

	@Override
	public String url() {
		return this.properties.url();
	}

	@Override
	public String token() {
		return this.properties.token();
	}

}
