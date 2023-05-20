package com.anuj.markdowntojson.schemagenerator.manager;


import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.cache.datatypes.DatatypesCache;
import com.anuj.markdowntojson.cache.operations.OperationsCacheFactory;
import com.anuj.markdowntojson.exception.ApplicationException;
import com.anuj.markdowntojson.schemagenerator.manager.schema.SchemaGeneratorImpl;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.DatatypeGenerator;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache.ComplexDatatypeCache;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache.CompositeDatatypeCache;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache.JSONSchemaCache;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache.SimpleDatatypeCache;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache.TempJSONSchemaCache;
import com.anuj.markdowntojson.schemagenerator.manager.schema.exception.SchemaCreationException;
import com.anuj.markdowntojson.util.HTTPMethods;

/**
 * 
 * @author anujmehra
 *
 */
@Component("schemaGeneratorManager")
public class SchemaGeneratorManagerImpl {

	/**
	 * Logger object.
	 */
	private static final Logger LOG = Logger.getLogger(SchemaGeneratorManagerImpl.class);
	
	/**
	 * 
	 */
	@Autowired
	private DatatypeGenerator simpleDatatypeGenerator;

	/**
	 * 
	 */
	@Autowired
	private DatatypeGenerator complexDatatypeGenerator;
	
	/**
	 * 
	 */
	@Autowired
	private DatatypeGenerator compositeDatatypeGenerator;

	/**
	 * 
	 */
	@Autowired
	private DatatypesCache datatypesCache;
	
	/**
	 * 
	 */
	@Autowired
	private OperationsCacheFactory operationsCacheFactory;
	
	/**
	 * 
	 */
	@Autowired
	private SchemaGeneratorImpl schemaGenerator;
	
	/**
	 * 
	 */
	@Autowired
	private SimpleDatatypeCache simpleDatatypeCache;
	
	/**
	 * 
	 */
	@Autowired
	private ComplexDatatypeCache complexDatatypeCache;
	
	/**
	 * 
	 */
	@Autowired
	private CompositeDatatypeCache compositeDatatypeCache;
	
	/**
	 * 
	 */
	@Autowired
	private JSONSchemaCache jsonSchemaCache;
	
	/**
	 * 
	 */
	@Autowired
	private TempJSONSchemaCache tempJSONSchemaCache;
	
	/**
	 * 
	 * @param datatypeName String
	 * @throws ApplicationException
	 */
	public void generateSimpleDataTypes(String datatypeName) throws ApplicationException{

		List<String> lines = null;

		lines = datatypesCache.getDatatype(datatypeName);
		if(lines.size() == 1)
		{
			String currentLine = lines.get(0);

			currentLine = currentLine.replace("|", ",");

			final String[] lineData = currentLine.split(",");
			if(lineData[1].trim().equals("0"))
			{
				final String[] jsonDatatypeSchema = simpleDatatypeGenerator.getDatatype(lines,null);
				simpleDatatypeCache.updateSimpleDatatypeCache(jsonDatatypeSchema[0] ,jsonDatatypeSchema[1]);
				LOG.info("Putting datatype: "+datatypeName+" in Simple Cache");
			}
		}//end of if-block
		
	}

	/**
	 * 
	 * @param datatypeName String
	 * @throws ApplicationException
	 */
	public void generateComplexDataTypes(String datatypeName) throws ApplicationException {

		List<String> lines = null;
		
		lines = datatypesCache.getDatatype(datatypeName);
		
		if(lines.size() == 1)
		{
			String currentLine = lines.get(0);

			currentLine = currentLine.replace("|", ",");

			final String[] lineData = currentLine.split(",");
			
			if(lineData[1].trim().equals("1"))
			{
				final String[] jsonDatatypeSchema = complexDatatypeGenerator.getDatatype(lines,null);
				complexDatatypeCache.updateComplexDatatypeCache(jsonDatatypeSchema[0] ,jsonDatatypeSchema[1]);
				LOG.info("Putting datatype: "+datatypeName+" in Complex Cache");
			}
		}//end of if-block

	}
	
	
	/**
	 * 
	 * @param datatypeName String
	 * @throws ApplicationException
	 */
	public void generateCompositeDataTypes(String datatypeName) throws ApplicationException {

		List<String> lines = null;

		lines = datatypesCache.getDatatype(datatypeName);
		
		if(lines.size() > 1){
			final String key = datatypeName.substring(datatypeName.indexOf('_')+1);
			final String[] jsonDatatypeSchema = compositeDatatypeGenerator.getDatatype(lines, key);
			compositeDatatypeCache.updateCompositeDatatypeCache(jsonDatatypeSchema[0] ,jsonDatatypeSchema[1]);
			LOG.info("Putting datatype: "+datatypeName+" in Composite Cache");
		}

	}
	
	
	/**
	 * 
	 * @param outputDirectory String - Output directory in which the JSON schema files will be generated.
	 * @param resourceName String - Name of the resource for which the schema is to be generated.
	 * @param httpMethod HTTPMethodsEnum - Type of HTTP method for which the JSON schema is being generated.
	 * @throws ApplicationException
	 * 
	 */
	public void getJSONSchema(String outputDirectory, String resourceName,HTTPMethods httpMethod, Set<String> failedSchemas, boolean isRetry) throws ApplicationException
	{
		final List<String> resourceStringContentList = operationsCacheFactory.getOperationsCache(httpMethod).getOperations(resourceName);	
		
		String jsonSchema = null;
		
		try{
			jsonSchema = schemaGenerator.getJSONSchema(resourceStringContentList); 
			
			//Populate Cache of Schemas (Forest)
			if(null == httpMethod){
				tempJSONSchemaCache.updateJSONSchemaCache(toUpperCase(resourceName),jsonSchema.substring(1, jsonSchema.length()-1));
			}else{
				jsonSchemaCache.updateJSONSchemaCache(toUpperCase(resourceName),jsonSchema.substring(1, jsonSchema.length()-1));
				
				ExecutorService pool = null;
				Callable<Void> callable = null;
				
				try{
					pool = Executors.newFixedThreadPool(1);
					
					callable = new FileWriter(outputDirectory  + toUpperCase(resourceName) + ".json",jsonSchema);
					
					pool.submit(callable);
				}finally{
					pool.shutdown();
				}
			}
		
		}catch(SchemaCreationException e){
			if(!isRetry){
				failedSchemas.add(resourceName);	
			}else{
				LOG.error("JSON Schema not generated for Type:-" + httpMethod.toString() +  ". Operation:-" + toUpperCase(resourceName) + ". Reason:- " + e.getMessage());
			}
			
		}catch(Exception e){
			LOG.error("JSON Schema not generated for Type:-" + httpMethod.toString() +  ". Operation:-" + toUpperCase(resourceName) + ". Reason:- " + e.getMessage());
		}
		
		LOG.debug("--- Forest Cache Populated with : " + resourceName);
	}

	
	/**
	 * This util method converts the operation name to upper case.
	 * @param operationName String
	 * @return upperCaseValue String
	 */
	private String toUpperCase(String operationName) {
		return operationName.toUpperCase();
	}

	
	
}