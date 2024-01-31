package com.igsl.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.igsl.rest.Paged;
import com.igsl.rest.RestUtil;

public class Group extends Model<Group> {
	@Override
	public String getIdentifier() {
		return this.groupId;
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
		this.groupId = identifier;
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
		return this.name + " = " + this.groupId;
	}
	@Override
	public void setupRestUtil(RestUtil<Group> util) {
		util.path("/rest/api/3/group/bulk")
			.pagination(new Paged<Group>(Group.class));
	}
	private String name;
	private String groupId;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
}
