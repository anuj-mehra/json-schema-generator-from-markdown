package com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes;

import java.util.List;

import com.anuj.markdowntojson.exception.ApplicationException;

/**
 * 
 * @author anujmehra
 *
 */
public interface DatatypeGenerator {

	/**
	 * 
	 * @param lines
	 * @param fileName
	 * @return
	 * @throws ApplicationException
	 */
	String[] getDatatype(List<String> lines, String fileName) throws ApplicationException;
	
}
