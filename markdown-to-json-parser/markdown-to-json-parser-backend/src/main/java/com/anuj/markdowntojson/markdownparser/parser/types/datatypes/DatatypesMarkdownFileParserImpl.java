package com.anuj.markdowntojson.markdownparser.parser.types.datatypes;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.cache.datatypes.DatatypesCache;
import com.anuj.markdowntojson.exception.ParseException;
import com.anuj.markdowntojson.markdownparser.parser.dto.Operation;
import com.anuj.markdowntojson.markdownparser.parser.helper.CachePopulator;
import com.anuj.markdowntojson.markdownparser.parser.types.MarkdownFileParser;
import com.anuj.markdowntojson.util.ApplicationConstants;

/**
 * This class is the concrete implementation of 'MarkdownFileParser'.
 * This class is responsible to populate Application cache with all the datatypes that have been defined in the markdown file.
 * @author anujmehra
 *
 */
@Component("datatypesMarkdownFileParser")
public class DatatypesMarkdownFileParserImpl implements MarkdownFileParser{

	/**
	 * Object reference for CachePopulator
	 */
	@Autowired
	private CachePopulator cachePopulator;
	
	/**
	 * Object reference for DatatypesCache
	 */
	@Autowired
	private DatatypesCache datatypesCache;
	
	/**
	 * Logger object
	 */
	private static final Logger LOG = Logger.getLogger(DatatypesMarkdownFileParserImpl.class);

	/**
	 * This method accepts all the lines present in the markdown file and populates the Application cache with all the datatypes that have been defined in the markdown file.
	 * @param mdFileContents List<String>
	 * @throws ParseException 
	 */
	@Override
	public void performMarkdownParsing(List<String> mdFileContents) throws ParseException 
	{

		final List<Operation> operationsList = new ArrayList<Operation>();

		final int sizeOfDocument = mdFileContents.size();

		LOG.info("********************************************************* Parsing DataType's *********************************************************");

			for (int counter=0;counter<sizeOfDocument;counter++) 
			{
				if(mdFileContents.get(counter).startsWith("## DataType:"))
				{
					final Operation dataType = new Operation();
					try{
						final String[] data = mdFileContents.get(counter).split("\\(");
						
						dataType.setOperationDescription(data[0].replace("## DataType:", ""));
						
						dataType.setOperationName(ApplicationConstants.DATATYPE_KEYWORD + data[1].replace(")",""));
						
						dataType.setEndIndex(counter+1);

						boolean startIndexFlag = false;
						
						//Now traversing back the document and finding the operation URL and operation name.
						for(int i=counter;i<sizeOfDocument;i++)
						{
							if(mdFileContents.get(i).startsWith("|") && !startIndexFlag)
							{
								dataType.setStartIndex(i+1);
								startIndexFlag = true;
							}else if(startIndexFlag
									&& !mdFileContents.get(i).startsWith("|"))
							{
								dataType.setEndIndex(i);
								break;
							}
						}//end of for-loop

						LOG.info(" Parsed DataType...." + dataType);
						operationsList.add(dataType);
					} catch (Exception e) {
						// Bad practice to catch ALL exceptions
						throw new ParseException("Unable to perform Datatype parsing: "+dataType,e);
					}
				}//end of if-loop '"## DataType:"'

			}//end of for-loop

		//Populating the Cache
		cachePopulator.populateCache(mdFileContents, operationsList, datatypesCache.getDatatypeCacheMap());
		LOG.info("********************************************************* DataType's parsed ********************************************************* ");


	}//end of method generateDatatypes

}
