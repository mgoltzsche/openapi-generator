package org.openapitools.codegen.java.jaxrs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.openapitools.codegen.*;
import org.openapitools.codegen.java.assertions.JavaFileAssert;
import org.openapitools.codegen.languages.JavaJerseyServerCodegen;
import org.openapitools.codegen.languages.features.CXFServerFeatures;
import org.openapitools.codegen.templating.MustacheEngineAdapter;
import org.openapitools.codegen.testutils.ConfigAssert;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.openapitools.codegen.TestUtils.assertFileContains;

public class JavaJerseyServerCodegenTest extends JavaJaxrsBaseTest {

    @BeforeMethod
    public void before() {
        codegen = new JavaJerseyServerCodegen();
    }

    @Test
    public void testInitialConfigValues() throws Exception {
        Assert.assertEquals(codegen.getTag(), CodegenType.SERVER);
        Assert.assertEquals(codegen.getName(), "jaxrs-jersey");
        Assert.assertEquals(codegen.getTemplatingEngine().getClass(), MustacheEngineAdapter.class);
        Assert.assertEquals(codegen.getDateLibrary(), "legacy");
        Assert.assertEquals(codegen.supportedLibraries().keySet(), ImmutableSet.of("jersey2", "jersey3"));
        Assert.assertNull(codegen.getInputSpec());

        codegen.processOpts();

        OpenAPI openAPI = new OpenAPI();
        openAPI.addServersItem(new Server().url("https://api.abcde.xy:8082/v2"));
        codegen.preprocessOpenAPI(openAPI);

        ConfigAssert configAssert = new ConfigAssert(codegen.additionalProperties());
        configAssert.assertValue(CodegenConstants.HIDE_GENERATION_TIMESTAMP, codegen::isHideGenerationTimestamp, Boolean.FALSE);
        configAssert.assertValue(CodegenConstants.MODEL_PACKAGE, "org.openapitools.model");
        configAssert.assertValue(CodegenConstants.API_PACKAGE, "org.openapitools.api");
        configAssert.assertValue(CodegenConstants.INVOKER_PACKAGE, "org.openapitools.api");
        configAssert.assertValue(JavaJerseyServerCodegen.SERVER_PORT, "8082");
    }

    @Test
    public void testSettersForConfigValues() throws Exception {
        codegen.setHideGenerationTimestamp(true);
        codegen.setModelPackage("xx.yyyyyyyy.model");
        codegen.setApiPackage("xx.yyyyyyyy.api");
        codegen.setInvokerPackage("xx.yyyyyyyy.invoker");
        codegen.setDateLibrary("java8");
        codegen.processOpts();

        ConfigAssert configAssert = new ConfigAssert(codegen.additionalProperties());
        configAssert.assertValue(CodegenConstants.HIDE_GENERATION_TIMESTAMP, codegen::isHideGenerationTimestamp, true);
        configAssert.assertValue(CodegenConstants.MODEL_PACKAGE, "xx.yyyyyyyy.model");
        configAssert.assertValue(CodegenConstants.API_PACKAGE, "xx.yyyyyyyy.api");
        configAssert.assertValue(CodegenConstants.API_PACKAGE, "xx.yyyyyyyy.api");
        configAssert.assertValue(CodegenConstants.INVOKER_PACKAGE, "xx.yyyyyyyy.invoker");
        Assert.assertEquals(codegen.getDateLibrary(), "java8");
    }

    @Test
    public void testAdditionalPropertiesPutForConfigValues() throws Exception {
        codegen.additionalProperties().put(CodegenConstants.HIDE_GENERATION_TIMESTAMP, "true");
        codegen.additionalProperties().put(CodegenConstants.MODEL_PACKAGE, "xyz.yyyyy.mmmmm.model");
        codegen.additionalProperties().put(CodegenConstants.API_PACKAGE, "xyz.yyyyy.aaaaa.api");
        codegen.additionalProperties().put(CodegenConstants.INVOKER_PACKAGE, "xyz.yyyyy.iiii.invoker");
        codegen.additionalProperties().put("serverPort", "8088");
        codegen.processOpts();

        OpenAPI openAPI = new OpenAPI();
        openAPI.addServersItem(new Server().url("https://api.abcde.xy:8082/v2"));
        codegen.preprocessOpenAPI(openAPI);

        ConfigAssert configAssert = new ConfigAssert(codegen.additionalProperties());
        configAssert.assertValue(CodegenConstants.HIDE_GENERATION_TIMESTAMP, codegen::isHideGenerationTimestamp, true);
        configAssert.assertValue(CodegenConstants.MODEL_PACKAGE, codegen::modelPackage, "xyz.yyyyy.mmmmm.model");
        configAssert.assertValue(CodegenConstants.API_PACKAGE, codegen::apiPackage, "xyz.yyyyy.aaaaa.api");
        configAssert.assertValue(CodegenConstants.INVOKER_PACKAGE, codegen::getInvokerPackage, "xyz.yyyyy.iiii.invoker");
        Assert.assertEquals(codegen.additionalProperties().get(JavaJerseyServerCodegen.SERVER_PORT), "8088");
    }

    // Helper function, intended to reduce boilerplate @ copied from ../spring/SpringCodegenTest.java
    static private Map<String, File> generateFiles(DefaultCodegen codegen, String filePath) throws IOException {
        final File output = Files.createTempDirectory("test").toFile().getCanonicalFile();
        output.deleteOnExit();
        final String outputPath = output.getAbsolutePath().replace('\\', '/');

        codegen.setOutputDir(output.getAbsolutePath());
        codegen.additionalProperties().put(CXFServerFeatures.LOAD_TEST_DATA_FROM_FILE, "true");

        final ClientOptInput input = new ClientOptInput();
        final OpenAPI openAPI = new OpenAPIParser().readLocation(filePath, null, new ParseOptions()).getOpenAPI();
        input.openAPI(openAPI);
        input.config(codegen);

        final DefaultGenerator generator = new DefaultGenerator();
        final List<File> files = generator.opts(input).generate();

        Assert.assertTrue(files.size() > 0);
        TestUtils.validateJavaSourceFiles(files);
        TestUtils.validatePomXmlFiles(files);

        return files.stream().collect(Collectors.toMap(e -> e.getName().replace(outputPath, ""), i -> i));
    }

    @Test
    public void testJersey2Javax() throws Exception {
        codegen.setLibrary("jersey2");
        codegen.setDateLibrary("java8");
        codegen.setUseJakartaEe(false);

        final Map<String, File> files = generateFiles(codegen, "src/test/resources/3_0/petstore.yaml");

        files.values()
                .stream()
                .filter(file -> file.getName().endsWith(".java"))
                .forEach(file -> {
                    // Jersey2 uses "javax.ws.rs"
                    // Let's confirm that "jakarta.ws" is not present
                    TestUtils.assertFileNotContains(file.toPath(), "jakarta.ws");
                });
    }

    @Test
    public void testJersey2Jakarta() throws Exception {
        codegen.setLibrary("jersey2");
        codegen.setDateLibrary("java8");
        codegen.setUseJakartaEe(true);

        final Map<String, File> files = generateFiles(codegen, "src/test/resources/3_0/petstore.yaml");

        files.values()
                .stream()
                .filter(file -> file.getName().endsWith(".java"))
                .forEach(file -> {
                    // Jersey2 uses "javax.ws.rs"
                    // Let's confirm that "jakarta.ws" is not present
                    TestUtils.assertFileNotContains(file.toPath(), "javax.ws");
                });
    }

    @Test
    public void testJersey3() throws Exception {
        codegen.setLibrary("jersey3");
        codegen.setDateLibrary("java8");

        final Map<String, File> files = generateFiles(codegen, "src/test/resources/3_0/petstore.yaml");

        files.values()
                .stream()
                .filter(file -> file.getName().endsWith(".java"))
                .forEach(file -> {
                    // Jersey3 uses "jakarta.ws.rs"
                    // Let's confirm that "javax.ws" is not present
                    TestUtils.assertFileNotContains(file.toPath(), "javax.ws");
                });
    }

    @DataProvider(name = "codegenParameterMatrix")
    public Object[][] codegenParameterMatrix() {
        final Set<String> libraries = new JavaJerseyServerCodegen().supportedLibraries().keySet();
        final List<Object[]> rows = new ArrayList<Object[]>();
        for (final String jerseyLibrary : ImmutableList.of("jersey2")) {
            for (final String dateLibrary : ImmutableList.of("joda", "java8")) {
                rows.add(new Object[]{jerseyLibrary, dateLibrary});
            }
        }
        return rows.toArray(new Object[0][0]);
    }

    // almost same test as issue #3139 on Spring
    @Test(dataProvider = "codegenParameterMatrix")
    public void testMultipartJerseyServer(final String jerseyLibrary, final String dateLibrary) throws Exception {
        codegen.setLibrary(jerseyLibrary);
        codegen.setDateLibrary(dateLibrary);

        final Map<String, File> files = generateFiles(codegen, "src/test/resources/3_0/form-multipart-binary-array.yaml");

        // Check files for Single, Mixed
        String[] fileS = {
                "MultipartSingleApi.java", "MultipartSingleApiService.java", "MultipartSingleApiServiceImpl.java",
                "MultipartMixedApi.java", "MultipartMixedApiService.java", "MultipartMixedApiServiceImpl.java"};

        // UPDATE: the following test has been ignored due to https://github.com/OpenAPITools/openapi-generator/pull/11081/
        // We will contact the contributor of the following test to see if the fix will break their use cases and
        // how we can fix it accordingly.
        //for (String f : fileS){
        //    assertFileContains( files.get(f).toPath(), "FormDataBodyPart file" );
        //}

        // Check files for Array
        final String[] fileA = {"MultipartArrayApiService.java", "MultipartArrayApi.java", "MultipartArrayApiServiceImpl.java"};
        for (String f : fileA) {
            assertFileContains(files.get(f).toPath(), "List<FormDataBodyPart> files");
        }
    }

    @Test
    public void testHandleDefaultValue_issue8535() throws Exception {
        File output = Files.createTempDirectory("test").toFile().getCanonicalFile();
        output.deleteOnExit();

        OpenAPI openAPI = new OpenAPIParser()
                .readLocation("src/test/resources/3_0/issue_8535.yaml", null, new ParseOptions()).getOpenAPI();

        codegen.setOutputDir(output.getAbsolutePath());
        codegen.additionalProperties().put(CXFServerFeatures.LOAD_TEST_DATA_FROM_FILE, "true");

        ClientOptInput input = new ClientOptInput()
                .openAPI(openAPI)
                .config(codegen);

        DefaultGenerator generator = new DefaultGenerator();
        Map<String, File> files = generator.opts(input).generate().stream()
                .collect(Collectors.toMap(File::getName, Function.identity()));

        JavaFileAssert.assertThat(files.get("TestHeadersApi.java"))
                .assertMethod("headersTest")
                .assertParameter("headerNumber").hasType("BigDecimal")
                .assertParameterAnnotations()
                .containsWithNameAndAttributes("ApiParam", ImmutableMap.of("defaultValue", "\"11.2\""))
                .toParameter().toMethod()
                .assertParameter("headerString").hasType("String")
                .assertParameterAnnotations()
                .containsWithNameAndAttributes("ApiParam", ImmutableMap.of("defaultValue", "\"qwerty\""))
                .toParameter().toMethod()
                .assertParameter("headerStringWrapped").hasType("String")
                .assertParameterAnnotations()
                .containsWithNameAndAttributes("ApiParam", ImmutableMap.of("defaultValue", "\"qwerty\""))
                .toParameter().toMethod()
                .assertParameter("headerStringQuotes").hasType("String")
                .assertParameterAnnotations()
                .containsWithNameAndAttributes("ApiParam", ImmutableMap.of("defaultValue", "\"qwerty\\\"with quotes\\\" test\""))
                .toParameter().toMethod()
                .assertParameter("headerStringQuotesWrapped").hasType("String")
                .assertParameterAnnotations()
                .containsWithNameAndAttributes("ApiParam", ImmutableMap.of("defaultValue", "\"qwerty\\\"with quotes\\\" test\""))
                .toParameter().toMethod()
                .assertParameter("headerBoolean").hasType("Boolean")
                .assertParameterAnnotations()
                .containsWithNameAndAttributes("ApiParam", ImmutableMap.of("defaultValue", "\"true\""));

        JavaFileAssert.assertThat(files.get("TestQueryParamsApi.java"))
                .assertMethod("queryParamsTest")
                .assertParameter("queryNumber").hasType("BigDecimal")
                .assertParameterAnnotations()
                .containsWithNameAndAttributes("ApiParam", ImmutableMap.of("defaultValue", "\"11.2\""))
                .toParameter().toMethod()
                .assertParameter("queryString").hasType("String")
                .assertParameterAnnotations()
                .containsWithNameAndAttributes("ApiParam", ImmutableMap.of("defaultValue", "\"qwerty\""))
                .toParameter().toMethod()
                .assertParameter("queryStringWrapped").hasType("String")
                .assertParameterAnnotations()
                .containsWithNameAndAttributes("ApiParam", ImmutableMap.of("defaultValue", "\"qwerty\""))
                .toParameter().toMethod()
                .assertParameter("queryStringQuotes").hasType("String")
                .assertParameterAnnotations()
                .containsWithNameAndAttributes("ApiParam", ImmutableMap.of("defaultValue", "\"qwerty\\\"with quotes\\\" test\""))
                .toParameter().toMethod()
                .assertParameter("queryStringQuotesWrapped").hasType("String")
                .assertParameterAnnotations()
                .containsWithNameAndAttributes("ApiParam", ImmutableMap.of("defaultValue", "\"qwerty\\\"with quotes\\\" test\""))
                .toParameter().toMethod()
                .assertParameter("queryBoolean").hasType("Boolean")
                .assertParameterAnnotations()
                .containsWithNameAndAttributes("ApiParam", ImmutableMap.of("defaultValue", "\"true\""));
    }

}
