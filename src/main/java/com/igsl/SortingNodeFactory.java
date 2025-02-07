package com.igsl;

import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Jackson when serializing using JsonNode, the ORDER_MAP_ENTRIES_BY_KEYS feature does not work.
 * To force it to sort, replace node factory by using TreeMap instead of LinkedGHashMap in the original.
 */
public class SortingNodeFactory extends JsonNodeFactory {
	private static final long serialVersionUID = 1L;

	@Override
    public ObjectNode objectNode() {
      return new ObjectNode(this, new TreeMap<String, JsonNode>());
    }
}