package com.igsl.workflow;

import java.util.Collections;
import java.util.List;

/**
 * Bulk workflow JSON format returned by Atlassian REST API /rest/api/latest/workflows 
 */
public class BulkWorkflows implements Sortable {
	private List<BulkWorkflow> workflows;
	private List<BulkStatus> statuses;
	
	@Override
	public void sort() {
		Collections.sort(workflows);
		Collections.sort(statuses);
		for (BulkWorkflow bwf : workflows) {
			bwf.sort();
		}
	}
	
	public List<BulkWorkflow> getWorkflows() {
		return workflows;
	}
	public void setWorkflows(List<BulkWorkflow> workflows) {
		this.workflows = workflows;
	}
	public List<BulkStatus> getStatuses() {
		return statuses;
	}
	public void setStatuses(List<BulkStatus> statuses) {
		this.statuses = statuses;
	}
}
