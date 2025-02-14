package parser;

/**
 * This class represents a checked exception. This is used by parser.JsonParser
 * implementations to signal invalid JSON.
 */

public class InvalidJsonException extends Exception {
  public InvalidJsonException(String message) {
    super(message);
  }
}
