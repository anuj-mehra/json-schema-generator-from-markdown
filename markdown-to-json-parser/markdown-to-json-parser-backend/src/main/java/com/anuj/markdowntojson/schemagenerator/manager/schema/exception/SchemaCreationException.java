package com.anuj.markdowntojson.schemagenerator.manager.schema.exception;

/**
 * 
 * @author anujmehra
 *
 */
public class SchemaCreationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4534553048920245397L;

	/**
	 * Instantiates a new application exception.
	 */
	public SchemaCreationException() {
		super();
	}

	/**
	 * Instantiates a new application exception.
	 *
	 * @param message the message
	 */
	public SchemaCreationException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new application exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public SchemaCreationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new application exception.
	 *
	 * @param cause the cause
	 */
	public SchemaCreationException(Throwable cause) {
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
	protected SchemaCreationException(String message, Throwable cause, boolean enableSuppression,boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
	
}
