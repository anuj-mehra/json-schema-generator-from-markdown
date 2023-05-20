package com.anuj.markdowntojson.markdownparser.parser.helper;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.markdownparser.parser.dto.Operation;
import com.anuj.markdowntojson.util.ApplicationConstants;
import com.anuj.markdowntojson.util.HTTPMethods;

/**
 * This is the helper class used during MarkdownFileParsing. 
 * @author anujmehra
 *
 */
@Component("markdownFileParserUtil")
public class MarkdownFileParserUtil {

	/**
	 * Logger object
	 */
	private static final Logger LOG = Logger.getLogger(MarkdownFileParserUtil.class);

	/**
	 * 
	 * @param lines List<String>
	 * @param counter Integer
	 * @param operationsList List<Operation>
	 * @param httpMethod HTTPMethods
	 */
	public void populateResource(List<String> lines, Integer counter, List<Operation> operationsList,  HTTPMethods httpMethod){

		final Operation resource = new Operation();

		try {
			//Setting operation description
			resource.setOperationDescription(lines.get(counter).replace("#", ""));

			resource.setEndIndex(counter+1);

			//Now traversing back the document for finding the operation URL, operation name and start index.
			for(int i=counter-1;i>=0;i--)
			{
				if(lines.get(i).startsWith(ApplicationConstants.CONST_OPERATION_START) )
				{
					resource.setOperationName(lines.get(i).substring(lines.get(i).indexOf(ApplicationConstants.CONST_OPERATION_START) + 3, lines.get(i).indexOf(" [")));
					resource.setResourceURI(lines.get(i).substring(lines.get(i).indexOf(" [") + 2, lines.get(i).indexOf("]")));
					resource.setStartIndex(i+1);
					break;
				}
			}
			operationsList.add(resource);
			LOG.info("Parsed [" + httpMethod +"] resource: " + resource);
		} catch (Exception e) {
			// Bad practice to catch ALL exception
			LOG.error("Unable to perform Resource parsing: "+resource,e);				
		}
	

	}


	/**
	 * 
	 * @param lines List<String>
	 * @param counter Integer
	 * @param sizeOfDocument Integer
	 * @param httpMethod HTTPMethods
	 * @param methodDescription String
	 * @param operationsList List<Operation> - This list contains the HTTP method specific operation list for which the corresponding JSON script will be generated.
	 * @param tempOperationsList  List<Operation> - This list will contain those schemas that have been referred by another schema, 
	 * 												but there is no JSON file for this schema that needs to be generated.
	 */
	public void populateResource(List<String> lines, Integer counter, Integer sizeOfDocument,  HTTPMethods httpMethod
								, String methodDescription, List<Operation> operationsList, List<Operation> tempOperationsList){
	
		final Operation resource = new Operation();

		try {
			//Setting operation description
			resource.setOperationDescription(lines.get(counter).replace("#", ""));

			//Now traversing back the document for finding the operation URL and operation name.
			for(int i=counter-1;i>0;i--)
			{
				if(lines.get(i).startsWith(ApplicationConstants.CONST_OPERATION_START) )
				{
					resource.setOperationName(lines.get(i).substring(lines.get(i).indexOf(ApplicationConstants.CONST_OPERATION_START) + 3, lines.get(i).indexOf(" [")));
					resource.setResourceURI(lines.get(i).substring(lines.get(i).indexOf(" [") + 2, lines.get(i).indexOf("]")));
					break;
				}
			}


			boolean toBeAdded = false;
			String skippingReason = null;

			//Setting start and end indexes
			//First we see if there is any table defined below the HTTP method specification.
			//This means that this schema will be referring to some other schema.
			//If this case is not found then, we look upwards in the markdown file.
			for(int i=counter;i<sizeOfDocument;i++)
			{
				//Marks start of new operation
				if(lines.get(i).trim().startsWith(ApplicationConstants.CONST_OPERATION_START)){
					toBeAdded = false;
					break;
				}
				
				//if(lines.get(i).startsWith("### Description of DELETE resource/type attributes:"))
				if(lines.get(i).startsWith(methodDescription))
				{
					for(int k=i;k<sizeOfDocument;k++)
					{
						
						if(lines.get(k).toUpperCase().startsWith(ApplicationConstants.NO_PAYLOAD_KEYWORD.toUpperCase())
								|| lines.get(k).toUpperCase().startsWith(ApplicationConstants.ATTACHMENT_KEYWORD.toUpperCase())
								|| lines.get(k).toUpperCase().startsWith(ApplicationConstants.NO_CONTENT_KEYWORD.toUpperCase())){
							toBeAdded = false;
							skippingReason = lines.get(k).toUpperCase();
							break;
						}

						//Setting the start index
						if(resource.getStartIndex() == 0 
								&& lines.get(k).startsWith("|")){
							resource.setStartIndex(k);
						}

						//Setting the end index
						if(resource.getStartIndex() > 0 
								&& lines.get(k).trim().length() == 0)  {
							resource.setEndIndex(k);
							toBeAdded = true;
							break;
						}

					}

					break;
				}//end of if-loop 'lines.get(i).startsWith(methodDescription)'

			}//end of for-loop 'int i=counter;i<sizeOfDocument;i++'

			if(toBeAdded){
				//This means that a table was found beneath the resource in markdown file.
				operationsList.add(resource);
				
				//TODO: need to see if there is any table above the resource as well or not.
				this.populateResource(lines, counter, tempOperationsList, null);
				
			}else{
				if(null != skippingReason){
					LOG.error("<<<Skipped>>> :: " + httpMethod  + ":" + resource.getOperationName() + ". Reason :-" + skippingReason);
				}else{
					//LOG.error("<<<Skipped>>> :: " + httpMethod + " :" + resource.getOperationName() + ". Reason :-" + "Code to be updated");
					this.populateResource(lines, counter, operationsList, httpMethod);
				}
				
			}
			
			LOG.info("Parsed [" + httpMethod + "] resource: " + resource);
		} catch (Exception e) {
			// Bad practice to catch ALL exception
			LOG.error("Unable to perform Resource parsing: "+ resource,e);				
		}
		
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	public  boolean validateNumber(String str){
		boolean isNumber = true;
		try { 
			Integer.parseInt(str); 
		} catch(NumberFormatException e) { 
			isNumber= false; 
		} catch(NullPointerException e) {
			isNumber= false;
		}
		return isNumber;
	}
}
