package com.anuj.markdowntojson.markdownparser.parser.helper;

/**
 * 
 */
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.markdownparser.parser.dto.Operation;

/**
 * 
 * @author anujmehra
 *
 */
@Component("cachePopulator")
public class CachePopulator {

	/**
	 * 
	 */
	@Autowired
	private MarkdownFileParserUtil markdownFileParserUtil;
	
	/**
	 * 
	 * @param lines
	 * @param operationsList
	 * @param typeCache
	 */
	public void populateCache(List<String> lines, List<Operation> operationsList, Map<String, List<String>> typeCache){

		for(final Operation operation :operationsList)
		{
			for(int i=operation.getStartIndex(); i< operation.getEndIndex(); i++)
			{
				
				if(operation.getSchemaRows().size() >0 
						&& lines.get(i).trim().length() == 0){
						break;
				}
				
				if(lines.get(i).startsWith("|") && !lines.get(i).contains("~~"))
				{
					final String currentLineline = lines.get(i).replace(" ", "");
					final String data[] = currentLineline.split("|");
	
					if(markdownFileParserUtil.validateNumber(data[2]))
					{
						operation.getSchemaRows().add(lines.get(i));
					}
					
				}

			}
		
			typeCache.put(operation.getOperationName().replace(" ", "-"), operation.getSchemaRows());
			
		}//end of for-loop 'Operation operation :operationsList'

	}//end of method 'populateDatatypeCache'


}
