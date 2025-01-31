package validator;

import java.util.Stack;

import parser.InvalidJsonException;
import parser.JsonParser;

/**
 * Validator Class to check JSON syntax.
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

  private String status;

  /**
   * Constructs variables required for syntax checking.
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
   * Handle characters when inside a string context.
   *
   * @param c the input character
   * @throws InvalidJsonException if the string is invalid
   */
  private void handleStringContext(char c) throws InvalidJsonException {
    if (escapeNext) {
      escapeNext = false;
    } else if (c == '\\') {
      escapeNext = true;
    } else if (c == '"') {
      inString = false;
    }
  }

  /**
   * Handle characters in the general JSON context.
   *
   * @param c the input character
   * @throws InvalidJsonException if the input causes structural issues
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
    } else if (c == ',') {
      stack.push(c);
      expectingCommaOrEnd = false;
    } else if (c == '}' || c == ']') {
      if (!stack.isEmpty() && ((c == ']' && stack.peek() == '[') || (c == '}'
              && stack.peek() == '{'))) {
        closeStructure(c);
        expectingCommaOrEnd = !stack.isEmpty();
      } else {
        throw new InvalidJsonException("Mismatched closing character: " + c);
      }
    } else {
      throw new InvalidJsonException("Expected key enclosed in double quotes. Found: " + c);
    }
  }

  private void handleExpectingColon(char c) throws InvalidJsonException {
    if (c == ':') {
      expectingColon = false;
      expectingCommaOrEnd = true;
    } else {
      throw new InvalidJsonException("Expected ':' after key. Found: " + c);
    }
  }

  private void handleExpectingCommaOrEnd(char c) throws InvalidJsonException {
    if (c == ',') {
      expectingCommaOrEnd = false;
      if (stack.peek() == '{') {
        expectingKey = true;
      }
    } else if (c == ']') {
      closeStructure(c);
      expectingCommaOrEnd = !stack.isEmpty() && stack.peek() == '[';
      insideArray = false;
    } else if (c == '}') {
      closeStructure(c);
      expectingCommaOrEnd = !stack.isEmpty();
    } else if (c == '[') {
      stack.push(c);
      expectingCommaOrEnd = false;
    } else if (c == '\"') {
      inString = true;
      expectingCommaOrEnd = false;
    } else {
      throw new InvalidJsonException("Expected comma or end of closed string. Found: " + c);
    }
  }

  private void handleDefaultContext(char c) throws InvalidJsonException {
    switch (c) {
      case '{':
      case '[':
        stack.push(c);
        expectingKey = true;
        break;
      case '}':
      case ']':
        closeStructure(c);
        expectingKey = !stack.isEmpty();
        break;
      case ',':
        if (stack.isEmpty() || (stack.peek() != '{' && stack.peek() != '[')) {
          throw new InvalidJsonException("Unexpected comma outside of structure.");
        }
        expectingCommaOrEnd = false;
        break;
      case '\"':
        inString = true;
        break;
      default:
        throw new InvalidJsonException("Unexpected character: " + c);
    }
  }

  /**
   * Close a structure and validate the stack.
   *
   * @param c the closing character
   * @throws InvalidJsonException if the structure is mismatched
   */
  private void closeStructure(char c) throws InvalidJsonException {
    if (stack.isEmpty()) {
      throw new InvalidJsonException("Unexpected closing character: " + c);
    }
    char expected = stack.pop();
    if ((c == '}' && expected != '{') || (c == ']' && expected != '[')) {
      throw new InvalidJsonException("Mismatched or unexpected closing character: " + c);
    }
    expectingCommaOrEnd = !stack.isEmpty();
  }

  private boolean isEmptyStructure() {
    return stack.isEmpty() && !inString && !expectingKey && !expectingColon
            && !expectingCommaOrEnd && !insideArray;
  }

  private boolean checkIncompleteStructure() {
    return !stack.isEmpty() || inString || expectingKey || expectingColon
            || expectingCommaOrEnd || insideArray;
  }

  /**
   * Update the current status of the parser.
   */
  private void updateStatus() {
    if (isInvalid) {
      this.status = "Status:Invalid";
    } else if (isEmptyStructure()) {
      this.status = "Status:Valid";
    } else if (checkIncompleteStructure()) {
      this.status = "Status:Incomplete";
    } else {
      this.status = "Status:Empty";
    }
  }

  /**
   * Provide the output of the parser, given all the inputs it has been provided so far.
   *
   * @return the output of the parser
   */
  @Override
  public String output() {
    return status;
  }
}
