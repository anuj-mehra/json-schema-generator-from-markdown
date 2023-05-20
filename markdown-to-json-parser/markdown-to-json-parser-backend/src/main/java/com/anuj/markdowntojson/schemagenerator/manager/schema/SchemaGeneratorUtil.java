package com.anuj.markdowntojson.schemagenerator.manager.schema;

import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * 
 * @author anujmehra
 *
 */
@Component("schemaGeneratorUtil")
public class SchemaGeneratorUtil {

	/**
	 * 
	 * @param generatedJSONStream
	 * @param requiredElements
	 */
	public void populateSchemaClosingTags(StringBuffer generatedJSONStream, Set<String> requiredElements){
		
		if(requiredElements.isEmpty()){
			generatedJSONStream.append('}');
		}else{
			generatedJSONStream.append("},");

			//Required elements at level '1' will be placed here.
			generatedJSONStream.append("\"required\":[");

			for(final String reqElements: requiredElements){
				generatedJSONStream.append(reqElements + ',');
			}
			generatedJSONStream.deleteCharAt(generatedJSONStream.length()-1);
			generatedJSONStream.append(']');

		}

		generatedJSONStream.append('}');
	}
	
	/**
	 * 
	 * @param str
	 * @return
	 */
	public  String trimValue(String str){
		return str.trim();
	}

	/**
	 * 
	 * @param strArr
	 * @return
	 */
	public  String[] trimValue(String... strArr){
		
		String[] tempArr = new String[strArr.length];
		
		for(int i=0;i<strArr.length;i++){
			tempArr[i] = strArr[i].trim();
		}
		
		return tempArr;
	}
	
}
