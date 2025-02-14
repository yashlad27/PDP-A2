package validator;

import java.util.Stack;

import parser.InvalidJsonException;
import parser.JsonParser;

/**
 * A Json syntax validator that checks input characters for compliance with JSON standards.
 * including proper structure, key formatting, and nesting. It tracks the parser state to determine
 * validity and throws exceptions for syntax errors.
 * <p>
 * Validator is using a stack based approach to manage nested structures
 * and boolean flags to track different context states.
 * </p>
 */
public class JsonValidator implements JsonParser<String> {

  private Stack<Character> stack;

  private boolean inString;
  private boolean escapeNext;
  private boolean isInvalid;

  private boolean expectingKey;
  private boolean expectingColon;
  private boolean expectingCommaOrEnd;
  private boolean insideArray;
  private boolean commaCheck;

  private String status;

  /**
   * Constructs a new JSON validator and initialises it's iternal state.
   * <p>
   * Initialises the stack for tracking structures and resets all boolean flags.
   * </p>
   */
  public JsonValidator() {
    this.stack = new Stack<>();
    this.inString = false;
    this.escapeNext = false;
    this.isInvalid = false;
    this.expectingKey = true;
    this.expectingColon = false;
    this.expectingCommaOrEnd = false;
    this.insideArray = false;
    this.commaCheck = false;
    this.status = "Status:Empty";
  }

  /**
   * Accept a single character as input, and update the parser state.
   *
   * @param c the input character
   * @return the parser after handling the provided character
   * @throws InvalidJsonException if the input causes the JSON to be invalid
   */
  @Override
  public JsonParser<String> input(char c) throws InvalidJsonException {
    if (isInvalid) {
      return this;
    }
    try {
      if (inString) {
        handleStringContext(c);
      } else {
        handleGeneralContext(c);
      }
      updateStatus();
    } catch (InvalidJsonException e) {
      this.isInvalid = true;
      this.status = "Status:Invalid";
      throw e;
    }
    return this;
  }

  /**
   * Provide the output of the parser, given all the inputs it has been provided so far.
   *
   * @return One of:
   * <ul>
   *   <li>`Status:Empty` - no input processed yet.</li>
   *   <li>`Status:Valid` - input is valid and complete JSON.</li>
   *   <li>`Status:Incomplete` - Input is valid but incomplete.</li>
   *   <li>`Status:Invalid` - Input contains syntax errors.</li>
   * </ul>
   */
  @Override
  public String output() {
    return status;
  }

  /**
   * Validates that all JSON structures are properly closed after processing input.
   *
   * @throws InvalidJsonException If unclosed structures remain ( unmatched `{` or `[` ).
   */
  public void validateFinalState() throws InvalidJsonException {
    if (!stack.isEmpty()) {
      throw new InvalidJsonException("Unclosed JSON structure");
    }
  }

  /**
   * Handle characters when inside a JSON string i.e. escaped characters and closing quotes.
   *
   * @param c the input character
   * @throws InvalidJsonException if an invalid escape sequence is detected.
   */
  private void handleStringContext(char c) throws InvalidJsonException {
    if (escapeNext) {
      escapeNext = false;
    } else if (c == '\\') {
      escapeNext = true;
    } else if (c == '"') {
      inString = false;
      if (!expectingKey) {
        expectingCommaOrEnd = true;
      }
    }
  }

  /**
   * Handle characters outside of strings.
   *
   * @param c the input character.
   * @throws InvalidJsonException if the input causes structural issues.
   */
  private void handleGeneralContext(char c) throws InvalidJsonException {
    if (Character.isWhitespace(c)) {
      return;
    }

    if (expectingKey) {
      handleExpectingKey(c);
    } else if (expectingColon) {
      handleExpectingColon(c);
    } else if (expectingCommaOrEnd) {
      handleExpectingCommaOrEnd(c);
    } else {
      handleDefaultContext(c);
    }
  }

  /**
   * Validates that a colon : follows a key in an object.
   *
   * @param c the input character.
   * @throws InvalidJsonException if the character is not a colon.
   */
  private void handleExpectingColon(char c) throws InvalidJsonException {
    if (c == ':') {
      expectingColon = false;
    } else if (c == ',') {
      expectingColon = false;
      expectingKey = true;
    } else {
      throw new InvalidJsonException("Expected ':' after key. Found: " + c);
    }
  }

  /**
   * Handles characters when a comma or closing brace is expected after a value.
   *
   * @param c the input character.
   * @throws InvalidJsonException for:
   *                              <ul>
   *                                <li>unexpected commas.</li>
   *                                <li>trailing commas before closing braces.</li>
   *                                <li>invalid characters in the context.</li>
   *                              </ul>
   */
  private void handleExpectingCommaOrEnd(char c) throws InvalidJsonException {
    if (c == ',') {
      if (stack.isEmpty()) {
        throw new InvalidJsonException("Unexpected comma.");
      }
      char top = stack.peek();
      if (top == '{') {
        expectingCommaOrEnd = false;
        commaCheck = true;
        expectingKey = true;
      } else if (top == '[') {
        expectingCommaOrEnd = false;
        commaCheck = true;
      } else {
        throw new InvalidJsonException("Unexpected comma.");
      }
    } else if (c == ']' || c == '}') {
      if (commaCheck) {
        throw new InvalidJsonException("Trailing comma before closing: " + c);
      }
      closeStructure(c);
      expectingCommaOrEnd = !stack.isEmpty();
      insideArray = !stack.isEmpty() && stack.peek() == '[';
    } else if (c == '{' || c == '[') {
      stack.push(c);
      expectingKey = (c == '{');
      expectingCommaOrEnd = false;
      insideArray = (c == '[');
    } else if (c == '"') {
      inString = true;
      commaCheck = false;
      expectingCommaOrEnd = false;
    } else {
      throw new InvalidJsonException("Expected comma, closing bracket, or new structure. Found: "
              + c);
    }
  }

  /**
   * Handles characters in default context i.e. structural tokens and values in arrays.
   *
   * @param c the input character
   * @throws InvalidJsonException For:
   *                              <ul>
   *                                <li>Unexpected characters outside arrays.</li>
   *                                <li>Mismatched closing brackets.</li>
   *                              </ul>
   */
  private void handleDefaultContext(char c) throws InvalidJsonException {
    switch (c) {
      case '{':
      case '[':
        stack.push(c);
        expectingKey = (c == '{');
        insideArray = (c == '[');
        commaCheck = false;
        break;
      case '}':
      case ']':
        closeStructure(c);
        expectingCommaOrEnd = !stack.isEmpty();
        break;
      case '\"':
        inString = true;
        commaCheck = false;
        break;
      default:
        if (insideArray) {
          commaCheck = false;
          expectingCommaOrEnd = true;
          break;
        }
        throw new InvalidJsonException("Unexpected character: " + c);
    }
  }

  /**
   * Close a structure (object or array) and validates the stack state.
   *
   * @param c the closing character ( } or ] ).
   * @throws InvalidJsonException For:
   *                              <ul>
   *                                <li>Mismatched closing characters for an array.</li>
   *                                <li>Trailing commas before closure.</li>
   *                              </ul>
   */
  private void closeStructure(char c) throws InvalidJsonException {
    if (stack.isEmpty()) {
      throw new InvalidJsonException("Unexpected closing character: " + c);
    }
    char expected = stack.pop();
    if ((c == '}' && expected != '{') || (c == ']' && expected != '[')) {
      throw new InvalidJsonException("Mismatched closing character: " + c);
    }
    if (commaCheck) {
      throw new InvalidJsonException("Mismatch comma after closing character: " + c);
    }
    insideArray = !stack.isEmpty() && stack.peek() == '[';
    commaCheck = false;
  }

  /**
   * Update the current status of the parser.
   */
  private void updateStatus() {
    if (isInvalid) {
      status = "Status:Invalid";
      return;
    } else if (stack.isEmpty() && !inString && !expectingKey && !expectingColon) {
      status = "Status:Valid";
    } else {
      status = "Status:Incomplete";
    }
  }

  /**
   * Handles the characters when a key is expected in an object.
   *
   * @param c the input character.
   * @throws InvalidJsonException For:
   *                              <ul>
   *                                <li>Unquoted Numeric keys.</li>
   *                                <li>Unexpected commmas or brackets.</li>
   *                              </ul>
   */
  private void handleExpectingKey(char c) throws InvalidJsonException {
    if (c == '\"') {
      inString = true;
      expectingKey = false;
      expectingColon = true;
    } else if (c == '{') {
      stack.push(c);
      expectingKey = true;
    } else if (c == '[') {
      stack.push(c);
      insideArray = true;
      expectingCommaOrEnd = false;
    } else if (c == ']' || c == '}') {
      if (!stack.isEmpty() && ((c == ']' && stack.peek() == '[') || (c == '}'
              && stack.peek() == '{'))) {
        closeStructure(c);
        insideArray = c != ']' && insideArray;
        expectingCommaOrEnd = !stack.isEmpty();
      } else {
        throw new InvalidJsonException("Mismatched or unexpected closing character: " + c);
      }
    } else {
      throw new InvalidJsonException("Expected key enclosed in double quotes. Found: " + c);
    }
  }

  int getStackSize() {
    return stack.size();
  }

  boolean isExpectingKey() {
    return expectingKey;
  }

  boolean isInsideArray() {
    return insideArray;
  }

  boolean isCommaCheck() {
    return commaCheck;
  }
}
