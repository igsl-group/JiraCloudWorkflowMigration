# Jira Cloud Workflow Migration Tool
This tool is used to clone workflows from sandbox to production. References to supported object types are automatically updated.

## Configuration

### Log4j2 Configuration
Edit /log4j2.xml as needed.

### General Configuration
Edit /Config.json:
<table border="1" style="border-collapse: collapse">
    <thead>
        <tr>
            <th>JSON Property</th>
            <th>Description</th>
            <th>Example</th> 
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>rate</td>
            <td>Max. no. of REST API calls within period</td>
            <td>100</td>
        </tr>
        <tr>
            <td>period</td>
            <td>No. of milliseconds in a period</td>
            <td>1000</td>
        </tr>
        <tr>
            <td>objectTypePackage</td>
            <td>Package name containing object type classes</td>
            <td>com.igsl.model</td>
        </tr>
        <tr>
            <td>objectTypes</td>
            <td>List of object type class names</td>
            <td>
                [
                    "Status",
                    "Project",
                    "Field",
                    "Screen",
                    "Priority",
                    "User",
                    "Group",
                    "Workflow",
                    "Resolution",
                    "ProjectCategory"
                ]
            </td>
        </tr>
    </tbody>
</table>

### mapper/*.json
Configure workflow remapping logic by adding JSON files in mapper folder. See mapperDisabled/Example.json for description and examples.

#### Mapper Example: Screen ID
```
[
    {
        "name": "Workflow Transition Screens",
        "jsonPaths": [
            "[*]",
            "workflows[*]",
            "transitions[*]", 
            "transitionScreen",
            "parameters",
            "screenId"
        ],
        "valueProcessorClass": null,
        "valueRegex": "[0-9]+",
        "valueCaptureGroups": [],
        "valueReplacement": null,
        "modelClass": "com.igsl.model.Screen",
        "targetWorkflowNames": []
    }
]
```
1. This defines a single mapper called "Workflow Transition Screens". 
1. Because targetWorkflowNames is empty, this mapper applies for all workflows. 
1. It looks for JSON path `$[*].workflows[*].transitions[*].transitionScreen.parameters.screenId`.
    1. Starting from document root.
    1. Find all `workflows` nodes.
    1. Find all `transitions` nodes within those workflow elements.
    1. Find `transitionScreen` node within those transitions elements. 
    1. Find `parameters` node within transitionScreen.
    1. Find `screenId` node within parameters. 
    1. The value of screenId is returned as the value.
1. No value preprocessor is used, so the value is not changed.
1. Regular expression `[0-9]+` is used to search for matches. 
1. Because valueCaptureGroup is empty, for each match, group 0 is the sandbox object ID.
1. Based on modelClass, it looks up sandbox object ID in Screen mapping and returns production object ID. 
1. Because valueReplacement is empty, production object ID replaces the whole match.
1. No value preprocessor is used, so the value is not changed.

#### Mapper Example 2: ScriptRunner Custom Field IDs in Run Script Post Function 
```
[
    {
        "name": "ScriptRunner PostFunction Custom Field",
        "jsonPaths": [
            "[*]",
            "workflows[*]",
            "transitions[*]",
            "actions[*]",
            "parameters[?(@.appKey==\"com.onresolve.jira.groovy.groovyrunner__script-postfunction\")]",
            "config"
        ],
        "valueProcessorClass": "com.igsl.workflow.preprocessor.ScriptRunnerCompressedData",
        "valueRegex": "customfield_[0-9]+",
        "valueCaptureGroups": [],
        "valueReplacement": null,
        "modelClass": "com.igsl.mode.Field",
        "targetWorkflowNames": []
    }
]
```
1. It looks for JSON path `$[*].workflows[*].transitions[*].actions[*].parameters[...].config`.
    1. Starting from document root.
    1. Find all `workflows` nodes.
    1. Find all `transitions` nodes within those workflow elements.
    1. Find `actions` node within those transitions elements. 
    1. Find `parameters` node within transitionScreen, where the `appKey` attribute is `com.onresolve.jira.groovy.groovyrunner__script-postfunction`.
    1. Find `config` node within parameters. 
    1. The value of config is returned as the value.
1. Preprocessor class `com.igsl.workflow.preprocessor.ScriptRunnerCompressedData` is used to unpack the value. 
1. Regular expression `customfield_[0-9]+` is used to search for matches. 
1. Because valueCaptureGroup is empty, for each match, group 0 is the sandbox object ID.
1. Based on modelClass, it looks up sandbox object ID in Field mapping and returns production object ID. 
1. Because valueReplacement is empty, production object ID replaces the whole match.
1. Preprocessor class `com.igsl.workflow.preprocessor.ScriptRunnerCompressedData` is used to pack the value.

#### Mapper Example 3: Replace application/* with mimetype/*
```
[
    {
        "name": "Replace MIME type",
        "jsonPaths": [
            "[*]",
            "workflows[*]",
            "transitions[*]",
            "actions[*]",
            "parameters[?(@.appKey==\"com.onresolve.jira.groovy.groovyrunner__script-postfunction\")]",
            "config"
        ],
        "valueProcessorClass": "com.igsl.workflow.preprocessor.ScriptRunnerCompressedData",
        "valueRegex": "application/([\\W]+)",
        "valueCaptureGroups": 1,
        "valueReplacement": "mimetype/$1",
        "modelClass": null,
        "targetWorkflowNames": []
    }
]
```
1. It looks for JSON path `$[*].workflows[*].transitions[*].actions[*].parameters[...].config`.
1. Preprocessor class `com.igsl.workflow.preprocessor.ScriptRunnerCompressedData` is used to unpack the value. 
1. Regular expression `application/([\\W]+)` is used to search for matches. 
1. Capture group 1 is returned as value.
1. Because modelClass is not specified, no mapping is performed.
1. valueReplacement is used to construct the new value.
1. Preprocessor class `com.igsl.workflow.preprocessor.ScriptRunnerCompressedData` is used to pack the value.

## Usage
1. Before you run this tool, production must contain the same objects (e.g. status, screens, fields) as sandbox. This tool only clones workflows, not their dependent objects. 
1. Prepare text file Workflow.txt. Put one workflow name on each line. This file is used to control which workflows will be processed. 
1. Export objects from sandbox: `java -jar JiraCloudWorkflowMigration-[version].jar -es`
    1. This creates a new folder with current timestamp. Rename it to `SANDBOX`.
    1. The folder contains: 
        1. `[Name].csv` - One file per object type. Contains Object ID, unique name and additional columns that can be used to identify the object. 
        1. `Workflow ([Name]).json` - One file per workflow. JSON is in the bulk format (https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-workflows/#api-rest-api-3-workflows-post). 
1. Export objects from production: `java -jar JiraCloudWorkflowMigration-[version].jar -ep`
    1. This creates a new folder with current timestmap. Rename it to `PRODUCTION`.
1. Compare exported objects: `java -jar JiraCloudWorkflowMigration-[version].jar -m -sd SANDBOX -pd PRODUCTION`.
    1. This creates a new folder with current timestamp. Rename it to `MATCH`.
    1. The folder contains: 
        1. `[Name].csv` - One file per object type. Contains sandbox object information, with additional columns `MATCH_RESULT` and `MATCH_WITH`. 
        1. Manually inspect each CSV file and check `MATCH_RESULT` column. If its value is `No Match` or `Collision`: 
            1. That means automatic matching failed. 
            1. For `Collision`, `MATCH_RESULT` contains multiple object IDs with the same `UNIQUE_NAME`.
            1. Modify `MATCH_WITH` to contain a single production object ID.
            1. Modify `MATCH_RESULT` to `Matched`.
1. Move unwanted mapper JSON file away from mapper folder.
1. Remap sandbox workflows: `java -jar JiraCloudWorkflowMigration-[version].jar -rw -sd SANDBOX -md MATCH`.
    1. This created a new folder with current timestmap. Rename it to `REMAP`.
    1. This folder contains one `Workflow ([Name]).json` per remapped workflow. 
1. Update production workflows: `java -jar JiraCloudWorkflowMigration-[version].jar -uw -wd REMAP -wl Workflow.txt`.