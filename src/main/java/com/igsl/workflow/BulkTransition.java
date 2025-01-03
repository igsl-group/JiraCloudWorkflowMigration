package com.igsl.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;

public class BulkTransition implements Comparable<BulkTransition>, Sortable {
	
	public static class WorkflowStatusAndPort implements Comparable<WorkflowStatusAndPort> {
		private int port;
		private String statusReference;
		public int getPort() {
			return port;
		}
		public void setPort(int port) {
			this.port = port;
		}
		public String getStatusReference() {
			return statusReference;
		}
		public void setStatusReference(String statusReference) {
			this.statusReference = statusReference;
		}
		@Override
		public int compareTo(WorkflowStatusAndPort o) {
			if (o != null) {
				return getStatusReference().compareTo(o.getStatusReference());
			}
			return 1;
		}
	}
	
	public static class WorkflowTransitionLink implements Comparable<WorkflowTransitionLink> {
		private int fromPort;
		private int toPort;
		private String fromStatusReference;
		public int getFromPort() {
			return fromPort;
		}
		public void setFromPort(int fromPort) {
			this.fromPort = fromPort;
		}
		public int getToPort() {
			return toPort;
		}
		public void setToPort(int toPort) {
			this.toPort = toPort;
		}
		public String getFromStatusReference() {
			return fromStatusReference;
		}
		public void setFromStatusReference(String fromStatusReference) {
			this.fromStatusReference = fromStatusReference;
		}
		@Override
		public int compareTo(WorkflowTransitionLink o) {
			if (o != null) {
				return getFromStatusReference().compareTo(o.getFromStatusReference());
			}
			return 1;
		}
	}
	
	@Override
	public int compareTo(BulkTransition o) {
		if (o != null) {
			return getId().compareTo(o.getId());
		}
		return 1;
	}
	
	@Override
	public void sort() {
		if (from != null) {
			Collections.sort(from);
		}
		if (links != null) {
			Collections.sort(links);
		}
	}
	
	private String id;
	
	/**
	 * Deprecated way to store associated statuses
	 */
	@JsonView(WorkflowView.Obsolete.class)
	private List<WorkflowStatusAndPort> from;
	/**
	 * Deprecated way to store associated statuses
	 */
	@JsonView(WorkflowView.Obsolete.class)
	private WorkflowStatusAndPort to;
	
	/**
	 * Current way to store associated statuses
	 */
	@JsonView(WorkflowView.Current.class)
	private List<WorkflowTransitionLink> links;
	/**
	 * Current way to store associated statuses
	 */
	@JsonView(WorkflowView.Current.class)
	private String toStatusReference;
	
	@JsonIgnore
	public boolean isObsoleteFormat() {
		if (to != null) {
			return true;
		}
		return false;
	}
	
	/**
	 * Helper method to convert from (from/to) to (links/toStatusReference)
	 */
	public void convertStatusReference() {
		if (from != null) {
			links = new ArrayList<>();
			for (WorkflowStatusAndPort item : from) {
				WorkflowTransitionLink newItem = new WorkflowTransitionLink();
				newItem.setFromPort(item.getPort());
				newItem.setFromStatusReference(item.getStatusReference());
				if (to != null) {
					newItem.setToPort(to.getPort());
				}
				links.add(newItem);
			}
			from = null;
		}
		if (to != null) {
			toStatusReference = to.getStatusReference();
			to = null;
		}
	}
	
	@JsonView({WorkflowView.Obsolete.class, WorkflowView.Current.class})
	private Map<String, Object> otherProperties = new HashMap<>();
    @JsonAnySetter
    @JsonIgnoreProperties({"id", "from", "to", "links", "toStatusReference"})
    public void add(String key, Object value) {
    	otherProperties.put(key, value);
    }
    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return otherProperties;
    }
    
	public List<WorkflowStatusAndPort> getFrom() {
		return from;
	}
	public void setFrom(List<WorkflowStatusAndPort> from) {
		this.from = from;
	}
	public WorkflowStatusAndPort getTo() {
		return to;
	}
	public void setTo(WorkflowStatusAndPort to) {
		this.to = to;
	}
	public List<WorkflowTransitionLink> getLinks() {
		return links;
	}
	public void setLinks(List<WorkflowTransitionLink> links) {
		this.links = links;
	}
	public String getToStatusReference() {
		return toStatusReference;
	}
	public void setToStatusReference(String toStatusReference) {
		this.toStatusReference = toStatusReference;
	}
	public Map<String, Object> getOtherProperties() {
		return otherProperties;
	}
	public void setOtherProperties(Map<String, Object> otherProperties) {
		this.otherProperties = otherProperties;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
}
