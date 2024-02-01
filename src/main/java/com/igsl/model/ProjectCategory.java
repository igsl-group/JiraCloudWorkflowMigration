package com.igsl.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.igsl.rest.RestUtil;
import com.igsl.rest.SinglePage;

public class ProjectCategory extends Model<ProjectCategory> {
	public static final String DESCRIPTION = "DESCRIPTION";
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
		return Arrays.asList(DESCRIPTION);
	}
	@Override
	public Map<String, String> getValues() {
		Map<String, String> map = new HashMap<>();
		map.put(DESCRIPTION, this.description);
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
	}
	@Override
	public String toString() {
		return this.name + " = " + this.id;
	}
	@Override
	public void setupRestUtil(RestUtil<ProjectCategory> util) {
		util.path("/rest/api/3/projectCategory")
			.pagination(new SinglePage<ProjectCategory>(ProjectCategory.class));
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
