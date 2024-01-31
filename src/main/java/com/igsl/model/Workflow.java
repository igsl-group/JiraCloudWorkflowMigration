package com.igsl.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.igsl.model.nested.WorkflowId;
import com.igsl.rest.Paged;
import com.igsl.rest.RestUtil;

public class Workflow extends Model<Workflow> {
	private static final String DESCRIPTION = "DESCRIPTION";
	@Override
	@JsonIgnore
	public String getIdentifier() {
		return this.id.getEntityId();
	}
	@Override
	@JsonIgnore
	public String getUniqueName() {
		return this.id.getName();
	}
	@Override
	@JsonIgnore
	public List<String> getColumns() {
		return Arrays.asList(DESCRIPTION);
	}
	@Override
	@JsonIgnore
	public Map<String, String> getValues() {
		Map<String, String> map = new HashMap<>();
		map.put(DESCRIPTION, this.description);
		return map;
	}
	@Override
	@JsonIgnore
	public void setIdentifier(String identifier) {
		this.id.setEntityId(identifier);
	}
	@Override
	@JsonIgnore
	public void setUniqueName(String uniqueName) {
		this.id.setName(uniqueName);
	}
	@Override
	@JsonIgnore
	public void setValues(Map<String, String> values) {
		this.description = values.get(DESCRIPTION);
	}
	@Override
	public String toString() {
		return id.getName() + " = " + id.getEntityId();
	}
	@Override
	@JsonIgnore
	public void setupRestUtil(RestUtil<Workflow> util) {
		util.path("/rest/api/3/workflow/search")
			.path("/rest/api/3/workflow/search")
			.query("workflowName", "")
			.query("isActive", true)
			.pagination(new Paged<Workflow>(Workflow.class));
	}
	private WorkflowId id = new WorkflowId();
	private String description;
	private Map<String, Object> properties = new HashMap<>();
    @JsonAnySetter
    @JsonIgnoreProperties({"id", "description"})
    public void add(String key, Object value) {
        properties.put(key, value);
    }
    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }
	public WorkflowId getId() {
		return id;
	}
	public void setId(WorkflowId id) {
		this.id = id;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
