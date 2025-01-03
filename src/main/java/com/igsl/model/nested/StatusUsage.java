package com.igsl.model.nested;

import java.util.List;

import com.igsl.model.Project;

public class StatusUsage {
	private List<String> issueTypes;
	private Project project;
	public List<String> getIssueTypes() {
		return issueTypes;
	}
	public void setIssueTypes(List<String> issueTypes) {
		this.issueTypes = issueTypes;
	}
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}
}
