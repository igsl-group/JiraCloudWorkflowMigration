package com.igsl.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.igsl.rest.Paged;
import com.igsl.rest.RestUtil;

public class Project extends Model<Project> {
	private static final String NAME = "NAME";
	@Override
	public String getIdentifier() {
		return this.id;
	}
	@Override
	public String getUniqueName() {
		return this.key;
	}
	@Override
	public List<String> getColumns() {
		return Arrays.asList(NAME);
	}
	@Override
	public Map<String, String> getValues() {
		Map<String, String> result = new HashMap<>();
		result.put(NAME, this.name);
		return result;
	}
	@Override
	public void setIdentifier(String identifier) {
		this.id = identifier;
	}
	@Override
	public void setUniqueName(String uniqueName) {
		this.key = uniqueName;
	}
	@Override
	public void setValues(Map<String, String> values) {
		this.name = values.get(NAME);
	}
	@Override
	public String toString() {
		return this.name + " = " + this.key + " (" + this.id + ")";
	}	
	@Override
	public void setupRestUtil(RestUtil<Project> util) {
		util.path("/rest/api/3/project/search")
			.pagination(new Paged<Project>(Project.class));
	}
	private String id;
	private String name;
	private String key;
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
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
}
