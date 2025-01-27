package validator;

import java.util.Stack;

import parser.InvalidJsonException;
import parser.JsonParser;

public class JsonValidator implements JsonParser<String> {

  private Stack<Character> stack;

  // Flags for context
  private boolean inString;
  private boolean escapeNext;
  private boolean isInvalid;

  // States to track expected elements
  private boolean expectingKey;
  private boolean expectingColon;
  private boolean expectingCommaOrEnd;

  // Tracks the current status
  private String status;

  // Constructor to initialize state
  public JsonValidator() {
    this.stack = new Stack<>();
    this.inString = false;
    this.escapeNext = false;
    this.isInvalid = false;
    this.expectingKey = true;
    this.expectingColon = false;
    this.expectingCommaOrEnd = false;
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
      if (c == '{') {
        stack.push(c);
        expectingKey = true;
      } else if (c == '"') {
        inString = true;
        expectingKey = false;
        expectingColon = true;
      } else {
        throw new InvalidJsonException("Expected key enclosed in double quotes. Found: " + c);
      }
    } else if (expectingColon) {
      if (c == ':') {
        expectingColon = false;
        expectingCommaOrEnd = true;
      } else {
        throw new InvalidJsonException("Expected ':' after key. Found: " + c);
      }
    } else if (expectingCommaOrEnd) {
      if (c == ',') {
        expectingCommaOrEnd = false;
        expectingKey = stack.peek() == '{';
      } else if (c == '}' || c == ']') {
        closeStructure(c);
      } else if (c == '[') {
        stack.push(c);
        expectingCommaOrEnd = false;
      } else if (c == '\"') {
        inString = true;
        expectingCommaOrEnd = false;
      } else {
        throw new InvalidJsonException("Expected ',' or closing brace/bracket. Found: " + c);
      }
    } else {
      switch (c) {
        case '{':
        case '[':
          stack.push(c);
          expectingKey = c == '{';
          break;
        case '}':
        case ']':
          closeStructure(c);
          break;
        case '"':
          inString = true;
          break;
        default:
          throw new InvalidJsonException("Unexpected character: " + c);
      }
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

  /**
   * Update the current status of the parser.
   */
  private void updateStatus() {
    if (isInvalid) {
      this.status = "Status:Invalid";
    } else if (stack.isEmpty() && !inString && !expectingKey && !expectingColon) {
      this.status = "Status:Valid";
    } else if (stack.isEmpty() && !inString) {
      this.status = "Status:Incomplete";
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
