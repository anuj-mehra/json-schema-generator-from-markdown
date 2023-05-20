package com.anuj.markdowntojson;


import org.apache.log4j.Logger;

import com.anuj.markdowntojson.exception.ApplicationException;
import com.anuj.markdowntojson.service.JSONSchemaServiceImpl;

/**
 * This is the Application java class. This class is called from the batch/shell script.
 * This class has following responsibilities:
 * 		1. Load spring configuration<version>${spring.version}</version>
 * 		2. Call application's service layer
 * 
 * @author anujmehra
 *
 */
public final class Application {

	/**
	 * Logger Object.
	 */
	private static final Logger LOG = Logger.getLogger(Application.class);

	/**
	 * Made this utility class.
	 */
	private Application(){
		
	}
	
	/**
	 * This is the main() method for the application.
	 * Execution starts from this method. 
	 * @param args
	 */
	public static void main(String... args){
		
		//Load spring beans
		final JSONSchemaServiceImpl service = ServiceRun.getExecutableService();	
		
		//Generate JSON Schema
		try {
			
			String inputFile = "/Users/anujmehra/git/json-schema-generator-from-markdown/markdown-to-json-parser/markdown-to-json-parser-backend/src/main/resources/input/input-markdown-file.md";
			String outputFolderLocation = "/Users/anujmehra/git/json-schema-generator-from-markdown/markdown-to-json-parser/markdown-to-json-parser-backend/src/main/resources/output";
			
			final String filePath = inputFile.replace("\\", "/");
			final String outputPath = outputFolderLocation.replace("\\", "/");
			
			service.execute(filePath, outputPath);
		} catch (ApplicationException applicationException) {
			LOG.error("Not able to complete successfully due to following Errors....",applicationException);
		}	
	}//end of method main().
	

}//end of class Application
