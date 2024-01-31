package com.igsl.model;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.igsl.Config;
import com.igsl.rest.RestUtil;

/**
 * Mark a POJO class as a usable class for ModelUtil.
 * @param <T>
 */
public abstract class Model<T> {
	public static final String IDENTIFIER = "IDENTIFIER";
	public static final String UNIQUE_NAME = "UNIQUE_NAME";
	/**
	 * Get identifier.
	 */
	public abstract String getIdentifier();
	/**
	 * Set identifier.
	 */
	public abstract void setIdentifier(String identifier);
	/**
	 * Get the name that can be used for automatic matching.
	 */
	public abstract String getUniqueName();	
	/**
	 * Set unique name.
	 */
	public abstract void setUniqueName(String uniqueName);
	/**
	 * Get CSV column headers. 
	 * Do not include identifier and unique name, those two will be added automatically.
	 */
	public abstract List<String> getColumns();
	/**
	 * Get values for CSV record matching return value of getColumns().
	 */
	public abstract Map<String, String> getValues();
	/**
	 * Update members based on provided data.
	 */
	public abstract void setValues(Map<String, String> values);
	/**
	 * Get string representation.
	 */
	public abstract String toString();
	/**
	 * Configure RestUtil for export.
	 * @param util RestUtil instance.
	 */
	public abstract void setupRestUtil(RestUtil<T> util);
	/**
	 * Default export method. 
	 * Override if needed, e.g. when you need to invoke more REST APIs for nested objects.
	 * @throws URISyntaxException 
	 * @throws IllegalStateException 
	 * @throws JsonProcessingException 
	 * @throws JsonMappingException 
	 * @throws UnsupportedEncodingException 
	 */
	public List<T> export(Config config, Class<T> dataClass, String host) 
			throws 	JsonMappingException, 
					JsonProcessingException, 
					IllegalStateException, 
					URISyntaxException, 
					UnsupportedEncodingException {
		RestUtil<T> util = RestUtil.getInstance(dataClass)
				.config(config)
				.host(host);
		setupRestUtil(util);
		return util.requestAllPages();
	}	
	/**
	 * Export Model<U> items.
	 * Static version of .export().
	 */
	@SuppressWarnings("unchecked")
	public static final <U extends Model<U>> List<U> exportObject(Config config, Class<U> dataClass, String host) 
			throws 	UnsupportedEncodingException, 
					JsonMappingException, 
					JsonProcessingException, 
					IllegalStateException, 
					URISyntaxException, 
					InstantiationException, 
					IllegalAccessException, 
					IllegalArgumentException, 
					InvocationTargetException, 
					NoSuchMethodException, 
					SecurityException {
		// Get a dummy instance of POJO class to invoke its .export() method
		U cls = dataClass.getConstructor().newInstance();
		Method method = dataClass.getMethod("export", Config.class, Class.class, String.class);
		return (List<U>) method.invoke(cls, config, dataClass, host);
	}
}