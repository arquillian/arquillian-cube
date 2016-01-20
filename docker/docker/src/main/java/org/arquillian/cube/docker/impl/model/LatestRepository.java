package org.arquillian.cube.docker.impl.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LatestRepository {

	@JsonProperty("tag_name")
	private String tagName;

	@JsonProperty("name")
	private String name;

	public String getTagName() {
		return this.tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "LatestRepository{" +
				"tagName='" + tagName + '\'' +
				", name='" + name + '\'' +
				'}';
	}

}
