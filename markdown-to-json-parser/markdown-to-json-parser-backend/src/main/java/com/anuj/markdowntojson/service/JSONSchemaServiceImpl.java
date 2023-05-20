package com.anuj.markdowntojson.service;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.exception.ApplicationException;
import com.anuj.markdowntojson.markdownparser.service.MarkdownParserServiceImpl;
import com.anuj.markdowntojson.schemagenerator.service.SchemaGeneratorService;

/**
 * This is the application service class.
 * This complete application has been divided into two parts.
 * Part1 - It reads the input markdown file and populates cache with lines for each data type and operation respectively.
 * This class makes call to:
 * 	1. MarkdownParser Service layer
 * 	2. SchemaGenerator Service layer
 * 
 * @author anujmehra
 *
 */
@Component("jsonSchemaService")
public class JSONSchemaServiceImpl {
	
	/**
	 * MarkdownParserService bean. This component reads the input markdown file.
	 */
	@Autowired
	private MarkdownParserServiceImpl markdownParserService;
	
	/**
	 * SchemaGeneratorService bean. This component generates the JSON schemas.
	 */
	@Autowired
	private SchemaGeneratorService schemaGeneratorService;
	
	
	/**
	 * Logger Object.
	 */
	private static final Logger LOG = Logger.getLogger(JSONSchemaServiceImpl.class);

	
	/**
	 * This method makes call to the MarkdownParserService and SchemaGeneratorService
	 * @param markdownFileFullyQualifiedName String --
	 * @param outputFolderLocation String
	 * 
	 */
	public void execute(String markdownFileFullyQualifiedName, String outputFolderLocation) throws ApplicationException{
		
		LOG.info("Starting to parse markdownFile: "+ markdownFileFullyQualifiedName);

		markdownParserService.parse(markdownFileFullyQualifiedName);
		
		LOG.info("Successfully parsed markdownFile: "+ markdownFileFullyQualifiedName);
		
		LOG.info("Starting to generate Schema's in location: "+ outputFolderLocation);

		schemaGeneratorService.generateJSONSchema(outputFolderLocation);
		
		LOG.info("Successfully generated Schema's in location: "+ outputFolderLocation);
	}//end of method execute.

	
}//end of class JSONSchemaServiceImpl
