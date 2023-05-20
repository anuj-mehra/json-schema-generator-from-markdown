package com.anuj.markdowntojson.util;

/**
 * 
 * @author anujmehra
 *
 */
public enum HTTPMethods {

	GET("GET"),
	PUT ("PUT"),
	POST("POST"),
	DELETE("DELETE");
	
	/**
	 * 
	 */
	private String httpMethodType;

	/**
	 * 
	 */
	private HTTPMethods(String httpMethodType) {
		this.httpMethodType = httpMethodType;
	}
	
	/**
	 * 
	 */
	public String getHTTPMethodType() {
		return httpMethodType;
	}
}
