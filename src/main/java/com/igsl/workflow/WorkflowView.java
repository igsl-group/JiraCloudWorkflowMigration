package com.igsl.workflow;

/**
 * Interface for customizing Jackson serialization
 */
public interface WorkflowView {
	interface Obsolete {}  
    interface Current {} 
	interface Hybrid extends Obsolete, Current {}
}
