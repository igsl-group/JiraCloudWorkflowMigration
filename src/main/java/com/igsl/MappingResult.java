package com.igsl;

public class MappingResult {
	private int changedCount = 0;
	private int mappingNotFoundCount = 0;
	public void addChanged() {
		this.changedCount++;
	}
	public void addMappingNotFound() {
		this.mappingNotFoundCount++;
	}
	public void add(MappingResult r) { 
		this.changedCount += r.getChangedCount();
		this.mappingNotFoundCount += r.getMappingNotFoundCount();
	}
	// Generated
	public int getChangedCount() {
		return changedCount;
	}
	public int getMappingNotFoundCount() {
		return mappingNotFoundCount;
	}
	
}
