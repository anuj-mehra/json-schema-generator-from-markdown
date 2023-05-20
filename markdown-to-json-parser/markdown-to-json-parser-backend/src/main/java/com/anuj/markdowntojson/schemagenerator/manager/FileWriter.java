package com.anuj.markdowntojson.schemagenerator.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.anuj.markdowntojson.exception.ApplicationException;

/**
 * 
 * @author anujmehra
 *
 */
public class FileWriter implements Callable<Void>{

	/**
	 * Logger object.
	 */
	private static final Logger logger = Logger.getLogger(FileWriter.class);
	
	/**
	 * 
	 */
	private final String jsonFileName;

	/**
	 * 
	 */
	private final String jsonSchema;

	/**
	 * 
	 * @param jsonSchema
	 * @param jsonFileName
	 */
	public FileWriter(String jsonSchema, String jsonFileName){
		this.jsonFileName = jsonFileName;
		this.jsonSchema = jsonSchema;
	}


	/**
	 * 
	 */
	@Override
	public Void call() throws ApplicationException {
		this.publishJSONSchema(jsonSchema, jsonFileName);
		return null;
	}
	
	
	/**
	 * 
	 * @param jsonFileName
	 * @param jsonSchema
	 * @throws ApplicationException
	 */
	private void publishJSONSchema(String jsonFileName, String  jsonSchema) throws ApplicationException{

		try {
			Files.write(Paths.get(jsonFileName), jsonSchema.getBytes());
		} catch (IOException e) {
			logger.error("IOException Occured : FileWriter : publishJSONSchema :: ", e);
			throw new ApplicationException("IOException Occured : FileWriter : publishJSONSchema :: ", e);
		} 
	}


}
