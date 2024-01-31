package com.igsl.workflow;

import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Workflow mapper configuration item.
 */
public class MapperEntry {
	/**
	 * Unique name
	 */
	private String name;
	/**
	 * Starting from root, an array of JSON path elements.
	 * Root "$" is already included implicitly. 
	 * For arrays, specify [*] at the end to indicate processing all elements.
	 */
	private List<String> jsonPaths;
	/**
	 * Class to process value.
	 * ScriptRunner compresses custom script in a specific format (ScriptRunnrCompressedData class).
	 * Most values will leave this as null.
	 */
	private String valueProcessorClass;
	/**
	 * Regular expression for value, to locate object ids to be remapped.
	 * This is used in .find(), so you can capture remap multiple ids.
	 */
	private String valueRegex;
	/**
	 * Capture group in valueRegex containing the object id to be remapped.
	 * If empty, default is 0 (match whole expression).
	 * This group value will be looked up as an object id and replaced.
	 */
	private List<Integer> valueCaptureGroups;
	/**
	 * Replacement for value. Syntax is for Matcher class.
	 * Default is $0.
	 * If your capture group is not 0, you should include the other parts. 
	 * 
	 * e.g. valueRegex = customfield_([0-9]+)
	 * 		The value found in [0-9]+ is remapped as an object id.
	 * 		valueReplacement = customfield_$1.
	 */
	private String valueReplacement;
	/**
	 * Model class name.
	 * This is used to determine which object mapping data to use.
	 * If null, valueReplacement is used to perform regex replace without remapping.
	 */
	private String modelClass;
	/**
	 * Apply this mapping only to these workflow names.
	 * Default is an empty list, which applies to all workflows.
	 */
	private List<String> targetWorkflowNames;
	@JsonIgnore
	private Pattern valuePattern;
	@JsonIgnore
	public Pattern getValuePattern() {
		if (valuePattern != null) {
			return valuePattern;
		}
		return Pattern.compile(this.valueRegex);
	}	
	/**
	 * Get model class name without package
	 */
	@JsonIgnore
	public String getModelClassName() {
		if (modelClass != null) {
			int index = modelClass.lastIndexOf(".");
			return modelClass.substring(index + 1);
		}
		return "";
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValueRegex() {
		return valueRegex;
	}
	public void setValueRegex(String valueRegex) {
		this.valueRegex = valueRegex;
	}
	public String getValueReplacement() {
		return valueReplacement;
	}
	public void setValueReplacement(String valueReplacement) {
		this.valueReplacement = valueReplacement;
	}
	public String getModelClass() {
		return modelClass;
	}
	public void setModelClass(String modelClass) {
		this.modelClass = modelClass;
	}
	public List<String> getTargetWorkflowNames() {
		return targetWorkflowNames;
	}
	public void setTargetWorkflowNames(List<String> targetWorkflowNames) {
		this.targetWorkflowNames = targetWorkflowNames;
	}
	public String getValueProcessorClass() {
		return valueProcessorClass;
	}
	public void setValueProcessorClass(String valueProcessorClass) {
		this.valueProcessorClass = valueProcessorClass;
	}
	public List<Integer> getValueCaptureGroups() {
		return valueCaptureGroups;
	}
	public void setValueCaptureGroups(List<Integer> valueCaptureGroups) {
		this.valueCaptureGroups = valueCaptureGroups;
	}
	public List<String> getJsonPaths() {
		return jsonPaths;
	}
	public void setJsonPaths(List<String> jsonPaths) {
		this.jsonPaths = jsonPaths;
	}
}
