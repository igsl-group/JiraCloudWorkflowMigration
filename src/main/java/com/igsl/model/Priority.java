package com.igsl.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.igsl.rest.Paged;
import com.igsl.rest.RestUtil;

public class Priority extends Model<Priority> {
	private static final String DESCRIPTION = "DESCRIPTION";
	private static final String STATUS_COLOR = "STATUS_COLOR";
	private static final String ICON_URL = "ICON_URL";
	private static final String IS_DEFAULT = "IS_DEFAULT";
	
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
		return Arrays.asList(DESCRIPTION, STATUS_COLOR, ICON_URL, IS_DEFAULT);
	}
	@Override
	public Map<String, String> getValues() {
		Map<String, String> result = new HashMap<>();
		result.put(DESCRIPTION, this.description);
		result.put(STATUS_COLOR, this.statusColor);
		result.put(ICON_URL, this.iconUrl);
		result.put(IS_DEFAULT, Boolean.toString(this.defaultPriority));
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
		this.description = values.get(DESCRIPTION);
		this.iconUrl = values.get(ICON_URL);
		this.statusColor = values.get(STATUS_COLOR);
		this.defaultPriority = Boolean.parseBoolean(values.get("IS_DEFAULT"));
	}
	@Override
	public String toString() {
		return this.name + " = " + this.id;
	}
	@Override
	public void setupRestUtil(RestUtil<Priority> util) {
		util.path("/rest/api/3/priority/search")
			.pagination(new Paged<Priority>(Priority.class));
	}
	private String id;
	private String name;
	private String description;
	private String statusColor;
	private String iconUrl;
	@JsonProperty("isDefault")
	private boolean defaultPriority;
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
	public boolean isDefaultPriority() {
		return defaultPriority;
	}
	public void setDefaultPriority(boolean defaultPriority) {
		this.defaultPriority = defaultPriority;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getStatusColor() {
		return statusColor;
	}
	public void setStatusColor(String statusColor) {
		this.statusColor = statusColor;
	}
	public String getIconUrl() {
		return iconUrl;
	}
	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}
}
