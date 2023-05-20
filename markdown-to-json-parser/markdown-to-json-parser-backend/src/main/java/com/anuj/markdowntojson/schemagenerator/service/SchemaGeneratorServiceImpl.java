package com.anuj.markdowntojson.schemagenerator.service;


import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.exception.ApplicationException;
import com.anuj.markdowntojson.schemagenerator.manager.SchemaGeneratorManager;
import com.anuj.markdowntojson.util.ApplicationConstants;
import com.anuj.markdowntojson.util.HTTPMethods;

/**
 * 
 * @author anujmehra
 *
 */
@Component("schemaGeneratorService")
public class SchemaGeneratorServiceImpl extends SchemaGeneratorService{

	/**
	 * Reference object for SchemaGeneratorManager
	 */
	@Autowired
	private SchemaGeneratorManager schemaGeneratorManager;

	/**
	 * Logger Object.
	 */
	private static final Logger LOG = Logger.getLogger(SchemaGeneratorServiceImpl.class);

	/**
	 * This method creates the GET/POST/DELETE/PUT folders inside the /schema folder.
	 */
	@Override
	public void createOutputFolders(String schemaFolderLocation) throws ApplicationException{

		LOG.info("********************************************************* Creating output folders *********************************************************");

		final String filePath = new File("").getAbsolutePath().replace("\\", "/").concat(schemaFolderLocation);

		for(final HTTPMethods httpType : HTTPMethods.values())
		{

			final Path dir = Paths.get(filePath + httpType.toString());

			try{
				if(Files.exists(dir)){
					//Delete all files present inside this folder.

					try {
						Files.walkFileTree(dir, new SimpleFileVisitor<Path>(){
							@Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
								if(!attrs.isDirectory()){
									Files.delete(file);
								}
								return FileVisitResult.CONTINUE;
							}
						});
					} catch (IOException e) {
						LOG.error(" * IOException Occured : SchemaGeneratorServiceImpl : createOutputFolders :: ",e);
						throw new ApplicationException(" ** IOException Occured : SchemaGeneratorServiceImpl : createOutputFolders :: ",e);
					}
					///////////////////////
					Files.delete(dir);
					Files.createDirectories(dir);
				}else{
					Files.createDirectories(dir);
				}
			}catch(IOException e){
				LOG.error(" ** IOException Occured : SchemaGeneratorServiceImpl : createOutputFolders :: ",e);
				throw new ApplicationException(" ** IOException Occured : SchemaGeneratorServiceImpl : createOutputFolders :: ",e);
			}

		}

		LOG.info("********************************************************* Output folders created *********************************************************");
	}

	/**
	 * This method reads all the Schema operations - Output of Markdown parsing.
	 * Then it uses each schema operation one by one, to generate corresponding JSON datatypes, that will be referred in other schemas.
	 * 
	 * @param dataTypes Set<String> - List of datatypes present in the markdown file.
	 * @throws ApplicationException
	 */
	@Override
	public void generateDatatypes(Set<String> dataTypes) throws ApplicationException{

		LOG.info("********************************************************* Populating datatype cache *********************************************************");

		//First need to generate the datatypes.
		for(final String dataType : dataTypes)
		{
			if(dataType.contains(ApplicationConstants.DATATYPE_KEYWORD))
			{
				schemaGeneratorManager.generateSimpleDataTypes(dataType);
			}
		}

		for(final String dataType : dataTypes)
		{
			if(dataType.contains(ApplicationConstants.DATATYPE_KEYWORD))
			{
				schemaGeneratorManager.generateComplexDataTypes(dataType);
			}
		}

		for(final String dataType : dataTypes)
		{
			if(dataType.contains(ApplicationConstants.DATATYPE_KEYWORD))
			{
				schemaGeneratorManager.generateCompositeDataTypes(dataType);
			}
		}

		LOG.info("********************************************************* Cache Populated *********************************************************");

	}//end of method generateDatatypes


	/**
	 * This method reads all the Schema operations - Output of Markdown parsing.
	 * Then it uses each operation one by one, to generate corresponding JSON Schema.
	 * 
	 * @param outputDirectory String - Directory path on file system in which the generated JSON schema files will be placed.
	 * @param resources Set<String> - Name of the resources for which the schema is to be generated.
	 * @param httpMethod HTTPMethodsEnum - Type of HTTP method for which the JSON schema is being generated
	 * @throws ApplicationException
	 */
	@Override
	public Set<String> generateOperationsSchema(String outputDirectory, Set<String> resources, HTTPMethods httpMethod, boolean isRetry) throws ApplicationException{

		LOG.info("********************************************************* Generating Schema's *********************************************************");

		final Set<String> failedSchemas = new HashSet<String>();

		//This is being done to create forest.
		for(final String resource : resources)
		{
			if(!resource.startsWith(ApplicationConstants.DATATYPE_KEYWORD))
			{
				LOG.info("Generating Schema for Resource: "+resource);

				if(null != httpMethod){
					schemaGeneratorManager.getJSONSchema(outputDirectory + "/" + httpMethod.toString() +"/",resource, httpMethod, failedSchemas, isRetry);
				}else{
					schemaGeneratorManager.getJSONSchema(outputDirectory + "/" ,resource, httpMethod, failedSchemas, isRetry);
				}


			}
		}
		LOG.info("********************************************************* Schema's Generated *********************************************************");

		return failedSchemas;
	}//end of method generateOperationsSchema


}//end of class SchemaGeneratorServiceImpl
