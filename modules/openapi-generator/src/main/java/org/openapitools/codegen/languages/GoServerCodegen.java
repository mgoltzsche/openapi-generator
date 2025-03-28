/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.languages;

import com.google.common.collect.Iterables;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.meta.features.*;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static org.openapitools.codegen.utils.StringUtils.camelize;

public class GoServerCodegen extends AbstractGoCodegen {

    /**
     * Name of additional property for switching routers
     */
    private static final String ROUTER_SWITCH = "router";

    /**
     * Description of additional property for switching routers
     */
    private static final String ROUTER_SWITCH_DESC = "Specify the router which should be used.";

    /**
     * List of available routers
     */
    private static final String[] ROUTERS = {"mux", "chi"};

    private final Logger LOGGER = LoggerFactory.getLogger(GoServerCodegen.class);

    @Setter protected String packageVersion = "1.0.0";
    @Setter protected int serverPort = 8080;
    protected String projectName = "openapi-server";
    @Setter protected String sourceFolder = "go";
    protected Boolean corsFeatureEnabled = false;
    @Setter protected Boolean addResponseHeaders = false;
    @Setter protected Boolean outputAsLibrary = false;
    @Setter protected Boolean onlyInterfaces = false;


    public GoServerCodegen() {
        super();

        // skip sorting of operations to preserve the order found in the OpenAPI spec file
        super.setSkipSortingOperations(true);

        modifyFeatureSet(features -> features
                .includeDocumentationFeatures(DocumentationFeature.Readme)
                .wireFormatFeatures(EnumSet.of(WireFormatFeature.JSON, WireFormatFeature.XML))
                .securityFeatures(EnumSet.noneOf(
                        SecurityFeature.class
                ))
                .excludeGlobalFeatures(
                        GlobalFeature.XMLStructureDefinitions,
                        GlobalFeature.Callbacks,
                        GlobalFeature.LinkObjects,
                        GlobalFeature.ParameterStyling
                )
                .excludeSchemaSupportFeatures(
                        SchemaSupportFeature.Polymorphism
                )
                .excludeParameterFeatures(
                        ParameterFeature.Cookie
                )
        );

        // set the output folder here
        outputFolder = "generated-code/go";

        cliOptions.add(new CliOption(CodegenConstants.SOURCE_FOLDER, CodegenConstants.SOURCE_FOLDER_DESC)
                .defaultValue(sourceFolder));

        CliOption frameworkOption = new CliOption(ROUTER_SWITCH, ROUTER_SWITCH_DESC);
        for (String option : ROUTERS) {
            frameworkOption.addEnum(option, option);
        }
        frameworkOption.defaultValue(ROUTERS[0]);
        cliOptions.add(frameworkOption);

        CliOption optServerPort = new CliOption("serverPort", "The network port the generated server binds to");
        optServerPort.setType("int");
        optServerPort.defaultValue(Integer.toString(serverPort));
        cliOptions.add(optServerPort);

        CliOption optFeatureCORS = new CliOption("featureCORS", "Enable Cross-Origin Resource Sharing middleware");
        optFeatureCORS.setType("bool");
        optFeatureCORS.defaultValue(corsFeatureEnabled.toString());
        cliOptions.add(optFeatureCORS);

        cliOptions.add(CliOption.newBoolean(CodegenConstants.ENUM_CLASS_PREFIX, CodegenConstants.ENUM_CLASS_PREFIX_DESC));

        // option to include headers in the response
        CliOption optAddResponseHeaders = new CliOption("addResponseHeaders", "To include response headers in ImplResponse");
        optAddResponseHeaders.setType("bool");
        optAddResponseHeaders.defaultValue(addResponseHeaders.toString());
        cliOptions.add(optAddResponseHeaders);


        // option to exclude service factories; only interfaces are rendered
        CliOption optOnlyInterfaces = new CliOption("onlyInterfaces", "Exclude default service creators from output; only generate interfaces");
        optOnlyInterfaces.setType("bool");
        optOnlyInterfaces.defaultValue(onlyInterfaces.toString());
        cliOptions.add(optOnlyInterfaces);

        // option to exclude main package (main.go), Dockerfile, and go.mod files
        CliOption optOutputAsLibrary = new CliOption("outputAsLibrary", "Exclude main.go, go.mod, and Dockerfile from output");
        optOutputAsLibrary.setType("bool");
        optOutputAsLibrary.defaultValue(outputAsLibrary.toString());
        cliOptions.add(optOutputAsLibrary);
        /*
         * Models.  You can write model files using the modelTemplateFiles map.
         * if you want to create one template for file, you can do so here.
         * for multiple files for model, just put another entry in the `modelTemplateFiles` with
         * a different extension
         */
        modelTemplateFiles.put(
                "model.mustache",
                ".go");

        /*
         * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
         * as with models, add multiple entries with different extensions for multiple files per
         * class
         */
        apiTemplateFiles.put(
                "controller-api.mustache",   // the template to use
                ".go");       // the extension for each file to write

        /*
         * Service templates.  You can write services for each Api file with the apiTemplateFiles map.
            These services are skeletons built to implement the logic of your api using the
            expected parameters and response.
         */
        apiTemplateFiles.put(
                "service.mustache",   // the template to use
                "_service.go");       // the extension for each file to write

        /*
         * Template Location.  This is the location which templates will be read from.  The generator
         * will use the resource stream to attempt to read the templates.
         */
        embeddedTemplateDir = templateDir = "go-server";

        /*
         * Reserved words.  Override this with reserved words specific to your language
         */
        setReservedWordsLowerCase(
                Arrays.asList(
                        // data type
                        "string", "bool", "uint", "uint8", "uint16", "uint32", "uint64",
                        "int", "int8", "int16", "int32", "int64", "float32", "float64",
                        "complex64", "complex128", "rune", "byte", "uintptr",

                        "break", "default", "func", "interface", "select",
                        "case", "defer", "go", "map", "struct",
                        "chan", "else", "goto", "package", "switch",
                        "const", "fallthrough", "if", "range", "type",
                        "continue", "for", "import", "return", "var", "error", "nil")
                // Added "error" as it's used so frequently that it may as well be a keyword
        );
    }

    @Override
    public void processOpts() {
        super.processOpts();
        /*
         * Additional Properties.  These values can be passed to the templates and
         * are available in models, apis, and supporting files
         */
        if (additionalProperties.containsKey(CodegenConstants.PACKAGE_NAME)) {
            setPackageName((String) additionalProperties.get(CodegenConstants.PACKAGE_NAME));
        } else {
            setPackageName("openapi");
            additionalProperties.put(CodegenConstants.PACKAGE_NAME, packageName);
        }

        if (additionalProperties.containsKey(CodegenConstants.PACKAGE_VERSION)) {
            this.setPackageVersion((String) additionalProperties.get(CodegenConstants.PACKAGE_VERSION));
        } else {
            additionalProperties.put(CodegenConstants.PACKAGE_VERSION, packageVersion);
        }

        if (additionalProperties.containsKey(CodegenConstants.SOURCE_FOLDER)) {
            this.setSourceFolder((String) additionalProperties.get(CodegenConstants.SOURCE_FOLDER));
        } else {
            additionalProperties.put(CodegenConstants.SOURCE_FOLDER, sourceFolder);
        }

        if (additionalProperties.containsKey("serverPort") && additionalProperties.get("serverPort") instanceof Integer) {
            this.setServerPort((int) additionalProperties.get("serverPort"));
        } else if (additionalProperties.containsKey("serverPort") && additionalProperties.get("serverPort") instanceof String) {
            try {
                this.setServerPort(Integer.parseInt(additionalProperties.get("serverPort").toString()));
                additionalProperties.put("serverPort", serverPort);
            } catch (NumberFormatException e) {
                LOGGER.warn("serverPort is not a valid integer... defaulting to {}", serverPort);
                additionalProperties.put("serverPort", serverPort);
            }
        } else {
            additionalProperties.put("serverPort", serverPort);
        }

        if (additionalProperties.containsKey("featureCORS")) {
            this.setFeatureCORS(convertPropertyToBooleanAndWriteBack("featureCORS"));
        } else {
            additionalProperties.put("featureCORS", corsFeatureEnabled);
        }

        if (additionalProperties.containsKey("addResponseHeaders")) {
            this.setAddResponseHeaders(convertPropertyToBooleanAndWriteBack("addResponseHeaders"));
        } else {
            additionalProperties.put("addResponseHeaders", addResponseHeaders);
        }

        if (additionalProperties.containsKey("onlyInterfaces")) {
            this.setOnlyInterfaces(convertPropertyToBooleanAndWriteBack("onlyInterfaces"));
        } else {
            additionalProperties.put("onlyInterfaces", onlyInterfaces);
        }

        if (this.onlyInterfaces) {
            apiTemplateFiles.remove("service.mustache");
        }

        if (additionalProperties.containsKey("outputAsLibrary")) {
            this.setOutputAsLibrary(convertPropertyToBooleanAndWriteBack("outputAsLibrary"));
        } else {
            additionalProperties.put("outputAsLibrary", outputAsLibrary);
        }

        if (additionalProperties.containsKey(CodegenConstants.ENUM_CLASS_PREFIX)) {
            setEnumClassPrefix(Boolean.parseBoolean(additionalProperties.get(CodegenConstants.ENUM_CLASS_PREFIX).toString()));
            if (enumClassPrefix) {
                additionalProperties.put(CodegenConstants.ENUM_CLASS_PREFIX, true);
            }
        }

        additionalProperties.putIfAbsent(ROUTER_SWITCH, ROUTERS[0]);

        final Object propRouter = additionalProperties.get(ROUTER_SWITCH);
        final Map<String, Boolean> routers = new HashMap<>();
        for (String router : ROUTERS) {
            routers.put(router, router.equals(propRouter));
        }
        additionalProperties.put("routers", routers);

        modelPackage = packageName;
        apiPackage = packageName;

        /*
         * Supporting Files.  You can write single files for the generator with the
         * entire object tree available.  If the input file has a suffix of `.mustache
         * it will be processed by the template engine.  Otherwise, it will be copied
         */
        if (!outputAsLibrary) {
            supportingFiles.add(new SupportingFile("main.mustache", "", "main.go"));
            supportingFiles.add(new SupportingFile("Dockerfile.mustache", "", "Dockerfile"));
            supportingFiles.add(new SupportingFile("go.mod.mustache", "", "go.mod"));
        }
        supportingFiles.add(new SupportingFile("openapi.mustache", "api", "openapi.yaml"));
        supportingFiles.add(new SupportingFile("routers.mustache", sourceFolder, "routers.go"));
        supportingFiles.add(new SupportingFile("logger.mustache", sourceFolder, "logger.go"));
        supportingFiles.add(new SupportingFile("impl.mustache", sourceFolder, "impl.go"));
        supportingFiles.add(new SupportingFile("helpers.mustache", sourceFolder, "helpers.go"));
        supportingFiles.add(new SupportingFile("api.mustache", sourceFolder, "api.go"));
        supportingFiles.add(new SupportingFile("error.mustache", sourceFolder, "error.go"));
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md")
                .doNotOverwrite());
    }

    @Override
    public ModelsMap postProcessModels(ModelsMap objs) {
        // The superclass determines the list of required golang imports. The actual list of imports
        // depends on which types are used. So super.postProcessModels must be invoked at the beginning
        // of this method.
        objs = super.postProcessModels(objs);

        List<Map<String, String>> imports = objs.getImports();

        for (ModelMap m : objs.getModels()) {
//            imports.add(createMapping("import", "encoding/json"));

            CodegenModel model = m.getModel();
            if (model.isEnum) {
                imports.add(createMapping("import", "fmt"));
                continue;
            }

            Boolean importErrors = false;

            for (CodegenProperty param : Iterables.concat(model.vars, model.allVars, model.requiredVars, model.optionalVars)) {
                if (param.isNumeric && (StringUtils.isNotEmpty(param.minimum) || StringUtils.isNotEmpty(param.maximum))) {
                    importErrors = true;
                }
            }

            if (importErrors) {
                imports.add(createMapping("import", "errors"));
            }
        }
        return objs;
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        // TODO: refactor abstractGoCodegen, decouple go client only code and remove this
        OperationMap objectMap = objs.getOperations();
        List<CodegenOperation> operations = objectMap.getOperation();

        for (CodegenOperation operation : operations) {
            // http method verb conversion (e.g. PUT => Put)
            operation.httpMethod = camelize(operation.httpMethod.toLowerCase(Locale.ROOT));
        }

        // remove model imports to avoid error
        List<Map<String, String>> imports = objs.getImports();
        if (imports == null)
            return objs;

        Iterator<Map<String, String>> iterator = imports.iterator();
        while (iterator.hasNext()) {
            String _import = iterator.next().get("import");
            if (_import.startsWith(apiPackage))
                iterator.remove();
        }

        boolean addedTimeImport = false;
        boolean addedOSImport = false;
        boolean addedReflectImport = false;
        for (CodegenOperation operation : operations) {
            for (CodegenParameter param : operation.allParams) {
                // import "os" if the operation uses files
                if (!addedOSImport && ("*os.File".equals(param.dataType) || "[]*os.File".equals(param.dataType))) {
                    imports.add(createMapping("import", "os"));
                    addedOSImport = true;
                }

                // import "time" if the operation has a time parameter.
                if (!addedTimeImport && "time.Time".equals(param.dataType)) {
                    imports.add(createMapping("import", "time"));
                    addedTimeImport = true;
                }

                // import "reflect" package if the parameter is collectionFormat=multi
                if (!addedReflectImport && param.isCollectionFormatMulti) {
                    imports.add(createMapping("import", "reflect"));
                    addedReflectImport = true;
                }

                // set x-exportParamName
                char nameFirstChar = param.paramName.charAt(0);
                if (Character.isUpperCase(nameFirstChar)) {
                    // First char is already uppercase, just use paramName.
                    param.vendorExtensions.put("x-export-param-name", param.paramName);
                } else {
                    // It's a lowercase first char, let's convert it to uppercase
                    StringBuilder sb = new StringBuilder(param.paramName);
                    sb.setCharAt(0, Character.toUpperCase(nameFirstChar));
                    param.vendorExtensions.put("x-export-param-name", sb.toString());
                }
            }

        }

        // recursively add import for mapping one type to multiple imports
        List<Map<String, String>> recursiveImports = objs.getImports();
        if (recursiveImports == null)
            return objs;

        ListIterator<Map<String, String>> listIterator = imports.listIterator();
        while (listIterator.hasNext()) {
            String _import = listIterator.next().get("import");
            // if the import package happens to be found in the importMapping (key)
            // add the corresponding import package to the list
            if (importMapping.containsKey(_import)) {
                listIterator.add(createMapping("import", importMapping.get(_import)));
            }
        }

        this.addConditionalImportInformation(objs);

        return objs;
    }

    private void addConditionalImportInformation(OperationsMap operations) {
        boolean hasPathParams = false;
        boolean hasBodyParams = false;

        for (CodegenOperation op : operations.getOperations().getOperation()) {
            if (op.getHasPathParams()) {
                hasPathParams = true;
            }
            if (op.getHasBodyParam()) {
                hasBodyParams = true;
            }
        }

        additionalProperties.put("hasPathParams", hasPathParams);
        additionalProperties.put("hasBodyParams", hasBodyParams);
    }


    @Override
    public String apiPackage() {
        return sourceFolder;
    }

    /**
     * Configures the type of generator.
     *
     * @return the CodegenType for this generator
     * @see org.openapitools.codegen.CodegenType
     */
    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    /**
     * Configures a friendly name for the generator.  This will be used by the generator
     * to select the library with the -g flag.
     *
     * @return the friendly name for the generator
     */
    @Override
    public String getName() {
        return "go-server";
    }

    /**
     * Returns human-friendly help for the generator.  Provide the consumer with help
     * tips, parameters here
     *
     * @return A string value for the help message
     */
    @Override
    public String getHelp() {
        return "Generates a Go server library using OpenAPI-Generator. By default, " +
                "it will also generate service classes -- which you can disable with the `-Dnoservice` environment variable.";
    }

    /**
     * Location to write api files.  You can use the apiPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + apiPackage().replace('.', File.separatorChar);
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + File.separator + apiPackage().replace('.', File.separatorChar);
    }

    public void setFeatureCORS(Boolean featureCORS) {
        this.corsFeatureEnabled = featureCORS;
    }

}
