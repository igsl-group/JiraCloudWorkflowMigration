package com.igsl;

import java.io.FileReader;
import java.io.FileWriter;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd_HHmmss");
	private static final ObjectMapper OM = new ObjectMapper();
	private static final Configuration JSONPATH_CONFIG_READ_VALUE = Configuration.builder()
			   .build();
	private static final Configuration JSONPATH_CONFIG_READ_PATH = Configuration.builder()
			   .options(com.jayway.jsonpath.Option.AS_PATH_LIST)
			   .build();
	
	private static final String MATCH_RESULT = "MATCH_RESULT";
	private static final String MATCH_RESULT_MATCHED = "Matched";
	private static final String MATCH_RESULT_NO_MATCH = "No Match";
	private static final String MATCH_RESULT_COLLISION = "Collision";
	private static final String MATCH_WITH = "MATCH_WITH";
	private static final String DELIMITER = "|";
	
	private static Option sandboxDirectoryOption;
	private static Option productionDirectoryOption;
	private static Option matchDirectoryOption;
	
	private static Options exportSandboxOptions;
	private static Option exportSandboxOption;
	
	private static Options exportProductionOptions;
	private static Option exportProductionOption;
	
	private static Options matchOptions;
	private static Option matchOption;
	
	private static Options remapSandboxWorkflowOptions;
	private static Option remapSandboxWorkflowOption;
	
	private static Options updateWorkflowOptions;
	private static Option updateWorkflowOption;
	private static Option workflowDirectoryOption;
	
	private static Option unpackOption;
	private static Option packOption;
	private static Option scriptOption;
	private static Options unpackOptions;
	private static Options packOptions;
	
	static {
		updateWorkflowOption = Option.builder()
				.argName("Update workflow")
				.option("uw")
				.longOpt("updateworkflow")
				.required()
				.build();
		workflowDirectoryOption = Option.builder()
				.argName("Modified workflow directory")
				.option("wd")
				.longOpt("workflowdirectory")
				.hasArg()
				.required()
				.build();
		updateWorkflowOptions = new Options()
				.addOption(updateWorkflowOption)
				.addOption(workflowDirectoryOption);
		
		unpackOption = Option.builder()
				.argName("Unpack ScriptRunner script")
				.option("u")
				.longOpt("unpack")
				.build();
		packOption = Option.builder()
				.argName("Package ScriptRunner script")
				.option("p")
				.longOpt("pack")
				.build();
		scriptOption = Option.builder()
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
		
		matchDirectoryOption = Option.builder()
				.argName("Match directory")
				.option("md")
				.longOpt("matchdir")
				.hasArg()
				.required()
				.build();
		sandboxDirectoryOption = Option.builder()
				.argName("Sandbox objects directory")
				.option("sd")
				.longOpt("sandboxdir")
				.hasArg()
				.required()
				.build();
		productionDirectoryOption = Option.builder()
				.argName("Production objects directory")
				.option("pd")
				.longOpt("productiondir")
				.hasArg()
				.required()
				.build();
		
		remapSandboxWorkflowOption = Option.builder()
				.argName("Remap sandbox workflows")
				.option("rw")
				.longOpt("remapworkflow")
				.required()
				.build();
		remapSandboxWorkflowOptions = new Options()
				.addOption(remapSandboxWorkflowOption)
				.addOption(matchDirectoryOption)
				.addOption(sandboxDirectoryOption);
		
		matchOption = Option.builder()
				.argName("Match sandbox and production objects")
				.option("m")
				.longOpt("match")
				.required()
				.build();
		matchOptions = new Options()
				.addOption(matchOption)
				.addOption(productionDirectoryOption)
				.addOption(sandboxDirectoryOption);
		
		exportSandboxOption = Option.builder()
				.argName("Export sandbox objects")
				.option("es")
				.longOpt("exportsandbox")
				.required()
				.build();
		exportSandboxOptions = new Options()
				.addOption(exportSandboxOption);
		
		exportProductionOption = Option.builder()
				.argName("Export production objects")
				.option("ep")
				.longOpt("exportproduction")
				.required()
				.build();
		exportProductionOptions = new Options()
				.addOption(exportProductionOption);
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
		String sandboxDir = cmd.getOptionValue(sandboxDirectoryOption);
		Path sandbox = Paths.get(sandboxDir);
		String productionDir = cmd.getOptionValue(productionDirectoryOption);
		Path production = Paths.get(productionDir);
		String packageName = config.getObjectTypePackage();
		for (String className : config.getObjectTypes()) {
			Class<?> cls = Class.forName(packageName + "." + className);
			Model<?> model = (Model<?>) cls.getConstructor().newInstance();
			// Read sandbox into map of identifier to Model
			Map<String, Model<?>> sandboxMap = new HashMap<>();
			try (	FileReader fr = new FileReader(getObjectFileName(sandbox, className).toFile()); 
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
			try (	FileReader fr = new FileReader(getObjectFileName(production, className).toFile()); 
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
						// Sandbox item is not matched
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
					String name = workflowName.replaceAll("\\W+", " ");				
					Path outputFile = outputDir.resolve("Workflow (" + name + ").json");
					OM.writeValue(outputFile.toFile(), wf);
					Log.info(LOGGER, "Saved Workflow: " + workflowName + " to " + outputFile.toString());
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
	private static int MAP_SIZE = 10;	// Max. no. of maps to keep in mapCache, FIFO
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
	
	private static String remapValue(Path matchDir, MapperEntry mapper, String value) throws Exception {
		String modelClassName = mapper.getModelClassName();
		String newValue = value;
		Log.debug(LOGGER, "Remapping " + modelClassName + " value: " + value);
		Map<String, String> map = loadMap(matchDir, mapper.getModelClassName());
		if (map.containsKey(newValue)) {
			newValue = map.get(newValue);
			Log.debug(LOGGER, "Remapped " + modelClassName + " value to: " + newValue);
		} else {
			Log.error(LOGGER, "Not mapping found for " + modelClassName + " value: " + newValue);
		}
		return newValue;
	}
	
	private static String processValue(Path matchDir, MapperEntry mapper, String value) throws Exception {
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
			Map<Integer, String> replacements = new HashMap<>();
			if (mapper.getModelClass() != null) {
				for (int groupId : groupList) {
					String groupValue = m.group(groupId);
					String remappedValue = remapValue(matchDir, mapper, groupValue);
					replacements.put(groupId, remappedValue);
				}
			}
			String replacement = mapper.getValueReplacement();
			if (replacement == null || replacement.isBlank()) {
				replacement = "$0";
			}			
			if (mapper.getModelClass() != null) {
				for (Map.Entry<Integer, String> entry : replacements.entrySet()) {
					replacement = replacement.replaceAll("\\$" + entry.getKey(), entry.getValue());
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
	
	private static String processJsonPath(Path matchDir, MapperEntry mapper, String jsonString) throws Exception {
		// Create both value and path DocumentContext
		DocumentContext pathCtx = JsonPath.using(JSONPATH_CONFIG_READ_PATH).parse(jsonString);
		DocumentContext valueCtx = JsonPath.parse(jsonString);
		processJsonPath(valueCtx, pathCtx, matchDir, mapper, 0, "$");
		return valueCtx.jsonString();
	}
	
	private static void processJsonPath(
			DocumentContext valueCtx, DocumentContext pathCtx, 
			Path matchDir, 
			MapperEntry mapper, int pathIndex, String path) throws Exception {
		String currentPath = mapper.getJsonPaths().get(pathIndex);
		Log.debug(LOGGER, "JSON path: " + path + "." + currentPath);
		boolean hasMorePaths = (pathIndex + 1) < mapper.getJsonPaths().size();
		if (currentPath.endsWith("[*]")) {
			// Resolve path
			try {
				List<String> resolvedPaths = pathCtx.read(path + "." + currentPath);
				for (String resolvedPath : resolvedPaths) {
					if (hasMorePaths) {
						// Drill deeper
						processJsonPath(valueCtx, pathCtx, matchDir, mapper, pathIndex + 1, resolvedPath);
					} else {
						// Resolve value
						String value = valueCtx.read(resolvedPath);
						Log.debug(LOGGER, "JSON path: " + resolvedPath + " value = " + value);
						String newValue = processValue(matchDir, mapper, value);
						Log.debug(LOGGER, "JSON path: " + resolvedPath + " new value = " + newValue);
						valueCtx.set(resolvedPath, newValue);
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
						processJsonPath(valueCtx, pathCtx, matchDir, mapper, pathIndex + 1, resolvedPath);
					} else {
						// Resolve value
						String value = valueCtx.read(resolvedPath);
						Log.debug(LOGGER, "JSON path: " + resolvedPath + " value = " + value);
						String newValue = processValue(matchDir, mapper, value);
						Log.debug(LOGGER, "JSON path: " + resolvedPath + " new value = " + newValue);
						valueCtx.set(resolvedPath, newValue);
					}
				}
			} catch (PathNotFoundException pnfex) {
				// Ignore
			}
		}
	}
	
	private static void remapSandboxWorkflow(Config config, Path outputDir, CommandLine cmd) throws Exception {
		Path sandboxDir = Paths.get(cmd.getOptionValue(sandboxDirectoryOption));
		Path matchDir = Paths.get(cmd.getOptionValue(matchDirectoryOption));
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*.json");
		MapperConfig mapperConfig = MapperConfig.getInstance();
		Files.list(sandboxDir).forEach(path -> {
			if (pathMatcher.matches(path.getFileName())) {
				try {
					String source = Files.readString(path);
					String modifiedSource = source;
					JsonNode workflow = OM.readTree(source);
					String workflowName = workflow.get(0).get("workflows").get(0).get("name").asText();
					Log.info(LOGGER, "Processing workflow: " + path.toString());
					// For each mapper
					for (MapperEntry mapper : mapperConfig.getMappers()) {
						// Check workflow name
						List<String> targetWorkflowNames = mapper.getTargetWorkflowNames();
						if (targetWorkflowNames.size() != 0 && 
							!targetWorkflowNames.contains(workflowName)) {
							// Skip this mapper
							Log.info(LOGGER, 
									"Mapper: " + mapper.getName() + 
									" does not apply to workflow " + workflowName);
							continue;
						}
						Log.info(LOGGER, "Mapper: " + mapper.getName() + " processing file: " + path);
						modifiedSource = processJsonPath(matchDir, mapper, modifiedSource);
					}	// For all mappers
					// Write to file
					Path outputFile = outputDir.resolve(path.getFileName());
					try (FileWriter fw = new FileWriter(outputFile.toFile())) {
						fw.write(modifiedSource);
						Log.info(LOGGER, "Updated: " + outputFile.toString());
					}
				} catch (Exception ex) {
					Log.error(LOGGER, "Error processing: " + path.toFile(), ex);
				}
			}	// pathMatcher.matches()
		});
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
	
	private static void updateProductionWorkflow(Config config, Path outputDir, CommandLine cmd) 
			throws Exception {
		Path workflowDir = Paths.get(cmd.getOptionValue(workflowDirectoryOption));
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:{**/*.json,*.json}");
		Files.list(workflowDir).forEach(path -> {
			if (pathMatcher.matches(path)) {
				try {
					JsonNode node = OM.readTree(Files.readString(path));
					String workflowName = node.get(0).get("workflows").get(0).get("name").asText();
					// Validate
					Map<String, Object> payload = new HashMap<>();
					payload.put("payload", node.get(0));
					RestUtil.getInstance(Object.class)
						.config(config)
						.host(config.getProduction())
						.path("/rest/api/3/workflows/update/validation")
						.method(HttpMethod.POST)
						.payload(payload)
						.request();
					// Update
					RestUtil.getInstance(Object.class)
						.config(config)
						.host(config.getProduction())
						.path("/rest/api/3/workflows/update")
						.method(HttpMethod.POST)
						.payload(node.get(0))
						.request();
					Log.info(LOGGER, "Workflow updated: " + workflowName + " from file " + path.toString());
				} catch (Exception ex) {
					Log.error(LOGGER, "Error updating workflow from file: " + path.toString(), ex);
				}
			}
		});
	}
	
	public static void main(String[] args) throws Exception {
		Config config = Config.getInstance();
		// Get user
		if (config.getEmail() == null || config.getEmail().isBlank()) {
			String email = Console.readLine("User Email: ");
			config.setEmail(email);
		}
		// Get password
		if (config.getToken() == null || config.getToken().isBlank()) {
			Console.println("User Email: " + config.getEmail());
			char[] pwd = Console.readPassword("API Token: ");
			config.setToken(new String(pwd));
		}	
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		Path outputDir = Paths.get(SDF.format(new Date()));
		Files.createDirectories(outputDir);
		try {
			cmd = parser.parse(exportSandboxOptions, args);
			export(config, config.getSandbox(), outputDir, cmd);
			exportWorkflow(config, config.getSandbox(), outputDir, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(exportProductionOptions, args);
			export(config, config.getProduction(), outputDir, cmd);
			exportWorkflow(config, config.getProduction(), outputDir, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(matchOptions, args);
			match(config, outputDir, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(remapSandboxWorkflowOptions, args);
			remapSandboxWorkflow(config, outputDir, cmd);
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
			updateProductionWorkflow(config, outputDir, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		if (cmd == null) {
			HelpFormatter hf = new HelpFormatter();
			hf.printHelp("Export sandbox objects", exportSandboxOptions);
			hf.printHelp("Export production objects", exportProductionOptions);
			hf.printHelp("Match sandbox and production objects", matchOptions);
			hf.printHelp("Remap sandbox workflows", remapSandboxWorkflowOptions);
			hf.printHelp("Update production workflows", updateWorkflowOptions);
			hf.printHelp("ScriptRunner pack utility", packOptions);
			hf.printHelp("ScriptRunner unpack utility", unpackOptions);
		}
	}
}
