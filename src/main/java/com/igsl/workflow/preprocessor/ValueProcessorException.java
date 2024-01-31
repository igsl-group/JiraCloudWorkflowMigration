package com.igsl.workflow.preprocessor;

public class ValueProcessorException extends Exception {
	private static final long serialVersionUID = 1L;
	public ValueProcessorException() {
		super();
	}
	public ValueProcessorException(String msg) {
		super(msg);
	}
	public ValueProcessorException(Throwable t) {
		super(t);
	}
	public ValueProcessorException(String msg, Throwable t) {
		super(msg, t);
	}
}
