package com.igsl.workflow.preprocessor;

public interface ValueProcessor {
	public String unpack(String value) throws ValueProcessorException;
	public String pack(String value) throws ValueProcessorException;
}
