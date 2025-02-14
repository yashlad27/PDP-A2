package validator;

import org.junit.Before;
import org.junit.Test;

import parser.InvalidJsonException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Junit test cases to check and validate JsonValidator class.
 */
public class JsonValidatorTest {

  private JsonValidator jsonValidator;

  @Before
  public void setUp() throws Exception {
    jsonValidator = new JsonValidator();
  }

  @Test
  public void testInitialStatus() {
    assertEquals("Status:Empty", jsonValidator.output());
  }

  @Test
  public void testValidObjectInput() throws InvalidJsonException {
    String validJson = "{\"k\":\"v\"}";
    for (char c : validJson.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testMissingKeyInObject() {
    assertThrows(InvalidJsonException.class, () -> {
      jsonValidator.input('{');
      jsonValidator.input('1');
      jsonValidator.input('}');
    });
  }

  @Test
  public void testKeyWithoutValue() {
    assertThrows(InvalidJsonException.class, () -> {
      jsonValidator.input('{');
      jsonValidator.input('"');
      jsonValidator.input('k');
      jsonValidator.input('"');
      jsonValidator.input('}');
    });
  }

  @Test(expected = InvalidJsonException.class)
  public void testInvalidKeyWithoutQuotes() throws InvalidJsonException {
    jsonValidator.input('{').input('k');
  }

  @Test
  public void testInvalidClosingBrace() {
    try {
      jsonValidator.input('}');
    } catch (InvalidJsonException e) {
      assertEquals("Status:Invalid", jsonValidator.output());
    }
  }

  @Test(expected = InvalidJsonException.class)
  public void testMismatchedBracket() throws InvalidJsonException {
    String invalidJson = "{]";
    jsonValidator.input('{');
    jsonValidator.input(']');
  }

  @Test(expected = InvalidJsonException.class)
  public void testInvalidExtraClosingBracket() throws InvalidJsonException {
    jsonValidator.input('}');
  }

  @Test(expected = InvalidJsonException.class)
  public void testUnmatchedOpeningBracket() throws InvalidJsonException {
    jsonValidator.input('{').input('k').input('"');
  }

  @Test
  public void testInvalidCharacterInKey() throws InvalidJsonException {
    String validJson = "{\"@\":\"v\"}";
    for (char c : validJson.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testInvalidCharacterAfterKey() {
    assertThrows(InvalidJsonException.class, () -> {
      jsonValidator.input('{');
      jsonValidator.input('"');
      jsonValidator.input('k');
      jsonValidator.input('"');
      jsonValidator.input('a');
    });
  }

  @Test
  public void testEmptyArray() throws InvalidJsonException {
    String emptyArray = "[]";
    for (char c : emptyArray.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Incomplete", jsonValidator.output());
  }

  @Test
  public void testEmptyObject() throws InvalidJsonException {
    String emptyObjectJson = "{}";
    for (char c : emptyObjectJson.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Incomplete", jsonValidator.output());
  }

  @Test(expected = InvalidJsonException.class)
  public void testCommaBeforeClosingBracket() throws InvalidJsonException {
    String invalidJson = "{\"a\":1,}";
    for (char c : invalidJson.toCharArray()) {
      jsonValidator.input(c);
    }
  }

  @Test
  public void testValidJsonWithWhiteSpace() throws InvalidJsonException {
    String validJson = "{ \"k\" : \"v\" }";
    for (char c : validJson.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test(expected = InvalidJsonException.class)
  public void testInvalidJsonWithExtraColon() throws InvalidJsonException {
    String invalidJson = "{\"k\"::\"v\"}";
    for (char c : invalidJson.toCharArray()) {
      jsonValidator.input(c);
    }
  }

  @Test(expected = InvalidJsonException.class)
  public void testMissingColonAfterKey() throws InvalidJsonException {
    String invalidJson = "{\"key\"\"value\"}";
    for (char c : invalidJson.toCharArray()) {
      jsonValidator.input(c);
    }
  }

  @Test(expected = InvalidJsonException.class)
  public void testMissingSeparatorAfterKey() throws InvalidJsonException {
    String invalidJson = "{\"key\":\"a\",\"b\"\"}";
    for (char c : invalidJson.toCharArray()) {
      jsonValidator.input(c);
    }
  }

  @Test
  public void testValidNestedObject() throws InvalidJsonException {
    String validJson = "{\"key\":{\"z\":\"1\"}}";
    for (char c : validJson.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testValidJson() throws InvalidJsonException {
    String validJson = "{\"key\":\"abc\"}";
    for (char c : validJson.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testInvalidJsonWithMissingArrayBracket() {
    String invalidJson = "{\"details\":[\"name\", \"address\", \"age\":13}";
    try {
      for (char c : invalidJson.toCharArray()) {
        jsonValidator.input(c);
      }
      fail("Expected InvalidJsonException to be thrown, but it wasn't.");
    } catch (InvalidJsonException e) {
      assertEquals("Status:Invalid", jsonValidator.output());
    }
  }

  @Test
  public void testInvalidJsonWithMissingObjectBracket() {
    String invalidJson = "{\"details\":[\"name\", \"address\"], \"age\":13";
    try {
      for (char c : invalidJson.toCharArray()) {
        jsonValidator.input(c);
      }
      fail("Expected InvalidJsonException to be thrown, but it wasn't.");
    } catch (InvalidJsonException e) {
      assertEquals("Status:Invalid", jsonValidator.output());
    }
  }

  @Test
  public void testIncompleteJson() throws InvalidJsonException {
    jsonValidator.input('{');
    jsonValidator.input('"');
    jsonValidator.input('k');
    jsonValidator.input('"');
    jsonValidator.input(':');
    assertEquals("Status:Incomplete", jsonValidator.output());
  }

  @Test(expected = InvalidJsonException.class)
  public void testUnclosedArray() throws InvalidJsonException {
    String invalidUnclosedArray = "[1,2";
    for (char c : invalidUnclosedArray.toCharArray()) {
      jsonValidator.input(c);
    }
    jsonValidator.validateFinalState();
  }

  @Test(expected = InvalidJsonException.class)
  public void testNoKey() throws InvalidJsonException {
    String invalidJson = "{\"y\":\"a\",";
    for (char c : invalidJson.toCharArray()) {
      jsonValidator.input(c);
    }
    jsonValidator.validateFinalState();
  }

  @Test
  public void testTrailingCommaInObject() {
    assertThrows(InvalidJsonException.class, () -> {
      jsonValidator.input('{');
      jsonValidator.input('"');
      jsonValidator.input('k');
      jsonValidator.input('"');
      jsonValidator.input(':');
      jsonValidator.input('1');
      jsonValidator.input(',');
      jsonValidator.input('}');
    });
  }

  @Test
  public void testTrailingCommaInArray() {
    assertThrows(InvalidJsonException.class, () -> {
      jsonValidator.input('[');
      jsonValidator.input('1');
      jsonValidator.input(',');
      jsonValidator.input(']');
    });
  }

  @Test
  public void testNestedTrailingComma() {
    assertThrows(InvalidJsonException.class, () -> {
      jsonValidator.input('{');
      jsonValidator.input('"');
      jsonValidator.input('a');
      jsonValidator.input('"');
      jsonValidator.input(':');
      jsonValidator.input('[');
      jsonValidator.input('1');
      jsonValidator.input(',');
      jsonValidator.input(']');
      jsonValidator.input('}');
    });
  }

  @Test
  public void testInvalidNumberFormat() {
    assertThrows(InvalidJsonException.class, () -> {
      jsonValidator.input('0');
      jsonValidator.input('1');
    });
  }

  @Test
  public void testValidObjectWithMultipleKeys() throws InvalidJsonException {
    String validJson = "{\"a\":\"b\", \"c\":\"d\"}";
    for (char c : validJson.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testCommaInEmptyObject() {
    assertThrows(InvalidJsonException.class, () -> {
      jsonValidator.input('{');
      jsonValidator.input(',');
      jsonValidator.input('}');
    });
  }

  @Test
  public void testValidQuotedNumericKey() throws InvalidJsonException {
    String validJson = "{\"123\":\"value\"}";
    for (char c : validJson.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test(expected = InvalidJsonException.class)
  public void testInvalidUnquotedKey() throws InvalidJsonException {
    jsonValidator.input('{');
    jsonValidator.input('1');
  }

  @Test
  public void testUnquotedNumericKey() {
    assertThrows(InvalidJsonException.class, () -> {
      jsonValidator.input('{');
      jsonValidator.input('1');
      jsonValidator.input(':');
      jsonValidator.input('"');
      jsonValidator.input('v');
      jsonValidator.input('"');
      jsonValidator.input('}');
    });
  }

  @Test
  public void testInvalidNestedJson() {
    String invalidJson = "{\"n\": \"c\" { \"k\":\"v\"}}";
    try {
      for (char c : invalidJson.toCharArray()) {
        jsonValidator.input(c);
      }
    } catch (InvalidJsonException e) {
      assertEquals("Status:Invalid", jsonValidator.output());
    }
  }

  @Test
  public void testInvalidKeyNumber() {
    String invalidJson = "{\"9\": \"value\"}";
    try {
      for (char c : invalidJson.toCharArray()) {
        jsonValidator.input(c);
      }
    } catch (InvalidJsonException e) {
      assertEquals("Status:Invalid", jsonValidator.output());
    }
  }

  @Test
  public void testValidMultipleDuplicateKeys() throws InvalidJsonException {
    String validJson = "{ \"scene\": { \"instance\":\"\" ,\"instance\":\"\" " +
            ",\"instance\":\"\" ,\"instance\":\"\" } }";

    for (char c : validJson.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test(expected = InvalidJsonException.class)
  public void testInvalidUnquotedNumericKey() throws InvalidJsonException {
    jsonValidator.input('{');
    jsonValidator.input('9');
    jsonValidator.input(':');
    jsonValidator.input('"');
    jsonValidator.input('v');
    jsonValidator.input('a');
    jsonValidator.input('l');
    jsonValidator.input('u');
    jsonValidator.input('e');
    jsonValidator.input('"');
    jsonValidator.input('}');
  }

  @Test
  public void testOpenObjectInDefaultContext() throws InvalidJsonException {
    jsonValidator.input('{');
    assertEquals(1, jsonValidator.getStackSize());
    assertTrue(jsonValidator.isExpectingKey());
    assertFalse(jsonValidator.isInsideArray());
    assertFalse(jsonValidator.isCommaCheck());
  }

  @Test(expected = InvalidJsonException.class)
  public void testInvalidCommaBeforeObject() throws InvalidJsonException {
    jsonValidator.input(',');
    jsonValidator.input('{');
  }

  @Test(expected = InvalidJsonException.class)
  public void testInvalidCharBeforeObject() throws InvalidJsonException {
    jsonValidator.input('a');
    jsonValidator.input('{');
  }

  @Test
  public void testNestedObjectInArray() throws InvalidJsonException {
    jsonValidator.input('[');
    jsonValidator.input('{');
    assertEquals(2, jsonValidator.getStackSize());
    assertTrue(jsonValidator.isExpectingKey());
    assertTrue(jsonValidator.isInsideArray());
  }

  @Test
  public void testUnquotedKeys() {
    String testJson = "{key: \"value\"}";

    boolean exceptionThrows = false;
    for (int i = 0; i < testJson.length(); i++) {
      try {
        jsonValidator.input(testJson.charAt(i));
        if (i == 1) {
          assertEquals("Status:Invalid", jsonValidator.output());
        }
      } catch (InvalidJsonException e) {
        exceptionThrows = true;
      }
    }
    assertTrue(exceptionThrows);
  }

  @Test
  public void testUnclosedString() {
    String testJson = "{\"key\": \"u }";
    boolean exceptionThrows = false;

    for (int i = 0; i < testJson.length(); i++) {
      try {
        jsonValidator.input(testJson.charAt(i));
      } catch (InvalidJsonException e) {
        exceptionThrows = true;
      }
    }
    assertEquals("Status:Incomplete", jsonValidator.output());
  }

  @Test
  public void testNumericLeadingZero() {
    String testJson = "{\"n\": 012}";
    boolean exceptionThrows = false;

    for (int i = 0; i < testJson.length(); i++) {
      try {
        jsonValidator.input(testJson.charAt(i));
      } catch (InvalidJsonException e) {
        exceptionThrows = true;
        assertEquals("Status:Invalid", jsonValidator.output());
      }
    }
    assertTrue(exceptionThrows);
  }

  @Test
  public void testBooleanCaseSensitivity() {
    String testJson = "{\"flag\": TRUE}";
    boolean exceptionThrows = false;

    for (int i = 0; i < testJson.length(); i++) {
      try {
        jsonValidator.input(testJson.charAt(i));
      } catch (InvalidJsonException e) {
        exceptionThrows = true;
        assertEquals("Status:Invalid", jsonValidator.output());
      }
    }
    assertTrue(exceptionThrows);
  }

  @Test
  public void testMixedNesting() throws InvalidJsonException {
    String testJson = "{\"a\": [1, {\"b\": [2]}]}";

    for (int i = 0; i < testJson.length(); i++) {
      jsonValidator.input(testJson.charAt(i));
    }
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test
  public void testUnexpectedCharacter() {
    String testJson = "{\"key\": #value}";
    boolean exceptionThrows = false;

    for (int i = 0; i < testJson.length(); i++) {
      try {
        jsonValidator.input(testJson.charAt(i));
        if (testJson.charAt(i) == '#' || testJson.charAt(i) == '@' || testJson.charAt(i) == '$'
                || testJson.charAt(i) == '%'
                || testJson.charAt(i) == '!' || testJson.charAt(i) == '^'
                || testJson.charAt(i) == '&' || testJson.charAt(i) == '~'
                || testJson.charAt(i) == '*' || testJson.charAt(i) == '?'
                || testJson.charAt(i) == '+' || testJson.charAt(i) == '`'
                || testJson.charAt(i) == '<' || testJson.charAt(i) == '>'
                || testJson.charAt(i) == '_' || testJson.charAt(i) == '-') {
          assertEquals("Status:Invalid", jsonValidator.output());
        }
      } catch (InvalidJsonException e) {
        exceptionThrows = true;
      }
    }
    assertTrue(exceptionThrows);
  }

  @Test
  public void testMidStringTermination() {
    String testJson = "{\"key\": \"value";

    for (int i = 0; i < testJson.length(); i++) {
      try {
        jsonValidator.input(testJson.charAt(i));
      } catch (InvalidJsonException e) {
        fail("Should not throw exception for incomplete string,");
      }
    }
    assertEquals("Status:Incomplete", jsonValidator.output());
  }

  @Test
  public void testInvalidJsonWithSubsequentValidInput() {
    JsonValidator validator = new JsonValidator();
    String json = "{\"name\":\"cs5010\"{"; // Invalid JSON

    boolean exceptionThrown = false;
    for (int i = 0; i < json.length(); i++) {
      char c = json.charAt(i);
      try {
        validator.input(c);
      } catch (InvalidJsonException e) {
        exceptionThrown = true;
        assertEquals("Status:Invalid", validator.output());
      }
    }

    // Provide valid input after the JSON is already invalid
    try {
      validator.input('}'); // Attempt to fix the JSON
    } catch (InvalidJsonException e) {
      fail("Should not throw exception after JSON is already invalid");
    }
    assertEquals("Status:Invalid", validator.output()); // Status should remain Invalid
  }

  @Test
  public void testInvalidJsonWithWhitespace() {
    JsonValidator validator = new JsonValidator();
    String json = "{\"name\": \"cs5010\" {"; // Invalid JSON with whitespace

    boolean exceptionThrown = false;
    for (int i = 0; i < json.length(); i++) {
      char c = json.charAt(i);
      try {
        validator.input(c);
      } catch (InvalidJsonException e) {
        exceptionThrown = true;
        assertEquals("Status:Invalid", validator.output());
      }
    }

    assertTrue(exceptionThrown);
    assertEquals("Status:Invalid", validator.output());
  }
}
