package com.igsl;

import java.io.InputStream;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class Config {
	private static final String SCHEME = "https";
	private static final String CONFIG_FILENAME = "Config.json";
	private static final Logger LOGGER = LogManager.getLogger();
	private static final ObjectMapper OM = JsonMapper.builder()
			.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
			.build();
	private static Config instance;
	private int rate;
	private int period;
	private String objectTypePackage;
	private List<String> objectTypes;
	private String email;
	private String token;
	static {
		try (InputStream is = Config.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME)) {
			instance = OM.readerFor(Config.class).readValue(is);
		} catch (Exception ex) {
			Log.error(LOGGER, "Error loading configuration " + CONFIG_FILENAME, ex);
		}
	}
	public static Config getInstance() {
		return instance;
	}
	@JsonIgnore
	public String getScheme() {
		return SCHEME;
	}
	// Generated
	public int getRate() {
		return rate;
	}
	public void setRate(int rate) {
		this.rate = rate;
	}
	public int getPeriod() {
		return period;
	}
	public void setPeriod(int period) {
		this.period = period;
	}
	public String getObjectTypePackage() {
		return objectTypePackage;
	}
	public void setObjectTypePackage(String objectTypePackage) {
		this.objectTypePackage = objectTypePackage;
	}
	public List<String> getObjectTypes() {
		return objectTypes;
	}
	public void setObjectTypes(List<String> objectTypes) {
		this.objectTypes = objectTypes;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
}
