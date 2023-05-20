package com.anuj.markdowntojson.markdownparser.manager;

import java.util.List;

import com.anuj.markdowntojson.exception.ApplicationException;
import com.anuj.markdowntojson.exception.ParseException;

/**
 * This is the Abstract class of the manager layer.
 * It implements template pattern.
 * Manager layer implementation performs following two functions:
 * 1. Parse all datatypes present in the markdown file. There can be three types of datatypes present in markdown file:
 * 		a. Simple datatypes
 * 		b. Complex dataypes
 * 		c. Composite datatypes
 * 
 * 2. Parse all operations present in the markdown file.
 * 
 * @author anujmehra
 *
 */
public abstract class MarkdownParserManager {

	/**
	 * Abstract method declaration
	 * @param mdFileContents List<String>
	 */
	public abstract void parseDatatypes(List<String> mdFileContents) throws ParseException;
	
	/**
	 * Abstract method declaration
	 * @param mdFileContents List<String>
	 * @throws ParseException 
	 */
	public abstract void parseOperations(List<String> mdFileContents) throws ParseException;
	
	/**
	 * This is the template method.
	 * This method is calls the methods which populate the application cache.
	 * @param mdFileContents List<String>
	 * @throws ApplicationException 
	 */
	public void parseMarkdownFile(List<String> mdFileContents) throws ApplicationException{
		this.parseDatatypes(mdFileContents);// fail if datatype parsing errors
		this.parseOperations(mdFileContents);// Don't fail and skip un parsable resources
	}//end of method 'parseMarkdownFile'
	
}
