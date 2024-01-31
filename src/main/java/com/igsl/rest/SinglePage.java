package com.igsl.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * For REST APIs without pagination, where the result is a top-level item or array.
 * @param <T>
 */
public class SinglePage<T> extends Pagination<T> {
	private List<T> values;
	
	public SinglePage(Class<T> dataClass) {
		super(dataClass);
	}
	
	@Override
	public void reset() {
		values = null;
	}

	@Override
	public void setup(RestUtil<?> util) {
		// Do nothing
	}

	@Override
	public void setResponse(Response response, ObjectMapper om) 
			throws JsonProcessingException, JsonMappingException {
		JsonNode node = om.readTree(response.readEntity(String.class));
		if (node != null) {
			values = new ArrayList<>();
			if (node.isArray()) {
				for (JsonNode item : node) {
					T value = om.treeToValue(item, dataClass);
					values.add(value);
				}
			} else {
				T value = om.treeToValue(node, dataClass);
				values.add(value);
			}
		}
	}

	@Override
	public boolean hasMore() {
		return false;
	}

	@Override
	public List<T> getObjects() {
		return values;
	}

}
