package validator;

import parser.InvalidJsonException;
import parser.JsonParser;

/**
 * A JSON validator that implements the JsonParser interface.
 * This validator enforces the following rules:
 * 1. Keys must start with a letter and contain only letters and numbers
 * 2. Empty objects and arrays are not allowed
 * 3. All values must be strings (in quotes)
 * 4. The JSON must start with { and be properly nested
 */
public class JsonValidator implements JsonParser<String> {

  /**
   * Manages the state of our JSON parsing. Think of this like a control panel
   * that shows us exactly where we are in the parsing process.
   */
  private class JsonParserState {
    // Basic parsing state
    boolean hasStarted = false;     // Have we seen the opening {?
    boolean inString = false;       // Are we between quotes?
    boolean escapeNext = false;     // Is the next character escaped with \?
    boolean isInvalid = false;      // Has the input become invalid?

    // Context tracking
    boolean expectingKey = false;    // Should we see a key next?
    boolean expectingColon = false;  // Should we see a colon next?
    boolean expectingValue = false;  // Should we see a value next?
    boolean expectingCommaOrEnd = false;  // Should we see a comma or closing bracket?
    boolean isKeyString = false;     // Are we processing a key right now?
    boolean hasSeenValue = false;    // Have we seen any value in current context?

    String status = "Status:Empty";  // Current parser status
    StringBuilder currentToken = new StringBuilder();  // Builds current string token
  }

  /**
   * Manages the structure of nested objects and arrays.
   * This helps us track proper nesting and matching of brackets.
   */
  private class JsonStructureHandler {
    private int nestingLevel = 0;
    private char[] structureTypes = new char[100];  // Tracks { or [ at each level

    private void handleOpeningBracket(char c) throws InvalidJsonException {
      if (nestingLevel >= 100) {
        throw new InvalidJsonException("Maximum nesting level exceeded");
      }
      structureTypes[nestingLevel++] = c;
    }

    private void handleClosingBracket(char c) throws InvalidJsonException {
      if (nestingLevel == 0) {
        throwError("Unexpected closing bracket: " + c);
      }

      char expected = structureTypes[nestingLevel - 1] == '{' ? '}' : ']';
      if (c != expected) {
        throwError("Mismatched closing character: " + c + " (expected " + expected + ")");
      }
      nestingLevel--;
    }

    private boolean isInObjectContext() {
      return nestingLevel > 0 && structureTypes[nestingLevel - 1] == '{';
    }

    private boolean isComplete() {
      return nestingLevel == 0;
    }
  }

  private final JsonParserState state;
  private final JsonStructureHandler structureHandler;

  public JsonValidator() {
    this.state = new JsonParserState();
    this.structureHandler = new JsonStructureHandler();
  }

  @Override
  public JsonParser<String> input(char c) throws InvalidJsonException {
    // Once invalid, stay invalid - this handles test requirement for state retention
    if (state.isInvalid) {
      return this;
    }

    try {
      // Handle characters differently based on whether we're in a string
      if (state.inString) {
        handleStringContext(c);
      } else {
        handleGeneralContext(c);
      }
      updateStatus();
    } catch (InvalidJsonException e) {
      // If any error occurs, mark as invalid and propagate the error
      state.isInvalid = true;
      state.status = "Status:Invalid";
      throw e;
    }

    return this;
  }

  /**
   * Handles characters outside of string contexts. This method manages the structure
   * of our JSON, handling brackets, commas, colons, and the start of strings.
   * Think of it like a traffic controller, directing each character to its proper place.
   */
  private void handleGeneralContext(char c) throws InvalidJsonException {
    // First, skip any whitespace - it doesn't affect our parsing
    if (Character.isWhitespace(c)) {
      return;
    }

    // If we haven't started yet, we must see an opening brace
    if (!state.hasStarted) {
      if (c != '{') {
        throwError("JSON must start with {");
      }
      state.hasStarted = true;
      structureHandler.handleOpeningBracket(c);
      state.expectingKey = true;
      return;
    }

    // Handle each possible character based on our current state
    switch (c) {
      case '{':
      case '[':
        handleOpenBracket(c);
        break;

      case '}':
      case ']':
        handleCloseBracket(c);
        break;

      case '"':
        handleQuoteStart();
        break;

      case ':':
        handleColon();
        break;

      case ',':
        handleComma();
        break;

      default:
        // Any other character is unexpected
        throwError("Unexpected character: " + c);
    }
  }

  /**
   * Handles the start of a new object or array.
   * This ensures we're in a valid state to start a new structure.
   */
  private void handleOpenBracket(char c) throws InvalidJsonException {
    if (state.expectingCommaOrEnd) {
      throwError("Expected comma or closing bracket");
    }
    structureHandler.handleOpeningBracket(c);
    state.expectingKey = (c == '{');
    state.hasSeenValue = false;
  }

  /**
   * Handles the end of an object or array.
   * This ensures the structure isn't empty and matches properly.
   */
  private void handleCloseBracket(char c) throws InvalidJsonException {
    if (!state.hasSeenValue) {
      throwError("Empty " + (c == '}' ? "objects" : "arrays") + " are not allowed");
    }
    structureHandler.handleClosingBracket(c);
    state.expectingCommaOrEnd = true;
  }

  /**
   * Handles the start of a string.
   * This ensures we're in a valid state to start a string and sets up string processing.
   */
  private void handleQuoteStart() throws InvalidJsonException {
    if (state.expectingCommaOrEnd) {
      throwError("Missing comma between key-value pairs");
    }
    state.inString = true;
    state.currentToken.setLength(0);
    state.isKeyString = state.expectingKey;
    state.expectingKey = false;
  }

  /**
   * Handles a colon between key and value.
   * This ensures we're in a valid state for a colon.
   */
  private void handleColon() throws InvalidJsonException {
    if (!state.expectingColon) {
      throwError("Unexpected colon");
    }
    state.expectingColon = false;
    state.expectingValue = true;
  }

  /**
   * Handles commas between elements.
   * This ensures commas appear in valid locations and updates state appropriately.
   */
  private void handleComma() throws InvalidJsonException {
    if (!state.expectingCommaOrEnd) {
      throwError("Unexpected comma");
    }
    state.expectingCommaOrEnd = false;
    state.expectingKey = structureHandler.isInObjectContext();
    state.expectingValue = !state.expectingKey;
  }

  @Override
  public String output() {
    return state.status;
  }

  /**
   * Updates the parser status based on current state.
   * This method is crucial for the tests that verify status transitions.
   */
  private void updateStatus() {
    if (state.isInvalid) {
      state.status = "Status:Invalid";
    } else if (!state.hasStarted) {
      state.status = "Status:Empty";
    } else if (structureHandler.isComplete() &&
            !state.inString &&
            state.hasSeenValue) {
      state.status = "Status:Valid";
    } else {
      state.status = "Status:Incomplete";
    }
  }

  /**
   * Helper method to throw errors that automatically set invalid state.
   */
  private void throwError(String message) throws InvalidJsonException {
    state.isInvalid = true;
    throw new InvalidJsonException(message);
  }

  /**
   * Handles characters that appear within string contexts (between quotes).
   * This handles both key strings and value strings.
   */
  private void handleStringContext(char c) throws InvalidJsonException {
    if (state.escapeNext) {
      // Handle escaped characters
      if ("\\\"bfnrt".indexOf(c) == -1) {
        throwError("Invalid escape sequence: \\" + c);
      }
      state.escapeNext = false;
      state.currentToken.append(c);
      return;
    }

    if (c == '\\') {
      state.escapeNext = true;
      return;
    }

    if (c == '"') {
      // End of string - handle differently for keys and values
      String content = state.currentToken.toString();
      state.inString = false;

      if (state.isKeyString) {
        validateKey(content);
        state.isKeyString = false;
        state.expectingColon = true;
      } else {
        state.hasSeenValue = true;
        state.expectingCommaOrEnd = true;
      }
      state.currentToken.setLength(0);
      return;
    }

    // Check for invalid control characters
    if (c < 0x20) {
      throwError("Invalid control character in string");
    }
    state.currentToken.append(c);
  }

  /**
   * Validates key format according to requirements.
   * Keys must start with a letter and contain only letters and numbers.
   */
  private void validateKey(String key) throws InvalidJsonException {
    if (key.isEmpty()) {
      throwError("Empty key is not allowed");
    }
    if (!Character.isLetter(key.charAt(0))) {
      throwError("Key must start with a letter: " + key);
    }
    for (int i = 1; i < key.length(); i++) {
      if (!Character.isLetterOrDigit(key.charAt(i))) {
        throwError("Key can only contain letters and numbers");
      }
    }
  }

}