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
  public void testEmptyArray() throws InvalidJsonException {
    String emptyArray = "[]";
    for (char c : emptyArray.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
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
  public void testNestedArrayInObject() throws InvalidJsonException {
    String validJson = "{\"key\":[\"a\",\"b\"]}";
    for (char c : validJson.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
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

  @Test
  public void testValidJsonWithWhitespace() throws InvalidJsonException {
    String validJson = "{ \"key\" : \"value\" }";
    for (char c : validJson.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
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
  public void testValidJson1() throws InvalidJsonException {
    String validJson = "{\"key\":\"abc\"}";
    for (char c : validJson.toCharArray()) {
      jsonValidator.input(c);
    }
    assertEquals("Status:Valid", jsonValidator.output());
  }

  @Test(expected = InvalidJsonException.class)
  public void testJsonMismatchedBrackets() throws InvalidJsonException {
    String invalidJson = "{\"z\":\"h\"]";
    for (char c : invalidJson.toCharArray()) {
      jsonValidator.input(c);
    }
  }
}