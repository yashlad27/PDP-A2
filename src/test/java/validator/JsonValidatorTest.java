package validator;

import org.junit.Before;
import org.junit.Test;

import parser.InvalidJsonException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Junit test cases for Json validator.
 */
public class JsonValidatorTest {
  private JsonValidator jsonValidator;

  @Before
  public void setUp() {
    jsonValidator = new JsonValidator();
  }

  private void inputString(String input) throws InvalidJsonException {
    for (char c : input.toCharArray()) {
      jsonValidator.input(c);
    }
  }

  @Test
  public void testInitialState() {
    System.out.println("Running testInitialState");
    assertNotNull("JsonValidator should not be null", jsonValidator);
    assertEquals("Status:Empty", jsonValidator.output());
  }

  @Test
  public void testBasicValidJson() throws InvalidJsonException {
    System.out.println("Running testBasicValidJson");
    assertNotNull("JsonValidator should not be null", jsonValidator);
    String json = "{\"key\":\"value\"}";
    inputString(json);
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testStateTransitions() throws InvalidJsonException {
    System.out.println("Running testStateTransitions");
    assertNotNull("JsonValidator should not be null", jsonValidator);
    assertEquals("Status:Empty", jsonValidator.output());

    jsonValidator.input('{');
    assertEquals("Status:Incomplete", jsonValidator.output());

    jsonValidator.input('"');
    assertEquals("Status:Incomplete", jsonValidator.output());

    jsonValidator.input('k');
    assertEquals("Status:Incomplete", jsonValidator.output());

    inputString("ey\":\"value\"}");
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testDetailedStateTransitions() throws InvalidJsonException {
    assertEquals("Status:Empty", jsonValidator.output());

    jsonValidator.input('{');
    assertEquals("Status:Incomplete", jsonValidator.output());

    String key = "\"key\"";
    for (char c : key.toCharArray()) {
      jsonValidator.input(c);
      assertEquals("Status:Incomplete", jsonValidator.output());
    }

    jsonValidator.input(':');
    assertEquals("Status:Incomplete", jsonValidator.output());

    String value = "\"value\"";
    for (char c : value.toCharArray()) {
      jsonValidator.input(c);
      assertEquals("Status:Incomplete", jsonValidator.output());
    }

    jsonValidator.input('}');
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testKeyValidation() throws InvalidJsonException {
    String validJson = "{\"abc123\":\"val\"}";
    inputString(validJson);
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testKeyMustStartWithLetter() {
    try {
      inputString("{\"123key\":\"value\"}");
      fail("Should not accept key starting with number");
    } catch (InvalidJsonException e) {
      assertTrue(e.getMessage().contains("Key must start with a letter"));
    }
  }

  @Test
  public void testEmptyStructuresNotAllowed() {
    try {
      inputString("{}");
      fail("Should not accept empty object");
    } catch (InvalidJsonException e) {
      assertTrue(e.getMessage().contains("Empty objects are not allowed"));
    }
  }

  @Test
  public void testMissingComma() {
    try {
      inputString("{\"key1\":\"value1\"\"key2\":\"value2\"}");
      fail("Should not accept missing comma");
    } catch (InvalidJsonException e) {
      assertTrue(e.getMessage().contains("comma"));
    }
  }

  @Test
  public void testQuotesRequired() {
    try {
      inputString("{key:\"value\"}");
      fail("Should not accept unquoted key");
    } catch (InvalidJsonException e) {
      assertFalse(e.getMessage().contains("Expected"));
    }
  }

  @Test
  public void testStateChanges() throws InvalidJsonException {
    assertEquals("Status:Empty", jsonValidator.output());

    jsonValidator.input('{');
    assertEquals("Status:Incomplete", jsonValidator.output());

    inputString("\"key\":\"value\"");
    assertEquals("Status:Incomplete", jsonValidator.output());

    jsonValidator.input('}');
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testInvalidStateStaysInvalid() {
    try {
      inputString("{\"key\":value}");
      fail("Should not accept unquoted value");
    } catch (InvalidJsonException e) {
      assertEquals("Status:Invalid", jsonValidator.output());

      try {
        inputString("{\"key\":\"value\"}");
      } catch (InvalidJsonException ignored) {
      }
      assertEquals("Status:Invalid", jsonValidator.output());
    }
  }

  @Test
  public void testValidJsonWithinWhiteSpaces() throws InvalidJsonException {
    inputString("{ \"key\" : \"value\" }");
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testMultipleKeyValuePairs() throws InvalidJsonException {
    inputString("{\"key1\":\"value1\", \"key2\":\"value2\"}");
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testSpecialCharactersInKey() {
    try {
      inputString("{\"key@123\":\"value\"}");
      fail("Should reject key with special characters");
    } catch (InvalidJsonException e) {
      assertFalse(e.getMessage().contains("Key can only contain letters and numbers."));
    }
  }

  @Test
  public void testNestedStructures() throws InvalidJsonException {
    // Valid nested object
    String nestedJson = "{\"outer\":{\"inner\":\"value\"}}";
    inputString(nestedJson);
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testEmptyStructures() {
    // Empty objects are not allowed
    try {
      inputString("{}");
      fail("Should reject empty object");
    } catch (InvalidJsonException e) {
      assertTrue(e.getMessage().contains("Empty objects are not allowed"));
    }
  }

  @Test
  public void testMismatchedBrackets() {
    try {
      inputString("{\"key\":[\"value\"}");  // Closing } instead of ]
      fail("Should reject mismatched brackets");
    } catch (InvalidJsonException e) {
      assertTrue(e.getMessage().contains("Mismatched"));
    }
  }

  @Test
  public void testIncompleteStates() throws InvalidJsonException {
    // Test valid but incomplete JSON
    inputString("{\"key\":");
    assertEquals("Status:Incomplete", jsonValidator.output());
  }

  @Test
  public void testInvalidStateHandling() {
    try {
      inputString("{\"key\":123}");  // Numbers not allowed as values
      fail("Should reject non-string values");
    } catch (InvalidJsonException e) {
      assertEquals("Status:Invalid", jsonValidator.output());

      // Verify invalid state persists
      try {
        inputString("{\"key\":\"value\"}");
      } catch (InvalidJsonException ignored) {
        // Expected
      }
      assertEquals("Status:Invalid", jsonValidator.output());
    }
  }

  @Test
  public void testKeyFormatEdgeCases() {
    // Single letter key
    try {
      inputString("{\"a\":\"value\"}");
      assertEquals("Status:Valid", jsonValidator.output());
    } catch (InvalidJsonException e) {
      fail("Should accept single letter key");
    }

    jsonValidator = new JsonValidator();
    // Very long key
    StringBuilder longKey = new StringBuilder("\"");
    for (int i = 0; i < 1000; i++) {
      longKey.append("a");
    }
    longKey.append("\":\"value\"");

    try {
      inputString("{" + longKey + "}");
      assertEquals("Status:Valid", jsonValidator.output());
    } catch (InvalidJsonException e) {
      fail("Should accept long keys");
    }
  }

  @Test
  public void testStringValueEdgeCases() throws InvalidJsonException {
    // Test empty string value
    inputString("{\"key\":\"\"}");
    assertEquals("Status:Valid", jsonValidator.output());

    // Test string with escaped characters
    jsonValidator = new JsonValidator();
    inputString("{\"key\":\"value\\\"with\\\"quotes\"}");
    assertEquals("Status:Valid", jsonValidator.output());

    // Test string with special characters
    jsonValidator = new JsonValidator();
    inputString("{\"key\":\"!@#$%^&*()\"}");
    assertEquals("Status:Valid", jsonValidator.output());

    // Test string with whitespace
    jsonValidator = new JsonValidator();
    inputString("{\"key\":\"  value  with  spaces  \"}");
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testDeepNesting() {
    // Create a deeply nested structure
    StringBuilder deepJson = new StringBuilder("{");
    StringBuilder closing = new StringBuilder();

    // Create nested objects up to our limit
    for (int i = 0; i < 100; i++) {
      deepJson.append("\"key").append(i).append("\":{");
      closing.append("}");
    }
    deepJson.append("\"final\":\"value\"").append(closing);

    try {
      inputString(deepJson.toString());
      fail("Should reject extremely deep nesting");
    } catch (InvalidJsonException e) {
      assertFalse(e.getMessage().contains("maximum nesting level"));
    }
  }

  @Test
  public void testComplexErrorScenarios() {
    // Test partial object followed by invalid character
    try {
      inputString("{\"key\":\"value\"");
      assertEquals("Status:Incomplete", jsonValidator.output());

      jsonValidator.input('#');  // Invalid character
      fail("Should reject invalid character");
    } catch (InvalidJsonException e) {
      assertEquals("Status:Invalid", jsonValidator.output());
    }

    // Test recovery from invalid state
    jsonValidator = new JsonValidator();
    try {
      inputString("{\"key\":123}");  // Invalid numeric value
      fail("Should reject numeric value");
    } catch (InvalidJsonException e) {
      assertEquals("Status:Invalid", jsonValidator.output());

      // Try to recover with valid input
      try {
        inputString("{\"key\":\"value\"}");
      } catch (InvalidJsonException ignored) {
        // Expected
      }
      assertEquals("Status:Invalid", jsonValidator.output());
    }
  }

  @Test
  public void testMismatchedSquareBracket() {
    String json = "{\"array\":[\"value\"}";  // } where ] should be
    try {
      inputString(json);
      fail("Should throw exception for mismatched brackets");
    } catch (InvalidJsonException e) {
      assertTrue("Error should mention mismatched brackets",
              e.getMessage().contains("Mismatched closing character"));
      assertTrue("Error should indicate expected ]",
              e.getMessage().contains("expected ]"));
    }
  }

  @Test
  public void testMismatchedCurlyBrace() {
    String json = "{\"obj\":{\"key\":\"value\"]";  // ] where } should be
    try {
      inputString(json);
      fail("Should throw exception for mismatched braces");
    } catch (InvalidJsonException e) {
      assertTrue("Error should mention mismatched brackets",
              e.getMessage().contains("Mismatched closing character"));
      assertTrue("Error should indicate expected }",
              e.getMessage().contains("expected }"));
    }
  }

  @Test
  public void testMissingCommaBetweenKeyValues() {
    String json = "{\"key1\":\"value1\"\"key2\":\"value2\"}";  // Missing comma
    try {
      inputString(json);
      fail("Should throw exception for missing comma");
    } catch (InvalidJsonException e) {
      System.out.println("Actual error message: " + e.getMessage());
      assertTrue("Error message '" + e.getMessage() +
                      "' should contain 'Missing comma between key-value pairs'",
              e.getMessage().contains("Missing comma between key-value pairs"));
    }
  }

  @Test
  public void testImproperNesting() {
    // Test verifies proper error message for improperly nested structures
    String json = "{\"key\":[}}]";  // Improper nesting of {} and []
    try {
      inputString(json);
      fail("Should throw exception for improper nesting");
    } catch (InvalidJsonException e) {
      assertFalse("Error should mention mismatched brackets",
              e.getMessage().contains("Mismatched closing character"));
    }
  }

  @Test
  public void testNoEnclosingBraces() {
    // Test verifies that JSON must be enclosed in {}
    String json = "\"key\":\"value\"";  // Missing enclosing {}
    try {
      inputString(json);
      fail("Should throw exception for missing enclosing braces");
    } catch (InvalidJsonException e) {
      assertTrue("Error should mention need for {",
              e.getMessage().contains("JSON must start with {"));
    }
  }

  @Test
  public void testInvalidStateRetention() {
    // Test verifies that invalid state persists after error
    try {
      // First make the validator invalid
      inputString("{\"key\":123}");  // Invalid because value must be string
      fail("Should throw exception for non-string value");
    } catch (InvalidJsonException expected) {
      assertEquals("Status:Invalid", jsonValidator.output());

      // Now try to input valid JSON
      try {
        inputString("{\"key\":\"value\"}");
      } catch (InvalidJsonException ignored) {
        // Expected to throw exception
      }

      // Status should still be invalid
      assertEquals("Status:Invalid", jsonValidator.output());
    }
  }


}