package com.igsl.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.igsl.rest.RestUtil;
import com.igsl.rest.SinglePage;

public class ProjectRole extends Model<ProjectRole> {
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
		return Collections.emptyList();
	}
	@Override
	public Map<String, String> getValues() {
		return Collections.emptyMap();
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
		// Do nothing
	}
	@Override
	public String toString() {
		return this.name + " = " + this.id;
	}
	@Override
	public void setupRestUtil(RestUtil<ProjectRole> util) {
		util.path("/rest/api/3/role")
			.pagination(new SinglePage<ProjectRole>(ProjectRole.class));
	}
	private String id;
	private String name;
	private String description;
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
}
