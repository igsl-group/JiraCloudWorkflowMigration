package com.igsl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.igsl.model.Model;
import com.igsl.model.Project;
import com.igsl.model.User;
import com.igsl.model.Workflow;
import com.igsl.rest.Paged;
import com.igsl.rest.RestUtil;
import com.igsl.workflow.BulkTransition;
import com.igsl.workflow.BulkWorkflows;
import com.igsl.workflow.MapperConfig;
import com.igsl.workflow.MapperEntry;
import com.igsl.workflow.Version;
import com.igsl.workflow.WorkflowView;
import com.igsl.workflow.preprocessor.ScriptRunnerCompressedData;
import com.igsl.workflow.preprocessor.ValueProcessor;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

public class JiraCloudWorkflowMigration {
	
	private static final Logger LOGGER = LogManager.getLogger();
	private static final String NEWLINE = System.getProperty("line.separator");
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd_HHmmss");
	private static final ObjectMapper OM = new ObjectMapper()
				.setNodeFactory(new SortingNodeFactory())
				.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
				.enable(SerializationFeature.INDENT_OUTPUT);
	private static final Configuration JSONPATH_CONFIG_READ_PATH = Configuration.builder()
			   .options(com.jayway.jsonpath.Option.AS_PATH_LIST)
			   .build();
	
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	private static final String MATCH_RESULT = "MATCH_RESULT";
	private static final String MATCH_RESULT_MATCHED = "Matched";
	private static final String MATCH_RESULT_NO_MATCH = "No Match";
	private static final String MATCH_RESULT_COLLISION = "Collision";
	private static final String MATCH_WITH = "MATCH_WITH";
	private static final String DELIMITER = "|";
	
	/*
	 * Batch mode operates on multiple sources at once.
	 * Each source contains its own workflow list, object dump and workflow dump.
	 * Batch mode perform a single target dump, then perform matching with each source.
	 * Then user manually verify matches.
	 * Then batch mode remaps and updates workflow for each source. 
	 */
	private static Option batchSourceListOption;
	private static Options batchMatchOptions;
	private static Option batchMatchOption;
	private static Options batchRemapOptions;
	private static Option batchRemapOption;
	private static Options batchUpdateOptions;
	private static Option batchUpdateOption;
	
	private static Option emailOption;
	private static Option tokenOption;
	
	private static Option targetHostOption;
	private static Option sourceDirOption;
	private static Option workflowListOption;
	private static Option targetDirOption;
	private static Option matchDirOption;
	
	private static Options exportOptions;
	private static Option exportOption;
	
	private static Options matchOptions;
	private static Option matchOption;
	private static Option exactMatchOption;

	private static Option targetUserOption;
	
	private static Option grantProjectsOption;
	private static Options grantProjectsOptions;
	
	private static Option revokeProjectsOption;
	private static Options revokeProjectsOptions;
	
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
		
		workflowListOption = Option.builder()
				.argName("Workflow list")
				.desc("File containing a list of workflow names to be included")
				.option("wl")
				.longOpt("workflowlist")
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

		// Grant or revoke project access
		targetUserOption = Option.builder()
				.desc("Grant admin access to this account id. If not specified, defaults to current user.")
				.option("tu")
				.longOpt("targetuser")
				.hasArg()
				.build();
		grantProjectsOption = Option.builder()
				.desc("Grant admin access to all projects")
				.option("gp")
				.longOpt("grantprojects")
				.required()
				.build();
		grantProjectsOptions = new Options()
				.addOption(grantProjectsOption)
				.addOption(targetHostOption)
				.addOption(emailOption)
				.addOption(tokenOption)
				.addOption(targetUserOption);
		revokeProjectsOption = Option.builder()
				.desc("Revoke admin access to all projects")
				.option("rp")
				.longOpt("revokeprojects")
				.required()
				.build();
		revokeProjectsOptions = new Options()
				.addOption(revokeProjectsOption)
				.addOption(targetHostOption)
				.addOption(emailOption)
				.addOption(tokenOption)
				.addOption(targetUserOption);
		
		// Update workflow on site using directory
		updateWorkflowOption = Option.builder()
				.desc("Update workflow in target site")
				.option("uw")
				.longOpt("updateworkflow")
				.required()
				.build();
		updateWorkflowOptions = new Options()
				.addOption(updateWorkflowOption)
				.addOption(sourceDirOption)
				.addOption(workflowListOption)
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
		exactMatchOption = Option.builder()
				.argName("Object names must be exact match")
				.option("em")
				.longOpt("exactMatch")
				.build();
		matchOptions = new Options()
				.addOption(matchOption)
				.addOption(exactMatchOption)
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
				.addOption(sourceDirOption)
				.addOption(targetDirOption)
				.addOption(workflowListOption);

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

		// Batch options
		batchSourceListOption = Option.builder()
				.argName("Batch source directory list")
				.option("bd")
				.longOpt("batchDirectory")
				.required()
				.hasArgs()
				.build();
		
		batchMatchOption = Option.builder()
				.argName("Batch match")
				.option("bm")
				.longOpt("batchMatch")
				.required()
				.build();
		batchMatchOptions = new Options()
				.addOption(batchSourceListOption)
				.addOption(targetDirOption)
				.addOption(batchMatchOption)
				.addOption(exactMatchOption);
				
		batchRemapOption = Option.builder()
				.argName("Batch remap")
				.option("br")
				.longOpt("batchRemap")
				.required()
				.build();
		batchRemapOptions = new Options()
				.addOption(batchSourceListOption)
				.addOption(targetDirOption)
				.addOption(batchRemapOption);

		batchUpdateOption = Option.builder()
				.argName("Batch update")
				.option("bu")
				.longOpt("batchUpdate")
				.required()
				.build();
		batchUpdateOptions = new Options()
				.addOption(batchSourceListOption)
				.addOption(batchUpdateOption)
				.addOption(targetHostOption)
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
	
	protected static final Comparator<String> STRING_COMPARATOR = Comparator.nullsFirst(String::compareTo);
	protected static final Pattern PATTERN = Pattern.compile("(.+?)( \\(migrated( [0-9]+)?\\))?");	
	// Compare names with option to allow (migrated #)
	protected static int compareName(String name1, String name2, boolean exactMatch) {
		if (name1 != null && name2 != null) {
			if (!exactMatch) {
				Matcher matcher1 = PATTERN.matcher(name1);
				if (matcher1.matches()) {
					name1 = matcher1.group(1);
				}
				Matcher matcher2 = PATTERN.matcher(name2);
				if (matcher2.matches()) {
					name2 = matcher2.group(1);
				}
			}
			return STRING_COMPARATOR.compare(name1, name2);
		}
		return -1;
	}
	
	private static void match(
			Config config, Path source, Path target, Path outputDir, CommandLine cmd, boolean exactMatch) 
			throws Exception {
		String packageName = config.getObjectTypePackage();
		String outputTableFormat = "%-70s %-6s %-8s %-9s %-10s";
		Log.info(LOGGER, 
				Log.formatArguments(
						outputTableFormat, 
						"File", "Total", "Matched", "No Match", "Collision"));
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
			int matchCount = 0;
			int collisionCount = 0;
			int noMatchCount = 0;
			int totalCount = 0;
			try (	FileWriter fw = new FileWriter(outputFile.toFile()); 
					CSVPrinter printer = new CSVPrinter(fw, CSV.getCSVWriteFormat(headers))) {
				for (Model<?> sandboxItem : sandboxMap.values()) {
					totalCount++;
					String uniqueName = sandboxItem.getUniqueName();
					List<String> v = new ArrayList<>();
					v.add(sandboxItem.getIdentifier());
					v.add(sandboxItem.getUniqueName());
					v.addAll(sandboxItem.getValues().values());
					if (!exactMatch) {
						// Fuzzy match logic
						List<Model<?>> modelList = new ArrayList<>();
						for (List<Model<?>> items : productionMap.values()) {
							for (Model<?> item : items) {
								if (0 == compareName(
										item.getUniqueName(), 
										sandboxItem.getUniqueName(),
										false)) {
									modelList.add(item);
								}
							}
						}
						if (modelList.size() == 0) {
							noMatchCount++;
							// Source item is not matched
							v.add(MATCH_RESULT_NO_MATCH);
							v.add("");
						} else if (modelList.size() == 1) {
							matchCount++;
							// Matched
							v.add(MATCH_RESULT_MATCHED);
							v.add(modelList.get(0).getIdentifier());
						} else {
							collisionCount++;
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
						// Exact match
						if (productionMap.containsKey(uniqueName)) {
							List<Model<?>> modelList = productionMap.get(uniqueName);
							if (modelList.size() == 1) {
								// Matched
								matchCount++;
								v.add(MATCH_RESULT_MATCHED);
								v.add(modelList.get(0).getIdentifier());
							} else {
								collisionCount++;
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
							noMatchCount++;
							// Source item is not matched
							v.add(MATCH_RESULT_NO_MATCH);
							v.add("");
						}
					}
					CSV.printRecord(printer, v);
				}
				Log.info(LOGGER,  
						Log.formatArguments(
								outputTableFormat, 
								outputFile.toString(),
								totalCount,
								matchCount, 
								noMatchCount, 
								collisionCount));
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
	
	private static String formatWorkflow(String json, Class<?> view) throws Exception {
		ObjectReader reader = OM.readerFor(BulkWorkflows.class);
		ObjectWriter writer = OM.writerWithView(view);
		List<BulkWorkflows> list = new ArrayList<>();
		MappingIterator<BulkWorkflows> it = reader.readValues(json);
		while (it.hasNext()) {
			BulkWorkflows wf = it.next();
			wf.sort();
			list.add(wf);
		}
		return writer.writeValueAsString(list);
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
					// Sort JSON
					json = formatWorkflow(json, WorkflowView.Hybrid.class);
					// Write sorted output
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

	private static void clearMapCache() {
		mapCache.clear();
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
		}
		m.appendTail(sb);
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
						if (value != null) {
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
						}	// Else no change
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
						if (value != null) {
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
						} // Else no change
					} 
				}
			} catch (PathNotFoundException pnfex) {
				// Ignore
			}
		}
		return result;
	}
	
	private static String modifyIdAndVersion(String sourceXML, String targetXML) throws Exception {
		DocumentContext sourceValueCtx = JsonPath.parse(sourceXML);
		DocumentContext targetValueCtx = JsonPath.parse(targetXML);
		String targetId = targetValueCtx.read("$.[0].workflows.[0].id", String.class);
		String targetVersionId = targetValueCtx.read("$.[0].workflows.[0].version.id", String.class);
		int targetVersionNumber = targetValueCtx.read("$.[0].workflows.[0].version.versionNumber", Integer.class);
		sourceValueCtx = sourceValueCtx.put("$.[0].workflows.[0]", "id", targetId);
		sourceValueCtx = sourceValueCtx.put("$.[0].workflows.[0].version", "id", targetVersionId);
		sourceValueCtx = sourceValueCtx.put("$.[0].workflows.[0].version", "versionNumber", targetVersionNumber);
		return sourceValueCtx.jsonString();
	}
	
	private static void remapWorkflow(
			Config config, 
			Path sourceDir,
			Path workflowListFile,
			Path targetDir,
			Path matchDir,
			Path outputDir, 
			CommandLine cmd) throws Exception {
		List<String> workflowAllowList = new ArrayList<>();
		if (workflowListFile != null) {
			StringBuilder sb = new StringBuilder();
			try (BufferedReader r = new BufferedReader(new FileReader(workflowListFile.toFile()))) {
				String name = null;
				while ((name = r.readLine()) != null) {
					workflowAllowList.add(name);
					sb.append(name).append(NEWLINE);
				}
			} 
			Log.info(LOGGER, "Workflow(s) to be processed: \n" + sb.toString());
		}
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*.json");
		MapperConfig mapperConfig = MapperConfig.getInstance();
		int total = 0;
		int success = 0;
		List<String> errorWorkflowNames = new ArrayList<>();
		Iterator<Path> it = Files.list(sourceDir).iterator();
		ObjectReader reader = OM.readerFor(BulkWorkflows.class);
		//ObjectWriter hybridWriter = OM.writerWithView(WorkflowView.Hybrid.class);
		ObjectWriter currentWriter = OM.writerWithView(WorkflowView.Current.class);
		while (it.hasNext()) {
			Path path = it.next();
			if (pathMatcher.matches(path.getFileName())) {
				String workflowName = null;
				try {
					String source = Files.readString(path);
					String modifiedSource = source;
					int modifiedCount = 0;
					int notFoundCount = 0;
					MappingIterator<BulkWorkflows> workflowIterator = reader.readValues(source);
					// Each file will only contain a single workflow
					BulkWorkflows workflow = workflowIterator.next();
					// Sort
					workflow.sort();
					// Convert status references
					workflow.getWorkflows().forEach(bwf -> {
						bwf.getTransitions().forEach(t -> {
							t.convertStatusReference();
						});
					});
					List<BulkWorkflows> workflowList = new ArrayList<>();
					workflowList.add(workflow);
					String originalSource = currentWriter.writeValueAsString(workflowList);
					// JsonNode workflow = OM.readTree(source);
					// workflowName = workflow.get(0).get("workflows").get(0).get("name").asText();
					workflowName = workflow.getWorkflows().get(0).getName();
					if (!workflowAllowList.isEmpty() && !workflowAllowList.contains(workflowName)) {
						//Log.info(LOGGER, "Workflow ignored: " + path.toString() + " Name: " + workflowName);
						continue;
					}
					total++;
					Log.info(LOGGER, "Processing workflow: " + path.toString() + " Name: " + workflowName);
					// Convert status reference
					for (BulkTransition transition : workflow.getWorkflows().get(0).getTransitions()) {
						if (transition.isObsoleteFormat()) {
							transition.convertStatusReference();
						}
					}
					modifiedSource = currentWriter.writeValueAsString(workflowList);
					// Find same workflow in targetDir
					Path targetWorkflowPath = targetDir.resolve(path.getFileName());
					if (targetWorkflowPath.toFile().exists() && targetWorkflowPath.toFile().isFile()) {
						// Patch version and Id to same as target
						String targetSource = Files.readString(targetWorkflowPath);
						modifiedSource = modifyIdAndVersion(modifiedSource, targetSource);
					}
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
					try (FileWriter fw = new FileWriter(outputFile.toFile(), UTF8)) {
						String output = formatWorkflow(modifiedSource, WorkflowView.Current.class);
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
			StringBuilder sbError = new StringBuilder();
			for (String name : errorWorkflowNames) {
				sbError.append(NEWLINE).append(name);
			}
			Log.info(LOGGER, "Error in " + errorWorkflowNames.size() + " workflows: " + sbError.toString());
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
	
	private static void updateWorkflow(
			Config config, 
			Path workflowDir,
			Path workflowListFile,
			String host, 
			CommandLine cmd) 
			throws Exception {
		ObjectReader reader = OM.readerFor(BulkWorkflows.class);
		ObjectWriter currentWriter = OM.writerWithView(WorkflowView.Current.class);
		List<String> workflowList = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		try (BufferedReader r = new BufferedReader(new FileReader(workflowListFile.toFile()))) {
			String name = null;
			while ((name = r.readLine()) != null) {
				workflowList.add(name);
				sb.append(name).append(NEWLINE);
			}
		} 
		Log.info(LOGGER, "Workflow(s) to be processed: \n" + sb.toString());
		Map<String, String> errorWorkflows = new HashMap<>();
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:{**/*.json,*.json}");
		int total = 0;
		int ignored = 0;
		int success = 0;
		Iterator<Path> it = Files.list(workflowDir).iterator();
		while (it.hasNext()) {
			Path path = it.next();
			if (pathMatcher.matches(path)) {
				total++;
				try {
					MappingIterator<BulkWorkflows> wfIt = reader.readValues(Files.readString(path));
					BulkWorkflows wf = null;
					if (wfIt.hasNext()) {
						wf = wfIt.next();
					} else {
						// No data in this file, ignore and continue
						Log.warn(LOGGER, "File [" + path + "] does not contain a valid workflow");
						errorWorkflows.put(path.toString(), "Not valid workflow");
						continue;
					}
					String workflowName = wf.getWorkflows().get(0).getName();
					if (!workflowList.contains(workflowName)) {
						Log.info(LOGGER, 
								"Workflow ignored: [" + workflowName + "]");
						ignored++;
						total--;
						continue;
					} else {
						Log.info(LOGGER, 
								"Processing workflow [" + workflowName + "]...");
					}
					// Update workflow id and version
					Map<String, Object> param = new HashMap<>();
					List<String> nameList = new ArrayList<>();
					nameList.add(workflowName);
					param.put("workflowNames", nameList);
					List<BulkWorkflows> list = 
						RestUtil.getInstance(BulkWorkflows.class)
						.config(config)
						.host(host)
						.path("/rest/api/3/workflows")
						.payload(param)
						.method(HttpMethod.POST)
						.requestAllPages();
					if (list.size() == 1) {
						String id = list.get(0).getWorkflows().get(0).getId();
						Version version = list.get(0).getWorkflows().get(0).getVersion();
						wf.getWorkflows().get(0).setId(id);
						Log.error(LOGGER, "Workflow id for [" + workflowName + "] updated to [" + id + "]");
						wf.getWorkflows().get(0).getVersion().setId(version.getId());
						Log.error(LOGGER, "Workflow version id for [" + workflowName + "] updated to [" + version.getId() + "]");
						wf.getWorkflows().get(0).getVersion().setVersionNumber(version.getVersionNumber());
						Log.error(LOGGER, "Workflow version number for [" + workflowName + "] updated to [" + version.getVersionNumber() + "]");
					} else {
						Log.error(LOGGER, "Unable to update workflow id and version for [" + workflowName + "]");
					}
					// Reformat JSON
					String json = currentWriter.writeValueAsString(wf);
					JsonNode node = OM.readTree(json);
					// Validate
					Map<String, Object> payload = new HashMap<>();
					payload.put("payload", node);
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
						.payload(node)
						.request();
					Log.info(LOGGER, 
							"Workflow updated: " + workflowName + " from file " + path.toString());
					success++;
				} catch (Exception ex) {
					Log.error(LOGGER, "Error updating workflow from file: " + path.toString(), ex);
					errorWorkflows.put(path.toString(), ex.getMessage());
				}
			} // If path matches pattern
		}
		Log.info(LOGGER, "Workflows updated: " + success + "/" + total);
		Log.info(LOGGER, "Workflows ignored: " + ignored);
		if (errorWorkflows.size() != 0) {
			Log.error(LOGGER, "Errors: ");
			errorWorkflows.entrySet().forEach(entry -> {
				Log.error(LOGGER, entry.getKey() + ": " + entry.getValue());
			});
		}
	}

	// Get list of projects
	private static List<Project> getProjects(Config config, String host) throws Exception {
		List<Project> projectList = Model.exportObject(config, Project.class, host);
		return projectList;
	}
	
	// Get administrator role id for project
	private static String getAdminProjectRole(Config config, String host, Project project) throws Exception {
		final String ADMINISTRATOR = "Administrators";
		final Pattern PATTERN = Pattern.compile(".+/role/([0-9]+)");
		RestUtil<Object> restUtil = RestUtil.getInstance(Object.class); 
		Response resp = restUtil
			.config(config)
			.host(host)
			.method(HttpMethod.GET)
			.path("/rest/api/3/project/" + project.getId() + "/role")
			.request();
		if ((resp.getStatus() & HttpStatus.SC_OK) == HttpStatus.SC_OK) {
			Map<String, String> map = resp.readEntity(new GenericType<Map<String, String>>(){});
			if (map.containsKey(ADMINISTRATOR)) {
				String url = map.get(ADMINISTRATOR);
				Matcher m = PATTERN.matcher(url);
				if (m.matches()) {
					return m.group(1);
				}
			}
		}
		return null;
	}
	
	private static String getAccountId(Config config, String host, String email, CommandLine cmd) 
			throws Exception {
		RestUtil<User> restUtil = RestUtil.getInstance(User.class); 
		Paged<User> pagination = new Paged<User>(User.class);
		pagination.valuesProperty("users");
		List<User> users = restUtil
				.config(config)
				.host(host)
				.method(HttpMethod.GET)
				.path("/rest/api/3/user/picker")
				.query("query", email)
				.pagination(pagination)
				.requestAllPages();
		if (users.size() == 1) {
			return users.get(0).getAccountId();
		}
		return null;
	}
	
	private static void grantProjects(Config config, String host, String accountId, CommandLine cmd) 
			throws Exception {
		if (accountId == null) {
			accountId = getAccountId(config, host, config.getEmail(), cmd);
		}
		List<String> userList = new ArrayList<>();
		userList.add(accountId);
		Map<String, Object> payload = new HashMap<>();
		payload.put("user", userList);
		RestUtil<Object> rest = RestUtil.getInstance(Object.class);
		List<Project> projectList = getProjects(config, host);
		int successCount = 0;
		for (Project project : projectList) {
			String roleId = getAdminProjectRole(config, host, project);
			try {
				Response resp = rest
					.config(config)
					.host(host)
					.method(HttpMethod.POST)
					.path("/rest/api/3/project/" + project.getId() + "/role/" + roleId)
					.payload(payload)
					.status()
					.request();
				switch (resp.getStatus()) {
				case 200: 
					Log.info(LOGGER, 
							"Granted administrator role (" + roleId + ") to user " + accountId + 
							" in project " + project.getName() + 
							" (" + project.getId() + ")");
					successCount++;
					break;
				case 400:
					Log.warn(LOGGER, 
							"Administrator role (" + roleId + ") is already granted to user " + accountId + 
							" in project " + project.getName() + 
							" (" + project.getId() + ")");
					successCount++;
					break;
				default: 
					throw new Exception(resp.readEntity(String.class));
				}
			} catch (Exception ex) {
				Log.error(LOGGER, 
						"Unable to grant administrator role (" + roleId + ") to user " + accountId + 
						" in project " + project.getName() + 
						" (" + project.getId() + ")", ex);
			}
		}
		Log.info(LOGGER, "Granted successfully: " + successCount + "/" + projectList.size());
	}
	
	private static void revokeProjects(Config config, String host, String accountId, CommandLine cmd) 
			throws Exception {
		if (accountId == null) {
			accountId = getAccountId(config, host, config.getEmail(), cmd);
		}
		RestUtil<Object> rest = RestUtil.getInstance(Object.class);
		List<Project> projectList = getProjects(config, host);
		int successCount = 0;
		for (Project project : projectList) {
			String roleId = getAdminProjectRole(config, host, project);
			try {
				Response resp = rest
					.config(config)
					.host(host)
					.method(HttpMethod.DELETE)
					.path("/rest/api/3/project/" + project.getId() + "/role/" + roleId)
					.query("user", accountId)
					.status()
					.request();
				switch (resp.getStatus()) {
				case 204: 
					Log.info(LOGGER, 
							"Revoked administrator role (" + roleId + ") from user " + accountId + 
							" in project " + project.getName() + 
							" (" + project.getId() + ")");
					successCount++;
					break;
				case 404:
					Log.warn(LOGGER, 
							"Administrator role (" + roleId + ") is not granted to user " + accountId + 
							" in project " + project.getName() + 
							" (" + project.getId() + ")");
					successCount++;
					break;
				default: 
					throw new Exception(resp.readEntity(String.class));
				}
			} catch (Exception ex) {
				Log.error(LOGGER, 
						"Unable to revoke administrator role (" + roleId + ") from user " + accountId + 
						" in project " + project.getName() + 
						" (" + project.getId() + ")", ex);
			}
		}
		Log.info(LOGGER, "Revoked successfully: " + successCount + "/" + projectList.size());
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

	private static Path createOutputDirectory(String parent, String prefix, boolean timestamped) throws Exception {
		Path outputDir;
		if (parent != null) {
			outputDir = Paths.get(
					parent, 
					(prefix != null? prefix : "") + 
					(timestamped? SDF.format(new Date()) : ""));
		} else {
			outputDir = Paths.get(
					(prefix != null? prefix : "") + 
					(timestamped? SDF.format(new Date()) : ""));
		}
		Files.createDirectories(outputDir);
		return outputDir;
	}
	
	public static void main(String[] args) throws Exception {
		Config config = Config.getInstance();
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		Path outputDir = null;
		try {
			cmd = parser.parse(batchMatchOptions, args);
			boolean exactMatch = cmd.hasOption(exactMatchOption);
			String[] sourceList = cmd.getOptionValues(batchSourceListOption);
			for (String source : sourceList) {
				outputDir = createOutputDirectory(source, "MATCH", false);
				Path src = Paths.get(source);
				Path target = Paths.get(cmd.getOptionValue(targetDirOption));
				match(config, src, target, outputDir, cmd, exactMatch);
			}
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(batchRemapOptions, args);
			String[] sourceList = cmd.getOptionValues(batchSourceListOption);
			for (String source : sourceList) {
				clearMapCache();
				Path targetDir = Paths.get(cmd.getOptionValue(targetDirOption));
				Path matchDir = Paths.get(source, "MATCH");
				outputDir = createOutputDirectory(source, "REMAP", false);
				Path workflowListFile = Paths.get(source, "Workflow.txt");
				remapWorkflow(config, Paths.get(source), workflowListFile, targetDir, matchDir, outputDir, cmd);
			}
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(batchUpdateOptions, args);
			String[] sourceList = cmd.getOptionValues(batchSourceListOption);
			for (String source : sourceList) {
				String targetHost = cmd.getOptionValue(targetHostOption);
				getCredential(config, cmd);
				Path workflowDir = Paths.get(source, "REMAP");
				Path workflowListFile = Paths.get(source, "Workflow.txt");
				updateWorkflow(config, workflowDir, workflowListFile, targetHost, cmd);
			}
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(exportOptions, args);
			String targetHost = cmd.getOptionValue(targetHostOption);
			outputDir = createOutputDirectory(null, null, true);
			getCredential(config, cmd);
			export(config, targetHost, outputDir, cmd);
			exportWorkflow(config, targetHost, outputDir, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(matchOptions, args);
			boolean exactMatch = cmd.hasOption(exactMatchOption);
			outputDir = createOutputDirectory(null, null, true);
			Path source = Paths.get(cmd.getOptionValue(sourceDirOption));
			Path target = Paths.get(cmd.getOptionValue(targetDirOption));
			match(config, source, target, outputDir, cmd, exactMatch);
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(remapWorkflowOptions, args);
			getCredential(config, cmd);
			Path sourceDir = Paths.get(cmd.getOptionValue(sourceDirOption));
			Path matchDir = Paths.get(cmd.getOptionValue(matchDirOption));
			Path targetDir = Paths.get(cmd.getOptionValue(targetDirOption));
			Path workflowListFile = Paths.get(cmd.getOptionValue(workflowListOption));
			outputDir = createOutputDirectory(null, null, true);
			remapWorkflow(config, sourceDir, workflowListFile, targetDir, matchDir, outputDir, cmd);
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
			Path workflowDir = Paths.get(cmd.getOptionValue(sourceDirOption));
			Path workflowListFile = Paths.get(cmd.getOptionValue(workflowListOption));
			updateWorkflow(config, workflowDir, workflowListFile, targetHost, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(grantProjectsOptions, args);
			String targetHost = cmd.getOptionValue(targetHostOption);
			String targetUser = cmd.getOptionValue(targetUserOption);
			getCredential(config, cmd);
			grantProjects(config, targetHost, targetUser, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		try {
			cmd = parser.parse(revokeProjectsOptions, args);
			String targetHost = cmd.getOptionValue(targetHostOption);
			String targetUser = cmd.getOptionValue(targetUserOption);
			getCredential(config, cmd);
			revokeProjects(config, targetHost, targetUser, cmd);
		} catch (ParseException pex) {
			// Ignore
		}
		if (cmd == null) {
			HelpFormatter hf = new HelpFormatter();
			
			Path source = Paths.get(JiraCloudWorkflowMigration.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			String jarPath = Paths.get(".").toAbsolutePath().relativize(source).toString();
			String app = "java -jar " + jarPath;
			int width = 100;
			
			hf.printHelp(width, app, "Export objects from site", exportOptions, "\n", true);
			hf.printHelp(width, app, "Match export objects from two sites", matchOptions, "\n", true);
			hf.printHelp(width, app, "Remap workflows to files", remapWorkflowOptions, "\n", true);
			hf.printHelp(width, app, "Update workflows in site", updateWorkflowOptions, "\n", true);

			hf.printHelp(width, app, "Batch match", batchMatchOptions, "\n", true);
			hf.printHelp(width, app, "Batch remap", batchRemapOptions, "\n", true);
			hf.printHelp(width, app, "Batch update", batchUpdateOptions, "\n", true);
			
			hf.printHelp(width, app, "Grant admin rights in all projects", grantProjectsOptions, "\n", true);
			hf.printHelp(width, app, "Revoke admin rights in all projects", revokeProjectsOptions, "\n", true);
			
			hf.printHelp(width, app, "ScriptRunner pack utility", packOptions, "\n", true);
			hf.printHelp(width, app, "ScriptRunner unpack utility", unpackOptions, "\n", true);
		}
	}
}
