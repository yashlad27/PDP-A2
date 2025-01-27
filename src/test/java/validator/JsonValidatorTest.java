package validator;

import org.junit.Before;
import org.junit.Test;

import parser.InvalidJsonException;

import static org.junit.Assert.assertEquals;

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

  @Test(expected = InvalidJsonException.class)
  public void testInvalidKeyWithoutQuotes() throws InvalidJsonException {
    jsonValidator.input('{').input('k');
  }

  @Test
  public void testIncompleteJson() throws InvalidJsonException {
    jsonValidator.input('{').input('"').input('k').input('"').input(':');
    assertEquals("Status:Incomplete", jsonValidator.output());
  }

  @Test
  public void testInvalidClosingBrace() {
    try {
      jsonValidator.input('}');
    } catch (InvalidJsonException e) {
      assertEquals("Status:Invalid", jsonValidator.output());
    }
  }

  @Test
  public void testValidNestedStructures() throws InvalidJsonException {
    String nestedJson = "{\"a\":[{\"b\":\"c\"},{\"d\":\"e\"}]}";
    for (char c : nestedJson.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test(expected = InvalidJsonException.class)
  public void testMismatchedBrackets() throws InvalidJsonException {
    jsonValidator.input('[').input('}');
  }

  @Test(expected = InvalidJsonException.class)
  public void testInvalidExtraClosingBracket() throws InvalidJsonException {
    jsonValidator.input('}');
  }

  @Test
  public void testArrayWithStrings() throws InvalidJsonException {
    String jsonArray = "[\"a\",\"b\",\"c\"]";

    for (char c : jsonArray.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test (expected = InvalidJsonException.class)
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
  public void testInvalidCharacterInValue() throws InvalidJsonException {

  }

  @Test
  public void testEmptyArray() throws InvalidJsonException {
    String emptyArray = "[]";
    for (char c : emptyArray.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test (expected = InvalidJsonException.class)
  public void testUnclosedArray() throws InvalidJsonException {
    String invalidUnclosedArray = "[1,2";
    for (char c : invalidUnclosedArray.toCharArray()) {
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

  @Test (expected = InvalidJsonException.class)
  public void testInvalidJsonWithExtraColon() throws InvalidJsonException {
    String invalidJson = "{\"k\"::\"v\"}";
    for (char c : invalidJson.toCharArray()) {
      jsonValidator.input(c);
    }
  }
}