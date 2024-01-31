package com.igsl.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.igsl.model.nested.StatusScope;
import com.igsl.rest.Paged;
import com.igsl.rest.RestUtil;

/**
 * POJO for Status
 */
public class Status extends Model<Status> {
	private static final String STATUS_CATEGORY = "STATUS_CATEGORY";
	private static final String TYPE = "TYPE";
	private static final String PROJECT = "PROJECT";
	private static final String DESCRIPTION = "DESCRIPTION";
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
		return Arrays.asList(STATUS_CATEGORY, TYPE, PROJECT, DESCRIPTION);
	}
	@Override
	public Map<String, String> getValues() {
		Map<String, String> result = new HashMap<>();
		result.put(STATUS_CATEGORY, this.statusCategory);
		result.put(TYPE, this.scope.getType());
		result.put(PROJECT, ((this.scope.getProject() != null)? this.scope.getProject().getKey() : ""));
		result.put(DESCRIPTION, this.description);
		return result;
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
		this.statusCategory = values.get(STATUS_CATEGORY);
		this.description = values.get(DESCRIPTION);
		this.scope = new StatusScope();
		this.scope.setType(values.get(TYPE));
		String projectKey = values.get(PROJECT);
		if (projectKey != null) {
			Project proj = new Project();
			proj.setKey(projectKey);
			this.scope.setProject(proj);
		}
	}
	@Override
	public String toString() {
		return this.name + " = " + this.id;
	}
	@Override
	public void setupRestUtil(RestUtil<Status> util) {
		util.path("/rest/api/3/statuses/search")
			.pagination(new Paged<Status>(Status.class));
	}
	private String id;
	private String name;
	private String statusCategory;
	private StatusScope scope = new StatusScope();
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
	public String getStatusCategory() {
		return statusCategory;
	}
	public void setStatusCategory(String statusCategory) {
		this.statusCategory = statusCategory;
	}
	public StatusScope getScope() {
		return scope;
	}
	public void setScope(StatusScope scope) {
		this.scope = scope;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
