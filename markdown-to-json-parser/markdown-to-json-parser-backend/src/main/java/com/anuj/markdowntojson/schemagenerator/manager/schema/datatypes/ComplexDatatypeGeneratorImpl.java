package com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.exception.ApplicationException;
import com.anuj.markdowntojson.schemagenerator.manager.schema.SchemaGeneratorUtil;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache.SimpleDatatypeCache;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.util.DatatypeGeneratorConstants;
import com.anuj.markdowntojson.schemagenerator.manager.schema.dto.ComplexDatatypeDTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author anujmehra
 *
 */
@Component("complexDatatypeGenerator")
public class ComplexDatatypeGeneratorImpl implements DatatypeGenerator{

	/**
	 * Logger object.
	 */
	private static final Logger LOG = Logger.getLogger(ComplexDatatypeGeneratorImpl.class);
	
	
	/**
	 * 
	 */
	@Autowired
	private SchemaGeneratorUtil schemaGeneratorUtil;
	
	/**
	 * 
	 */
	@Autowired
	private SimpleDatatypeCache simpleDatatypeCache;

	/**
	 * @param fileName String
	 * @param lines List<String>
	 * @return 
	 * @throws ApplicationException
	 */
	public String[] getDatatype(List<String> lines, String fileName) throws ApplicationException
	{

		String[] returnVal = new String[2];

		String currentLine = lines.get(0);

		currentLine = currentLine.replace("|", ";");

		String[] lineData = currentLine.split(";");

		lineData = schemaGeneratorUtil.trimValue(lineData);
		
		if(!lineData[3].startsWith("ARRAY of"))
		{
			returnVal = this.generateSchemaType(lineData,returnVal);

		}else{
			lineData[3] = lineData[3].replace("ARRAY of ", "");
			returnVal = this.generateSchemaType(lineData,returnVal);
			
			returnVal[1] = "\"type\": \"array\", \"items\":{" + returnVal[1] + "}";
		}

		return returnVal;
	}


	/**
	 * 
	 * @param lineData
	 * @param returnVal
	 * @return 
	 * @throws ApplicationException
	 */
	private String[] generateSchemaType(String[] lineData,String... returnVal) throws ApplicationException{

		final ComplexDatatypeDTO complexDatatypeDTO = new ComplexDatatypeDTO();

		if(null != simpleDatatypeCache.getSimpleDatatype(lineData[3]))
		{
			complexDatatypeDTO.setType(simpleDatatypeCache.getSimpleDatatype(lineData[3]));
		}else{

			final String temp[] = lineData[3].split(DatatypeGeneratorConstants.CONSTANT_REGEX_ALPHA_NUMERIC);
			complexDatatypeDTO.setType(simpleDatatypeCache.getSimpleDatatype(temp[0]));
			complexDatatypeDTO.setMaxLength(Integer.valueOf(temp[1]));
		}


		if(this.isNotNullAndEmpty(lineData[5])){
			complexDatatypeDTO.setPattern(lineData[5]);
		}

		if(this.isNotNullAndEmpty(lineData[6])){
			complexDatatypeDTO.setConstant(schemaGeneratorUtil.trimValue(lineData[6]));
		}

		final ObjectMapper mapper = new ObjectMapper();

		//This is being done, so that null values won't be returned in the JSON Stream.
		//mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		// serialize userList to JSON format and write to file
		String jsonString = null;
		try {
			jsonString = mapper.writeValueAsString(complexDatatypeDTO);
		} catch (JsonProcessingException e) {
			LOG.error("JsonProcessingException occured : ComplexDatatypeGeneratorImpl : generateSchemaType ::",e);
			throw new ApplicationException("JsonProcessingException occured : ComplexDatatypeGeneratorImpl : generateSchemaType ::",e);
		}

		returnVal[0] = lineData[2];
		returnVal[1] = jsonString.substring(1, jsonString.length()-1);
		
		return returnVal;
	}

	
	/**
	 * 
	 * @param var String
	 * @return
	 */
	public boolean isNotNullAndEmpty(String var){

		boolean flag = false;

		if(null != var 
				&& var.length() >0){
			flag = true;
		}

		return flag;
	}


}
