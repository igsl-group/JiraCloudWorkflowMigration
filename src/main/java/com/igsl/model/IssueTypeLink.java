package com.igsl.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.igsl.rest.RestUtil;
import com.igsl.rest.SinglePage;

public class IssueTypeLink extends Model<IssueTypeLink> {
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
	public void setupRestUtil(RestUtil<IssueTypeLink> util) {
		util.path("/rest/api/3/issueLinkType")
			.pagination(new SinglePage<IssueTypeLink>(IssueTypeLink.class, "issueLinkTypes"));
	}
	private String id;
	private String name;
	private String inward;
	private String outward;
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
	public String getInward() {
		return inward;
	}
	public void setInward(String inward) {
		this.inward = inward;
	}
	public String getOutward() {
		return outward;
	}
	public void setOutward(String outward) {
		this.outward = outward;
	}
}
