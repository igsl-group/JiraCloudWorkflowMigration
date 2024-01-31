package com.igsl.model.nested;

import com.igsl.model.Project;

public class StatusScope {
	private String type;
	private Project project;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}
}
