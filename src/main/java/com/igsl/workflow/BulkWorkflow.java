package com.igsl.workflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Workflow in JSON (bulk format)
 * 
 * Contains deprecated properties:
 * $.transitions[*].from
 * $.transitions[*].to
 * 
 * With helper method to convert them into new properties:
 * $.transitions[*].links
 * $.transitions[*].toStatusReference
 * 
 */
public class BulkWorkflow implements Comparable<BulkWorkflow>, Sortable {
	private String description;
	private String id;
	private String name;
	private List<BulkTransition> transitions;
	private Map<String, Object> otherProperties = new HashMap<>();
    @JsonAnySetter
    @JsonIgnoreProperties({"description", "id", "name", "transitions"})
    public void add(String key, Object value) {
    	otherProperties.put(key, value);
    }
    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return otherProperties;
    }
    
	@Override
	public int compareTo(BulkWorkflow o) {
		if (o != null) {
			return name.compareTo(o.getName());
		}
		return 1;
	}
    
	@Override
	public void sort() {
		Collections.sort(transitions);
		for (BulkTransition bt : transitions) {
			bt.sort();
		}
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
	public List<BulkTransition> getTransitions() {
		return transitions;
	}
	public void setTransitions(List<BulkTransition> transitions) {
		this.transitions = transitions;
	}
}
