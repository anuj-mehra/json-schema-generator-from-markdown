package com.anuj.markdowntojson.schemagenerator.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.anuj.markdowntojson.cache.datatypes.DatatypesCache;
import com.anuj.markdowntojson.cache.operations.OperationsCacheFactory;
import com.anuj.markdowntojson.exception.ApplicationException;
import com.anuj.markdowntojson.util.HTTPMethods;

/**
 * This is the 'SchemaGenerator' layer service class.
 * It is an abstract class and implements Template pattern.
 * @author anujmehra
 *
 */
public abstract class SchemaGeneratorService {

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
	 * @param dataTypes
	 */
	public abstract void generateDatatypes(Set<String> dataTypes) throws ApplicationException;

	/**
	 * 
	 * @param directory
	 * @param resources
	 * @throws ApplicationException
	 */
	public abstract Set<String> generateOperationsSchema(String directory, Set<String> resources, HTTPMethods httpMethod, boolean isRetry) throws ApplicationException;

	/**
	 * This method creates the GET/POST/DELETE/PUT folders inside the /schema folder.
	 */
	public abstract void createOutputFolders(String schemaFolderLocation) throws ApplicationException;
	
	/**
	 * 
	 * @param directory
	 * @throws ApplicationException
	 */
	public void generateJSONSchema(String directory) throws ApplicationException{

		this.createOutputFolders(directory);
		
		this.generateDatatypes(datatypesCache.getDatatypeNames());

		this.generateOperationsSchema(directory,operationsCacheFactory.getOperationsCache(null).getOperationsNames(), null, Boolean.FALSE);
		
		final Set<String> getFailedSchemas = this.generateOperationsSchema(directory,operationsCacheFactory.getOperationsCache(HTTPMethods.GET).getOperationsNames(), HTTPMethods.GET, Boolean.FALSE);

		final Set<String> putFailedSchemas = this.generateOperationsSchema(directory,operationsCacheFactory.getOperationsCache(HTTPMethods.PUT).getOperationsNames(), HTTPMethods.PUT, Boolean.FALSE);

		final Set<String> postFailedSchemas = this.generateOperationsSchema(directory,operationsCacheFactory.getOperationsCache(HTTPMethods.POST).getOperationsNames(), HTTPMethods.POST, Boolean.FALSE);

		final Set<String> deleteFailedSchemas = this.generateOperationsSchema(directory,operationsCacheFactory.getOperationsCache(HTTPMethods.DELETE).getOperationsNames(), HTTPMethods.DELETE, Boolean.FALSE);

		//Retry to generate Failed Schemas.
		this.retryFailedSchemas(directory,getFailedSchemas, HTTPMethods.GET);

		this.retryFailedSchemas(directory,putFailedSchemas, HTTPMethods.PUT);

		this.retryFailedSchemas(directory,postFailedSchemas, HTTPMethods.POST);

		this.retryFailedSchemas(directory,deleteFailedSchemas, HTTPMethods.DELETE);

	}

	/**
	 * 
	 * @param directory
	 * @param resources
	 * @param httpMethod
	 * @throws ApplicationException
	 */
	private void retryFailedSchemas(String directory, Set<String> resources, HTTPMethods httpMethod) throws ApplicationException{
		
		if(resources.size() >0){
			this.generateOperationsSchema(directory,resources, httpMethod, Boolean.TRUE);
		}
	}


}//end of abstract class SchemaGeneratorService
