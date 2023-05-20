package com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.schemagenerator.manager.schema.SchemaGeneratorUtil;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache.ComplexDatatypeCache;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache.SimpleDatatypeCache;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.util.DatatypeGeneratorConstants;

/**
 * 
 * @author anujmehra
 *
 */
@Component("compositeDatatypeGenerator")
public class CompositeDatatypeGeneratorImpl implements DatatypeGenerator{

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
	private SchemaGeneratorUtil schemaGeneratorUtil;
	
	/**
	 * 
	 */
	public String[] getDatatype(List<String> lines, String fileName){
		

		String[] returnVal = new String[2];
		
		final Set<String> mandatoryElements = new HashSet<String>();
		
		final StringBuffer complexDatatype = new StringBuffer();

		complexDatatype.append("\"type\": \"object\",");
		complexDatatype.append("\"properties\": {");
		
		for(int i=0;i<lines.size();i++)
		{
			String currentLine = lines.get(i);

			currentLine = currentLine.replace("|", ",");

			final String[] lineData = currentLine.split(",");

			if(lineData[4].trim().equals("M")){
				mandatoryElements.add("\"" + lineData[2].trim() + "\"");
			}
			
			complexDatatype.append("\"" +  lineData[2].trim() + "\": {");

			complexDatatype.append("\"id\":\"" + lineData[2].trim() +  "\",");

			//Now populating the type of the element
			if(null != simpleDatatypeCache.getSimpleDatatype(lineData[3].trim()))
			{
				complexDatatype.append("\"type\":\"" + simpleDatatypeCache.getSimpleDatatype(lineData[3].trim()) + "\"");
			}else if(null != complexDatatypeCache.getComplexDatatype(lineData[3].trim()))
			{
				complexDatatype.append(complexDatatypeCache.getComplexDatatype(lineData[3].trim()));
			}else
			{
				final String temp[] = lineData[3].split(DatatypeGeneratorConstants.CONSTANT_REGEX_ALPHA_NUMERIC);
				
				complexDatatype.append("\"type\":\"" + temp[0] + "\"");
				complexDatatype.append("\"maxvalue\":\"" + temp[1] + "\"");
			}

			
			complexDatatype.append("},");
		}
		
		complexDatatype.deleteCharAt(complexDatatype.length()-1);
		
		schemaGeneratorUtil.populateSchemaClosingTags(complexDatatype, mandatoryElements);
		
		
		//Removing the closing bracket.
		complexDatatype.deleteCharAt(complexDatatype.length()-1);
		
		returnVal[0] = fileName;
		returnVal[1] = complexDatatype.toString();
		
		return returnVal;
	
	}//end of method 'getDatatype'

}
