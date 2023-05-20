package com.anuj.markdowntojson.schemagenerator.manager;

import java.util.Set;

import com.anuj.markdowntojson.exception.ApplicationException;
import com.anuj.markdowntojson.util.HTTPMethods;

/**
 * 
 * @author anujmehra
 *
 */
public interface SchemaGeneratorManager {

	/**
	 * 
	 * @param datatypeName String
	 */
	void generateSimpleDataTypes(String datatypeName) throws ApplicationException;

	/**
	 * 
	 * @param datatypeName String
	 */
	void generateComplexDataTypes(String datatypeName) throws ApplicationException;

	/**
	 * 
	 * @param datatypeName String
	 */
	void generateCompositeDataTypes(String datatypeName) throws ApplicationException;

	/**
	 * 
	 * @param outputDirectory String
	 * @param operationName String
	 * @throws ApplicationException
	 */
	void getJSONSchema(String outputDirectory, String operationName,HTTPMethods httpMethod, Set<String> failedSchemas, boolean isRetry) throws ApplicationException;

}
