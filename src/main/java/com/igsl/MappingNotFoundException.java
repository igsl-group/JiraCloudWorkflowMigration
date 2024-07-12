package com.igsl;

public class MappingNotFoundException extends Exception {
	private static final long serialVersionUID = 1L;
	public MappingNotFoundException(String jsonPath, String mapperName, String modelClassName, String value) {
		super(	"Mapping not found:" + 
				" JSON Path: " + jsonPath + 
				" Mapper: " + mapperName + 
				" Model: " + modelClassName + 
				" Value: " + value);
	}
}
