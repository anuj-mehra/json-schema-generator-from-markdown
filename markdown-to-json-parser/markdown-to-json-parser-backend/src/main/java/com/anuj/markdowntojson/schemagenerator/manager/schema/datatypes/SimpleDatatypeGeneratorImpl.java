package com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.schemagenerator.manager.schema.SchemaGeneratorUtil;

/**
 * 
 * @author anujmehra
 *
 */
@Component("simpleDatatypeGenerator")
public class SimpleDatatypeGeneratorImpl implements DatatypeGenerator{

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
		
		String currentLine = lines.get(0);

		currentLine = currentLine.replace("|", ",");

		final String[] lineData = currentLine.split(",");
		 
		returnVal[0] = schemaGeneratorUtil.trimValue(lineData[2]);
		returnVal[1] = schemaGeneratorUtil.trimValue(lineData[3]); 		
		return returnVal;
	}

	
}
