package com.igsl.workflow;

import java.io.File;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.igsl.Log;

/**
 * Collection of MapperEntry.
 */
public class MapperConfig {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final ObjectMapper OM = JsonMapper.builder()
			.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
			.build();
	private static final String MAPPER_DIRECTORY = "mapper";	
	private static MapperConfig instance = null;
	private List<MapperEntry> mappers;
	
	static {
		instance = new MapperConfig();
		instance.mappers = new ArrayList<>();
		ObjectReader reader = OM.readerFor(MapperEntry.class);
		URL mapperFolder = MapperConfig.class.getClassLoader().getResource(MAPPER_DIRECTORY);
		File dir = new File(mapperFolder.getPath());
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:{**/*.json,*.json}");
		try {
			Files.list(dir.toPath()).forEach(path -> {
				if (matcher.matches(path)) {
					try {
						String content = Files.readString(path);
						MappingIterator<MapperEntry> it = reader.readValues(content);
						while (it.hasNext()) {
							instance.mappers.add(it.next()); 
						}
					} catch (Exception ex) {
						Log.error(LOGGER, "Error loading mapper configuration", ex);
					}
				}
			});
		} catch (Exception ex) {
			Log.error(LOGGER, "Error loading mapper configuration", ex);
		}
	}
	
	public static MapperConfig getInstance() {
		return instance;
	}
	
	public List<MapperEntry> getMappers() {
		return this.mappers;
	}
}
