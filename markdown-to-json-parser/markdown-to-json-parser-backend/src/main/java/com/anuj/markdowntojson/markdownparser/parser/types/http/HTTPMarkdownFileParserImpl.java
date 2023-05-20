package com.anuj.markdowntojson.markdownparser.parser.types.http;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.cache.operations.OperationsCacheFactory;
import com.anuj.markdowntojson.exception.ParseException;
import com.anuj.markdowntojson.markdownparser.parser.dto.Operation;
import com.anuj.markdowntojson.markdownparser.parser.helper.CachePopulator;
import com.anuj.markdowntojson.markdownparser.parser.helper.MarkdownFileParserUtil;
import com.anuj.markdowntojson.markdownparser.parser.types.MarkdownFileParser;
import com.anuj.markdowntojson.util.ApplicationConstants;
import com.anuj.markdowntojson.util.HTTPMethods;

/**
 * This class is the concrete implementation of 'MarkdownFileParser'.
 * This class is responsible to populate Application cache with all the GET/PUT/POST/DELETE operations that have been defined in the markdown file.
 * @author anujmehra
 *
 */
@Component("httpMarkdownFileParser")
public class HTTPMarkdownFileParserImpl implements MarkdownFileParser{

	/**
	 * Object reference for CachePopulator
	 */
	@Autowired
	private CachePopulator cachePopulator;

	/**
	 * Object reference for OperationsCache
	 */
	@Autowired
	private OperationsCacheFactory operationsCacheFactory;

	/**
	 * Object reference for MarkdownFileParserUtil
	 */
	@Autowired
	private MarkdownFileParserUtil markdownFileParserUtil;

	/**
	 * 
	 */
	private static final Logger LOG = Logger.getLogger(HTTPMarkdownFileParserImpl.class);

	/**
	 * This method accepts all the lines present in the markdown file and populates the Application cache with all the GET operations that have been defined in the markdown file.
	 * @param lines List<String>
	 * @throws ParseException 
	 */
	@Override
	public void performMarkdownParsing(List<String> lines) throws ParseException
	{

		final List<Operation> tempOperationsList = new ArrayList<Operation>();
		final List<Operation> getOperationsList = new ArrayList<Operation>();
		final List<Operation> putOperationsList = new ArrayList<Operation>();
		final List<Operation> postOperationsList = new ArrayList<Operation>();
		final List<Operation> deleteOperationsList = new ArrayList<Operation>();


		final int sizeOfDocument = lines.size();

		LOG.info("*********************************************************  Parsing resources *********************************************************");

		for (int counter=0;counter<sizeOfDocument;counter++) 
		{
			if(lines.get(counter).startsWith(ApplicationConstants.CONST_OPERATION_START) )
			{
				for(int i=counter + 1;i<sizeOfDocument;i++)
				{
					//If next operation start
					if(lines.get(i).startsWith(ApplicationConstants.CONST_OPERATION_START)){
						break;
					}

					if(lines.get(i).startsWith(ApplicationConstants.HTTP_METHOD_START) && lines.get(i).contains(" [GET]"))
						//|| lines.get(counter).contains("### Add/Change note and mark single card transaction [PUT]"))
					{
						//markdownFileParserUtil.populateResource(lines, i, getOperationsList, HTTPMethods.GET);
						
						markdownFileParserUtil.populateResource(lines, i, sizeOfDocument , HTTPMethods.GET, ApplicationConstants.GET_SCHEMA_SPEC_START_POINT, getOperationsList,tempOperationsList);
						
						//break;
					}else if(lines.get(i).startsWith(ApplicationConstants.HTTP_METHOD_START) && lines.get(i).contains(" [PUT]"))
					{
						markdownFileParserUtil.populateResource(lines, i, sizeOfDocument , HTTPMethods.PUT, ApplicationConstants.PUT_SCHEMA_SPEC_START_POINT , putOperationsList,tempOperationsList);


						//break;
					}else if(lines.get(i).startsWith(ApplicationConstants.HTTP_METHOD_START) && lines.get(i).contains(" [POST]"))
					{

						markdownFileParserUtil.populateResource(lines, i, sizeOfDocument , HTTPMethods.POST, ApplicationConstants.POST_SCHEMA_SPEC_START_POINT , postOperationsList,tempOperationsList);

						//break;
					}else if(lines.get(i).startsWith(ApplicationConstants.HTTP_METHOD_START) && lines.get(i).contains(" [DELETE]"))
					{
						markdownFileParserUtil.populateResource(lines, i, sizeOfDocument , HTTPMethods.DELETE, ApplicationConstants.DELETE_SCHEMA_SPEC_START_POINT , deleteOperationsList,tempOperationsList);
						
						//break;
					}
				
				}

			}


		}
		
		/*for(final Operation operation : tempOperationsList){
			if(operation.equals()){
				tempOperationsList.remove()
			}
		}*/

		//Populating the Cache
		cachePopulator.populateCache(lines, getOperationsList, operationsCacheFactory.getOperationsCache(HTTPMethods.GET).getOperationsCacheMap());
		cachePopulator.populateCache(lines, putOperationsList, operationsCacheFactory.getOperationsCache(HTTPMethods.PUT).getOperationsCacheMap());
		cachePopulator.populateCache(lines, postOperationsList, operationsCacheFactory.getOperationsCache(HTTPMethods.POST).getOperationsCacheMap());
		cachePopulator.populateCache(lines, deleteOperationsList, operationsCacheFactory.getOperationsCache(HTTPMethods.DELETE).getOperationsCacheMap());
		cachePopulator.populateCache(lines, tempOperationsList, operationsCacheFactory.getOperationsCache(null).getOperationsCacheMap());
		
		LOG.info("********************************************************* Resources parsed *********************************************************");

		System.out.println("----------------" + getOperationsList.size());
		System.out.println("----------------" + putOperationsList.size());
		System.out.println("----------------" + postOperationsList.size());
		System.out.println("----------------" + deleteOperationsList.size());
		System.out.println("----------------" + tempOperationsList.size());
	}//end of method execute




}
