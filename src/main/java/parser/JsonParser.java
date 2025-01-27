package parser;

/**
 * This interface represents a simple JSON parser that accepts input one
 * character at a time.
 */
public interface JsonParser<T> {
  /**
   * Accept a single character as input, and return the new parser as a result
   * of handling this character.
   *
   * @param c the input character
   * @return the parser after handling the provided character
   * @throws InvalidJsonException if the input causes the JSON to be invalid
   */
  JsonParser input(char c) throws InvalidJsonException;

  /**
   * Provide the output of the parser, given all the inputs it has been provided
   * so far. The content and format of this output is defined by individual
   * implementations.
   *
   * @return the output of the parser
   */
  T output();
}
