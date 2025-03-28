package org.openapitools.codegen.templating.mustache;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openapitools.codegen.CodegenConfig;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class CamelCaseLambdaTest extends LambdaTest {

    @Mock
    CodegenConfig generator;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void camelCaseTest() {
        // Given
        Map<String, Object> ctx = context("camelcase", new CamelCaseAndSanitizeLambda());

        // When & Then
        test("inputText", "{{#camelcase}}Input-text{{/camelcase}}", ctx);
    }

    @Test
    public void camelCaseSpaceTest() {
        // Given
        Map<String, Object> ctx = context("camelcase", new CamelCaseAndSanitizeLambda());

        // When & Then
        test("inputTextApi", "{{#camelcase}}Input text  api{{/camelcase}}", ctx);
    }

    @Test
    public void camelCaseReservedWordTest() {
        // Given
        Map<String, Object> ctx = context("camelcase", new CamelCaseAndSanitizeLambda().generator(generator));

        when(generator.sanitizeName(anyString())).then(returnsFirstArg());
        when(generator.reservedWords()).thenReturn(new HashSet<String>(Arrays.asList("reservedWord")));
        when(generator.escapeReservedWord("reservedWord")).thenReturn("escapedReservedWord");

        // When & Then
        test("escapedReservedWord", "{{#camelcase}}reserved-word{{/camelcase}}", ctx);
    }

    @Test
    public void camelCaseEscapeParamTest() {
        // Given
        Map<String, Object> ctx = context("camelcase", new CamelCaseAndSanitizeLambda()
                .generator(generator).escapeAsParamName(true));

        when(generator.sanitizeName(anyString())).then(returnsFirstArg());
        when(generator.reservedWords()).thenReturn(new HashSet<String>());
        when(generator.toParamName("inputText")).thenReturn("inputTextAsParam");

        // When & Then
        test("inputTextAsParam", "{{#camelcase}}Input_text{{/camelcase}}", ctx);
    }

}
