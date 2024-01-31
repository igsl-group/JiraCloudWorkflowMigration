package com.igsl.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.igsl.rest.RestUtil;
import com.igsl.rest.SinglePage;

public class User extends Model<User> {
	private static final String ACCOUNT_TYPE = "ACCOUNT_TYPE";
	private static final String ACTIVE = "ACTIVE";
	@Override
	public String getIdentifier() {
		return this.accountId;
	}
	@Override
	public String getUniqueName() {
		return this.displayName;
	}
	@Override
	public List<String> getColumns() {
		return Arrays.asList(ACCOUNT_TYPE, ACTIVE);
	}
	@Override
	public Map<String, String> getValues() {
		Map<String, String> result = new HashMap<>();
		result.put(ACCOUNT_TYPE, this.accountType);
		result.put(ACTIVE, Boolean.toString(this.active));
		return result;
	}
	@Override
	public void setIdentifier(String identifier) {
		this.accountId = identifier;
	}
	@Override
	public void setUniqueName(String uniqueName) {
		this.displayName = uniqueName;
	}
	@Override
	public void setValues(Map<String, String> values) {
		this.accountType = values.get(ACCOUNT_TYPE);
		this.active = Boolean.parseBoolean(values.get(ACTIVE));
	}
	@Override
	public String toString() {
		return this.displayName + " = " + this.accountId;
	}
	@Override
	public void setupRestUtil(RestUtil<User> util) {
		util.path("/rest/api/3/users/search")
			.pagination(new SinglePage<User>(User.class));
	}
	private String accountId;
	private String accountType;
	private String displayName;
	private boolean active;
	public String getAccountId() {
		return accountId;
	}
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	public String getAccountType() {
		return accountType;
	}
	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
}
