package com.anuj.markdowntojson.exception;

/**
 * The Class ApplicationException.
 *
 * @author anujmehra
 */
public class ParseException extends ApplicationException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5392713802182772995L;

	/**
	 * Instantiates a new application exception.
	 */
	public ParseException() {
		super();
	}

	/**
	 * Instantiates a new application exception.
	 *
	 * @param message the message
	 */
	public ParseException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new application exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public ParseException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new application exception.
	 *
	 * @param cause the cause
	 */
	public ParseException(Throwable cause) {
		super(cause);
	}

	/**
	 * Instantiates a new application exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 * @param enableSuppression the enable suppression
	 * @param writableStackTrace the writable stack trace
	 */
	protected ParseException(String message, Throwable cause, boolean enableSuppression,boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
