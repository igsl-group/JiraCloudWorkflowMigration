package com.igsl.workflow;

import java.util.List;

import com.igsl.model.nested.StatusScope;
import com.igsl.model.nested.StatusUsage;

/**
 * Global status in bulk workflow format
 */
public class BulkStatus implements Comparable<BulkStatus> {
	private String description;
	private String id;
	private String name;
	private StatusScope scope;
	private String statusCategory;
	private String statusReference;
	private List<StatusUsage> usages;
	
	@Override
	public int compareTo(BulkStatus o) {
		if (o != null) {
			return this.getId().compareTo(o.getId());
		}
		return 1;
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
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
	public StatusScope getScope() {
		return scope;
	}
	public void setScope(StatusScope scope) {
		this.scope = scope;
	}
	public String getStatusCategory() {
		return statusCategory;
	}
	public void setStatusCategory(String statusCategory) {
		this.statusCategory = statusCategory;
	}
	public String getStatusReference() {
		return statusReference;
	}
	public void setStatusReference(String statusReference) {
		this.statusReference = statusReference;
	}
	public List<StatusUsage> getUsages() {
		return usages;
	}
	public void setUsages(List<StatusUsage> usages) {
		this.usages = usages;
	}
}
