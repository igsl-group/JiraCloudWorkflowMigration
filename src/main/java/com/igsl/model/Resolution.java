package com.igsl.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.igsl.rest.Paged;
import com.igsl.rest.RestUtil;

public class Resolution extends Model<Resolution> {
	public static final String DESCRIPTION = "DESCRIPTION";
	public static final String IS_DEFAULT = "IS_DEFAULT";
	@Override
	public String getIdentifier() {
		return this.id;
	}
	@Override
	public String getUniqueName() {
		return this.name;
	}
	@Override
	public List<String> getColumns() {
		return Arrays.asList(DESCRIPTION, IS_DEFAULT);
	}
	@Override
	public Map<String, String> getValues() {
		Map<String, String> map = new HashMap<>();
		map.put(DESCRIPTION, this.description);
		map.put(IS_DEFAULT, Boolean.toString(this.isDefault));
		return map;
	}
	@Override
	public void setIdentifier(String identifier) {
		this.id = identifier;
	}
	@Override
	public void setUniqueName(String uniqueName) {
		this.name = uniqueName;
	}
	@Override
	public void setValues(Map<String, String> values) {
		this.description = values.get(DESCRIPTION);
		this.isDefault = Boolean.parseBoolean(values.get(IS_DEFAULT));
	}
	@Override
	public String toString() {
		return this.name + " = " + this.id;
	}
	@Override
	public void setupRestUtil(RestUtil<Resolution> util) {
		util.path("/rest/api/3/resolution/search")
			.pagination(new Paged<Resolution>(Resolution.class));
	}
	private String id;
	private String name;
	private String description;
	@JsonProperty("isDefault")
	private boolean isDefault;
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
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public boolean isDefault() {
		return isDefault;
	}
	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}
}
