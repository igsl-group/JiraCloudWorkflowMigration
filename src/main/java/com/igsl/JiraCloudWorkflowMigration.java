package com.igsl;

import java.io.FileReader;
import java.io.FileWriter;
import java.net.http.HttpResponse;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.igsl.model.Model;
import com.igsl.model.Workflow;
import com.igsl.rest.Paged;
import com.igsl.rest.RestUtil;
import com.igsl.workflow.MapperConfig;
import com.igsl.workflow.MapperEntry;
import com.igsl.workflow.preprocessor.ScriptRunnerCompressedData;
import com.igsl.workflow.preprocessor.ValueProcessor;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

public class JiraCloudWorkflowMigration {
	
	private static final Logger LOGGER = LogManager.getLogger();
	private static final String NEWLINE = System.getProperty("line.separator");
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd_HHmmss");
	private static final ObjectMapper OM = new ObjectMapper()
				.enable(SerializationFeature.INDENT_OUTPUT);
	private static final Configuration JSONPATH_CONFIG_READ_PATH = Configuration.builder()
			   .options(com.jayway.jsonpath.Option.AS_PATH_LIST)
			   .build();
	
	private static final String MATCH_RESULT = "MATCH_RESULT";
	private static final String MATCH_RESULT_MATCHED = "Matched";
	private static final String MATCH_RESULT_NO_MATCH = "No Match";
	private static final String MATCH_RESULT_COLLISION = "Collision";
	private static final String MATCH_WITH = "MATCH_WITH";
	private static final String DELIMITER = "|";
	
	private static Option emailOption;
	private static Option tokenOption;
	
	private static Option targetHostOption;
	private static Option sourceDirOption;
	private static Option targetDirOption;
	private static Option matchDirOption;
	
	private static Options exportOptions;
	private static Option exportOption;
	
	private static Options matchOptions;
	private static Option matchOption;
	
	private static Options remapWorkflowOptions;
	private static Option remapWorkflowOption;
	
	private static Options updateWorkflowOptions;
	private static Option updateWorkflowOption;
	
	private static Option unpackOption;
	private static Option packOption;
	private static Option scriptOption;
	private static Options unpackOptions;
	private static Options packOptions;
	
	static {
		emailOption = Option.builder()
				.desc("User email")
				.argName("User email")
				.option("u")
				.longOpt("user")
				.required()
				.hasArg()
				.build();
		tokenOption = Option.builder()
				.desc("API token")
				.argName("API token")
				.option("p")
				.longOpt("apitoken")
				.hasArg()
				.build();
		
		targetHostOption = Option.builder()
				.desc("Target site")
				.argName("Target site")
				.option("t")
				.longOpt("target")
				.required()
				.hasArg()
				.build();
		
		sourceDirOption = Option.builder()
				.argName("Directory")
				.desc("Directory containing objects exported from source site")
				.option("sd")
				.longOpt("sourcedir")
				.required()
				.hasArg()
				.build();
		targetDirOption = Option.builder()
				.argName("Directory")
				.desc("Directory containing objects exported from target site")
				.option("td")
				.longOpt("targetdir")
				.required()
				.hasArg()
				.build();
		matchDirOption = Option.builder()
				.argName("Directory")
				.desc("Directory containing result from match")
				.option("md")
				.longOpt("matchdir")
				.hasArg()
				.required()
				.build();

		// Update workflow on site using directory
		updateWorkflowOption = Option.builder()
				.desc("Update workflow in target site")
				.option("uw")
				.longOpt("updateworkflow")
				.required()
				.build();
		updateWorkflowOptions = new Options()
				.addOption(updateWorkflowOption)
				.addOption(targetHostOption)
				.addOption(sourceDirOption)
				.addOption(emailOption)
				.addOption(tokenOption);
				
		// For pack/unpack ScriptRunner scripts
		unpackOption = Option.builder()
				.desc("Unpack ScriptRunner script")
				.option("u")
				.longOpt("unpack")
				.build();
		packOption = Option.builder()
				.desc("Package ScriptRunner script")
				.option("p")
				.longOpt("pack")
				.build();
		scriptOption = Option.builder()
				.desc("Script value")
				.argName("Script value")
				.option("s")
				.longOpt("script")
				.hasArg()
				.required()
				.build();
		unpackOptions = new Options()
				.addOption(unpackOption)
				.addOption(scriptOption);
		packOptions = new Options()
				.addOption(packOption)
				.addOption(scriptOption);		
		
		// Match objects exported from two sites
		matchOption = Option.builder()
				.argName("Match objects between two sites")
				.option("m")
				.longOpt("match")
				.required()
				.build();
		matchOptions = new Options()
				.addOption(matchOption)
				.addOption(sourceDirOption)
				.addOption(targetDirOption);
		
		// Remap workflow content
		remapWorkflowOption = Option.builder()
				.argName("Remap workflows")
				.option("rw")
				.longOpt("remapworkflow")
				.required()
				.build();
		remapWorkflowOptions = new Options()
				.addOption(remapWorkflowOption)
				.addOption(matchDirOption)
				.addOption(sourceDirOption);

		// Export objects
		exportOption = Option.builder()
				.argName("Export objects from site")
				.option("e")
				.longOpt("export")
				.required()
				.build();
		exportOptions = new Options()
				.addOption(targetHostOption)
				.addOption(exportOption)
				.addOption(emailOption)
				.addOption(tokenOption);
	}
	
	private static Path getObjectFileName(Path folder, String modelClass) {
		return folder.resolve(modelClass + ".csv");
	}
	
	private static void export(Config config, String host, Path outputDir, CommandLine cmd) throws Exception {
		String packageName = config.getObjectTypePackage();
		for (String className : config.getObjectTypes()) {
			@SuppressWarnings("rawtypes")
			Class cls = Class.forName(packageName + "." + className);
			@SuppressWarnings("unchecked")
			List<Model<?>> list = Model.exportObject(config, cls, host);
			int count = 0;
			if (list.size() != 0) {
				List<String> columns = new ArrayList<>();
				columns.addAll(Arrays.asList(Model.IDENTIFIER, Model.UNIQUE_NAME));
				columns.addAll(list.get(0).getColumns());
				Path p = getObjectFileName(outputDir, className);
				try (	FileWriter fw = new FileWriter(p.toFile()); 
						CSVPrinter printer = new CSVPrinter(fw, 
								CSV.getCSVWriteFormat(columns))) {
					for (Model<?> m : list) {
						Log.debug(LOGGER, className + ": " + m.getValues());
						List<String> values = new ArrayList<>();
						values.addAll(Arrays.asList(m.getIdentifier(), m.getUniqueName()));
						for (String column : list.get(0).getColumns()) {
							values.add(m.getValues().get(column));
						}
						CSV.printRecord(printer, values.toArray());
						count++;
					}
					Log.info(LOGGER, "Saved " + className + " to " + p.toString());
				}				
			}
			Log.info(LOGGER, "Total " + className + ": " + count);
		}
	}
	
	private static void match(Config config, Path outputDir, CommandLine cmd) throws Exception {
		String sourceDir = cmd.getOptionValue(sourceDirOption);
		Path source = Paths.get(sourceDir);
		String targetDir = cmd.getOptionValue(targetDirOption);
		Path target = Paths.get(targetDir);
		String packageName = config.getObjectTypePackage();
		for (String className : config.getObjectTypes()) {
			Class<?> cls = Class.forName(packageName + "." + className);
			Model<?> model = (Model<?>) cls.getConstructor().newInstance();
			// Read sandbox into map of identifier to Model
			Map<String, Model<?>> sandboxMap = new HashMap<>();
			try (	FileReader fr = new FileReader(getObjectFileName(source, className).toFile()); 
					CSVParser parser = new CSVParser(fr, CSV.getCSVReadFormat())) {
				for (CSVRecord record : parser.getRecords()) {
					Model<?> m = (Model<?>) cls.getConstructor().newInstance();
					m.setIdentifier(record.get(Model.IDENTIFIER));
					m.setUniqueName(record.get(Model.UNIQUE_NAME));
					m.setValues(record.toMap());
					sandboxMap.put(m.getIdentifier(), m);
				}
			}
			// Read production into map of unique name to list of models sharing same unique name
			Map<String, List<Model<?>>> productionMap = new HashMap<>();
			try (	FileReader fr = new FileReader(getObjectFileName(target, className).toFile()); 
					CSVParser parser = new CSVParser(fr, CSV.getCSVReadFormat())) {
				for (CSVRecord record : parser.getRecords()) {
					Model<?> m = (Model<?>) cls.getConstructor().newInstance();
					m.setIdentifier(record.get(Model.IDENTIFIER));
					m.setUniqueName(record.get(Model.UNIQUE_NAME));
					m.setValues(record.toMap());
					if (!productionMap.containsKey(m.getUniqueName())) {
						productionMap.put(m.getUniqueName(), new ArrayList<>());
					}
					productionMap.get(m.getUniqueName()).add(m);
				}
			}
			// Find matches using sandbox
			Path outputFile = getObjectFileName(outputDir, className);
			List<String> headers = new ArrayList<>();
			headers.add(Model.IDENTIFIER);
			headers.add(Model.UNIQUE_NAME);
			headers.addAll(model.getColumns());
			headers.add(MATCH_RESULT);
			headers.add(MATCH_WITH);
			try (	FileWriter fw = new FileWriter(outputFile.toFile()); 
					CSVPrinter printer = new CSVPrinter(fw, CSV.getCSVWriteFormat(headers))) {
				for (Model<?> sandboxItem : sandboxMap.values()) {
					String uniqueName = sandboxItem.getUniqueName();
					List<String> v = new ArrayList<>();
					v.add(sandboxItem.getIdentifier());
					v.add(sandboxItem.getUniqueName());
					v.addAll(sandboxItem.getValues().values());
					if (productionMap.containsKey(uniqueName)) {
						List<Model<?>> modelList = productionMap.get(uniqueName);
						if (modelList.size() == 1) {
							// Matched
							v.add(MATCH_RESULT_MATCHED);
							v.add(modelList.get(0).getIdentifier());
						} else {
							// Collision
							v.add(MATCH_RESULT_COLLISION);
							StringBuilder sb = new StringBuilder();
							for (Model<?> m : modelList) {
								sb.append(DELIMITER).append(m.getIdentifier());
							}
							sb.deleteCharAt(0);
							v.add(sb.toString());
						}
					} else {
						// Source item is not matched
						v.add(MATCH_RESULT_NO_MATCH);
						v.add("");
					}
					CSV.printRecord(printer, v);
				}
				Log.info(LOGGER, "Saved " + outputFile.toString());
			}
		}
		Log.info(LOGGER, 
				"Please check \"" + MATCH_RESULT + "\" column of each output file" + 
				" and resolve \"" + MATCH_RESULT_COLLISION + "\" and \"" + MATCH_RESULT_NO_MATCH + "\""
		);
	}
	
	private static Map<String, String> extractScriptRunScript(String workflowJson) throws Exception {
		DocumentContext ctx = JsonPath.parse(workflowJson);
		DocumentContext pathCtx = JsonPath.using(JSONPATH_CONFIG_READ_PATH).parse(workflowJson);
		return extractScriptRunScript(ctx, pathCtx);		
	}
	
	private static Map<String, String> extractScriptRunScript(DocumentContext ctx, DocumentContext pathCtx) {
		Map<String, String> result = new HashMap<>();
		ScriptRunnerCompressedData scd = new ScriptRunnerCompressedData();
		List<String> paths = new ArrayList<>();
		try {
			/*
			List<String> actions = pathCtx.read("$[*].workflows[*].transitions[*].actions[*]");
			for (String action : actions) {
				paths = pathCtx.read(action + ".parameters[?(@.appKey==\"com.onresolve.jira.groovy.groovyrunner__script-postfunction\")]");
			}
			*/
			paths = pathCtx.read("$[*].workflows[*].transitions[*].actions[*].parameters[?(@.appKey==\"com.onresolve.jira.groovy.groovyrunner__script-postfunction\")]");
		} catch (PathNotFoundException pnfex) {
			// Log.error(LOGGER, "parameters Path not found", pnfex);
		}
		for (String path : paths) {
			try {
				String id = ctx.read(path + ".id");
				String value = ctx.read(path + ".config");
				String unpacked = scd.unpack(value);
				JsonNode node = OM.readTree(unpacked);
				String code = OM.writeValueAsString(node);
				result.put(id, code);
			} catch (PathNotFoundException pnfex) {
				// Log.error(LOGGER, "id/config Path not found", pnfex);
			} catch (Exception ex) {
				Log.error(LOGGER, "Error parsing workflow", ex);
			}
		}
		return result;
	}
	
	private static String getWorkflowFileName(String workflowName) {
		return "Workflow - " + workflowName.replaceAll("\\W+", " ") + ".json";	
	}
	
	private static void exportWorkflow(Config config, String host, Path outputDir, CommandLine cmd) 
			throws Exception {
		try {
			List<Workflow> workflows = RestUtil.getInstance(Workflow.class)
				.config(config)
				.host(host)
				.path("/rest/api/3/workflow/search")
				.query("workflowName", "")
				.query("isActive", true)
				.pagination(new Paged<Workflow>(Workflow.class))
				.requestAllPages();
			int count = 0;
			for (Workflow workflow : workflows) {
				String workflowName = workflow.getId().getName();
				try {
					List<String> workflowNames = new ArrayList<>();
					workflowNames.add(workflowName);
					Map<String, Object> payload = new HashMap<>();
					payload.put("workflowNames", workflowNames);
					// Get workflow in bulk format
					List<Object> wf = RestUtil.getInstance(Object.class)
							.config(config)
							.host(host)
							.path("/rest/api/3/workflows")
							.method(HttpMethod.POST)
							.payload(payload)
							.requestAllPages();
					// Replace characters invalid as filename
					String fileName = getWorkflowFileName(workflowName);
					Path outputFile = outputDir.resolve(fileName);
					String json = OM.writeValueAsString(wf);
					Files.writeString(outputFile, json);
					Log.info(LOGGER, "Saved Workflow: " + workflowName + " to " + outputFile.toString());
					Map<String, String> codes = extractScriptRunScript(json);
					for (Map.Entry<String, String> entry : codes.entrySet()) {
						Path codeFile = outputDir.resolve(
								getWorkflowFileName(workflowName) + 
								" - " + entry.getKey() + ".groovy");
						Files.writeString(codeFile, entry.getValue());
						Log.info(LOGGER, "Saved ScriptRunner post-function: " + codeFile.toString());
					}
					count++;
				} catch (Exception ex) {
					Log.error(LOGGER, "Error exporting workflow: " + workflowName, ex);
				}
			}
			Log.info(LOGGER, "Workflows exported: " + count + "/" + workflows.size());
		} catch (Exception ex) {
			Log.error(LOGGER, "Error retriving list of workflows", ex);
		}
	}
	
	// Cache of mappings
	private static int MAP_SIZE = 20;	// Max. no. of maps to keep in mapCache, FIFO
	private static Map<String, Map<String, String>> mapCache = new LinkedHashMap<>(MAP_SIZE);
	private static Map<String, String> loadMap(Path matchDir, String modelClass) throws Exception {
		if (mapCache.containsKey(modelClass)) {
			return mapCache.get(modelClass);
		}		
		if (mapCache.size() >= MAP_SIZE) {
			// Drop first item
			mapCache.remove(mapCache.keySet().iterator().next());
		}
		Path p = getObjectFileName(matchDir, modelClass);
		Map<String, String> map = new HashMap<>();
		try (	FileReader fr = new FileReader(p.toFile());
				CSVParser parser = new CSVParser(fr, CSV.getCSVReadFormat())) {
			for (CSVRecord record : parser.getRecords()) {
				String matchResult = record.get(MATCH_RESULT);
				if (MATCH_RESULT_MATCHED.equals(matchResult)) {
					String key = record.get(Model.IDENTIFIER);
					String value = record.get(MATCH_WITH);
					map.put(key, value);
				}
			}
			mapCache.put(modelClass, map);
		}
		return map;
	}
	
	private static String remapValue(Path matchDir, MapperEntry mapper, String jsonPath, String value) 
			throws Exception {
		String modelClassName = mapper.getModelClassName();
		String mapperName = mapper.getName();
		String newValue = value;
		Log.debug(LOGGER, "Remapping " + jsonPath + " " + modelClassName + " value: " + value);
		Map<String, String> map = loadMap(matchDir, mapper.getModelClassName());
		if (map.containsKey(value)) {
			newValue = map.get(value);
			Log.debug(LOGGER, 
					"Remapped: " + jsonPath + 
					" Mapper: " + mapperName + 
					" Model: " + modelClassName + 
					" Value from: " + value + 
					" Value to: " + newValue);
		} else {
			Log.warn(LOGGER, 
					"No mapping found for: " + jsonPath + 
					" Mapper: " + mapperName + 
					" Model: " + modelClassName + 
					" Value: " + value);
			throw new MappingNotFoundException(jsonPath, mapperName, modelClassName, value);
		}
		return newValue;
	}
	
	private static String processValue(Path matchDir, MapperEntry mapper, String jsonPath, String value) 
			throws Exception {
		String modelClassName = mapper.getModelClassName();
		Log.debug(LOGGER, "Processing " + modelClassName + " value: " + value);
		ValueProcessor processor = null;
		if (mapper.getValueProcessorClass() != null) {
			Class<?> processorClass = Class.forName(mapper.getValueProcessorClass());
			processor = (ValueProcessor) processorClass.getConstructor().newInstance();
		}
		String newValue = value;
		if (processor != null) {
			newValue = processor.unpack(newValue);
			Log.debug(LOGGER, "Unpacked " + modelClassName + " value: " + newValue);
		}
		Matcher m = mapper.getValuePattern().matcher(newValue);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			List<Integer> groupList = mapper.getValueCaptureGroups();
			if (groupList.size() == 0) {
				groupList.add(0);
			}
			String split = mapper.getSplitRegex();
			String quote = mapper.getQuote();
			Map<Integer, String> replacements = new HashMap<>();
			if (mapper.getModelClass() != null) {
				for (int groupId : groupList) {
					String groupValue = m.group(groupId);
					if (groupValue != null) {
						if (split != null) {
							String[] values = groupValue.split(split);
							StringBuilder remappedValues = new StringBuilder();
							for (String v : values) {
								boolean dequoted = false;
								if (quote != null) {
									if (v.startsWith(quote) && v.endsWith(quote)) {
										v = v.substring(
												quote.length(), v.length() - quote.length());
										dequoted = true;
									}
								}
								String remapped = remapValue(matchDir, mapper, jsonPath, v);
								if (quote != null && dequoted) {
									remapped = quote + remapped + quote;
								}
								remappedValues.append(split).append(remapped);
							}
							remappedValues.delete(0, split.length());
							replacements.put(groupId, remappedValues.toString());
						} else {
							boolean dequoted = false;
							if (quote != null) {
								if (groupValue.startsWith(quote) && groupValue.endsWith(quote)) {
									groupValue = groupValue.substring(
											quote.length(), groupValue.length() - quote.length());
									dequoted = true;
								}
							}
							String remappedValue = remapValue(matchDir, mapper, jsonPath, groupValue);
							if (quote != null && dequoted) {
								remappedValue = quote + remappedValue + quote;
							}
							replacements.put(groupId, remappedValue);
						}
					}
				}
			}
			String replacement = mapper.getValueReplacement();
			if (replacement == null || replacement.isBlank()) {
				replacement = "$0";
			}			
			if (mapper.getModelClass() != null) {
				for (Map.Entry<Integer, String> entry : replacements.entrySet()) {
					replacement = replacement.replaceAll("(?<!\\\\)\\$" + entry.getKey(), entry.getValue());
				}
			}
			m.appendReplacement(sb, replacement);
			m.appendTail(sb);
		}
		if (sb.length() != 0) {
			newValue = sb.toString();
			Log.debug(LOGGER, modelClassName + " value modified: " + newValue);
		} else {
			Log.debug(LOGGER, modelClassName + " value unchanged: " + newValue);
		}
		if (processor != null) {
			newValue = processor.pack(newValue);
			Log.debug(LOGGER, "Packed " + modelClassName + " value: " + newValue);
		}
		return newValue;
	}
	
	private static class ProcessJsonResult {
		public String value;
		public int updated;
		public int notFound;
		public ProcessJsonResult(String value, int updated, int notFound) {
			this.value = value;
			this.updated = updated;
			this.notFound = notFound;
		}
	}
	
	private static ProcessJsonResult processJsonPath(Path matchDir, MapperEntry mapper, String jsonString) throws Exception {
		// Create both value and path DocumentContext
		DocumentContext pathCtx = JsonPath.using(JSONPATH_CONFIG_READ_PATH).parse(jsonString);
		DocumentContext valueCtx = JsonPath.parse(jsonString);
		MappingResult count = processJsonPath(valueCtx, pathCtx, matchDir, mapper, 0, "$");
		return new ProcessJsonResult(valueCtx.jsonString(), count.getChangedCount(), count.getMappingNotFoundCount());
	}
	
	private static MappingResult processJsonPath(
			DocumentContext valueCtx, DocumentContext pathCtx, 
			Path matchDir, 
			MapperEntry mapper, int pathIndex, String path) throws Exception {
		MappingResult result = new MappingResult();
		String currentPath = mapper.getJsonPaths().get(pathIndex);
		Log.trace(LOGGER, "JSON path: " + path + "." + currentPath);
		boolean hasMorePaths = (pathIndex + 1) < mapper.getJsonPaths().size();
		if (currentPath.endsWith("[*]")) {
			// Resolve path
			try {
				List<String> resolvedPaths = pathCtx.read(path + "." + currentPath);
				for (String resolvedPath : resolvedPaths) {
					if (hasMorePaths) {
						// Drill deeper
						result.add(processJsonPath(
								valueCtx, pathCtx, matchDir, mapper, pathIndex + 1, resolvedPath));
					} else {
						// Resolve value
						String value = valueCtx.read(resolvedPath);
						Log.trace(LOGGER, "JSON path: " + resolvedPath + " value = " + value);
						try {
							String newValue = processValue(matchDir, mapper, resolvedPath, value);
							Log.trace(LOGGER, "JSON path: " + resolvedPath + " new value = " + newValue);
							if (!value.equals(newValue)) {
								valueCtx.set(resolvedPath, newValue);
								result.addChanged();
							}
						} catch (MappingNotFoundException mnfex) {
							Log.trace(LOGGER, 
									"JSON path: " + resolvedPath + " value = " + value + " Mapping not found");
							result.addMappingNotFound();
						}
					}
				}
			} catch (PathNotFoundException pnfex) {
				// Ignore
			}
		} else {
			try {
				List<String> resolvedPaths = pathCtx.read(path + "." + currentPath);
				for (String resolvedPath : resolvedPaths) {
					if (hasMorePaths) {
						result.add(processJsonPath(
								valueCtx, pathCtx, matchDir, mapper, pathIndex + 1, resolvedPath));
					} else {
						// Resolve value
						String value = valueCtx.read(resolvedPath);
						Log.debug(LOGGER, "JSON path: " + resolvedPath + " value = " + value);
						try {
							String newValue = processValue(matchDir, mapper, resolvedPath, value);
							Log.debug(LOGGER, "JSON path: " + resolvedPath + " new value = " + newValue);
							if (!value.equals(newValue)) {
								valueCtx.set(resolvedPath, newValue);
								result.addChanged();
							}
						} catch (MappingNotFoundException mnfex) {
							Log.trace(LOGGER, 
									"JSON path: " + resolvedPath + " value = " + value + " Mapping not found");
							result.addMappingNotFound();
						}
					}
				}
			} catch (PathNotFoundException pnfex) {
				// Ignore
			}
		}
		return result;
	}
	
	private static void remapWorkflow(Config config, Path outputDir, CommandLine cmd) throws Exception {
		Path sourceDir = Paths.get(cmd.getOptionValue(sourceDirOption));
		Path matchDir = Paths.get(cmd.getOptionValue(matchDirOption));
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*.json");
		MapperConfig mapperConfig = MapperConfig.getInstance();
		int total = 0;
		int success = 0;
		List<String> errorWorkflowNames = new ArrayList<>();
		Iterator<Path> it = Files.list(sourceDir).iterator();
		while (it.hasNext()) {
			Path path = it.next();
			if (pathMatcher.matches(path.getFileName())) {
				String workflowName = null;
				total++;
				try {
					String source = Files.readString(path);
					String modifiedSource = source;
					int modifiedCount = 0;
					int notFoundCount = 0;
					JsonNode workflow = OM.readTree(source);
					workflowName = workflow.get(0).get("workflows").get(0).get("name").asText();
					Log.debug(LOGGER, "Processing workflow: " + path.toString());
					// For each mapper
					for (MapperEntry mapper : mapperConfig.getMappers()) {
						// Check workflow name
						List<String> targetWorkflowNames = mapper.getTargetWorkflowNames();
						if (targetWorkflowNames.size() != 0 && 
							!targetWorkflowNames.contains(workflowName)) {
							// Skip this mapper
							Log.debug(LOGGER, 
									"Mapper: " + mapper.getName() + 
									" does not apply to workflow " + workflowName);
							continue;
						}
						Log.debug(LOGGER, "Mapper: " + mapper.getName() + " processing file: " + path);
						ProcessJsonResult result = processJsonPath(matchDir, mapper, modifiedSource);
						modifiedSource = result.value;
						modifiedCount += result.updated;
						notFoundCount += result.notFound;
					}	// For all mappers
					// Write to file
					Path outputFile = outputDir.resolve(path.getFileName());
					try (FileWriter fw = new FileWriter(outputFile.toFile())) {
						JsonNode node = OM.readTree(modifiedSource);
						String output = OM.writeValueAsString(node);
						fw.write(output);
						success++;
						if (modifiedCount > 0) {
							Log.info(LOGGER, 
									"Updated: " + outputFile.toString() + NEWLINE + 
									"\t" + modifiedCount + " change(s)" + NEWLINE + 
									"\t" + notFoundCount + " mapping(s) not found");
						} else {
							Log.info(LOGGER, "No change: " + outputFile.toString());
						}
						Map<String, String> codes = extractScriptRunScript(output);
						for (Map.Entry<String, String> entry : codes.entrySet()) {
							Path codeFile = outputDir.resolve(
									getWorkflowFileName(workflowName) + 
									" - " + entry.getKey() + ".groovy");
							Files.writeString(codeFile, entry.getValue());
							Log.info(LOGGER, "\tSaved ScriptRunner post-function: " + codeFile.toString());
						}
					}
				} catch (Exception ex) {
					if (workflowName != null) {
						errorWorkflowNames.add(workflowName);
					}
					Log.error(LOGGER, "Error: " + path.toFile(), ex);
				}
			}	// pathMatcher.matches()
		}
		Log.info(LOGGER, "Workflows remapped: " + success + "/" + total);
		if (errorWorkflowNames.size() != 0) {
			StringBuilder sb = new StringBuilder();
			for (String name : errorWorkflowNames) {
				sb.append(NEWLINE).append(name);
			}
			Log.info(LOGGER, "Error in " + errorWorkflowNames.size() + " workflows: " + sb.toString());
		}
	}
	
	private static void scriptRunnerTool(boolean pack, CommandLine cmd) throws Exception {
		String value = cmd.getOptionValue(scriptOption);
		String newValue = value;
		ScriptRunnerCompressedData scd = new ScriptRunnerCompressedData();
		if (pack) {
			newValue = scd.pack(value);
		} else {
			newValue = scd.unpack(value);
		}
		Log.info(LOGGER, "From: " + value);
		Log.info(LOGGER, "To: " + newValue);
	}
	
	private static void updateWorkflow(Config config, String host, CommandLine cmd) 
			throws Exception {
		Path workflowDir = Paths.get(cmd.getOptionValue(sourceDirOption));
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:{**/*.json,*.json}");
		int total = 0;
		int success = 0;
		Iterator<Path> it = Files.list(workflowDir).iterator();
		while (it.hasNext()) {
			Path path = it.next();
			if (pathMatcher.matches(path)) {
				total++;
				try {
					JsonNode node = OM.readTree(Files.readString(path));
					String workflowName = node.get(0).get("workflows").get(0).get("name").asText();
					String workflowId = node.get(0).get("workflows").get(0).get("id").asText();
					// Validate
					Map<String, Object> payload = new HashMap<>();
					payload.put("payload", node.get(0));
					// Validate
					RestUtil.getInstance(Object.class)
						.config(config)
						.host(host)
						.path("/rest/api/3/workflows/update/validation")
						.method(HttpMethod.POST)
						.payload(payload)
						.request();
					// Update
					RestUtil.getInstance(Object.class)
						.config(config)
						.host(host)
						.path("/rest/api/3/workflows/update")
						.method(HttpMethod.POST)
						.payload(node.get(0))
						.request();
					Log.info(LOGGER, 
							"Workflow updated: " + workflowName + " from file " + path.toString());
					success++;
				} catch (Exception ex) {
					Log.error(LOGGER, "Error updating workflow from file: " + path.toString(), ex);
				}
			} // If path matches pattern
		}
		Log.info(LOGGER, "Workflows updated: " + success + "/" + total);
	}

	private static void getCredential(Config config, CommandLine cmd) throws Exception {
		String email = cmd.getOptionValue(emailOption);
		config.setEmail(email);
		String token = cmd.getOptionValue(tokenOption);
		if (token == null || token.isEmpty()) {
			char[] pwd = Console.readPassword("API Token: ");
			config.setToken(new String(pwd));
		} else {
			config.setToken(token);
		}
	}

	private static Path createOutputDirectory() throws Exception {
		Path outputDir = Paths.get(SDF.format(new Date()));
		Files.createDirectories(outputDir);
		return outputDir;
	}
	
	public static void main(String[] args) throws Exception {
		Config config = Config.getInstance();
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		Path outputDir = null;
		try {
			cmd = parser.parse(exportOptions, args);
			String targetHost = cmd.getOptionValue(targetHostOption);
			outputDir = createOutputDirectory();
			getCredential(config, cmd);
			export(config, targetHost, outputDir, cmd);
			exportWorkflow(config, targetHost, outputDir, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(matchOptions, args);
			outputDir = createOutputDirectory();
			match(config, outputDir, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(remapWorkflowOptions, args);
			outputDir = createOutputDirectory();
			remapWorkflow(config, outputDir, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(packOptions, args);
			scriptRunnerTool(true, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(unpackOptions, args);
			scriptRunnerTool(false, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(updateWorkflowOptions, args);
			String targetHost = cmd.getOptionValue(targetHostOption);
			getCredential(config, cmd);
			updateWorkflow(config, targetHost, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		if (cmd == null) {
			HelpFormatter hf = new HelpFormatter();
			hf.printHelp("Export objects from site", exportOptions);
			hf.printHelp("Match export objects from two sites", matchOptions);
			hf.printHelp("Remap workflows to files", remapWorkflowOptions);
			hf.printHelp("Update workflows in site", updateWorkflowOptions);
			hf.printHelp("ScriptRunner pack utility", packOptions);
			hf.printHelp("ScriptRunner unpack utility", unpackOptions);
		}
	}
}
