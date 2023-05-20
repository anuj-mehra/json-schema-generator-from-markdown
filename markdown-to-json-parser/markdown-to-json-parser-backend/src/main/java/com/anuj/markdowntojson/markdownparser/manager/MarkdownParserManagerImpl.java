package com.anuj.markdowntojson.markdownparser.manager;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.exception.ParseException;
import com.anuj.markdowntojson.markdownparser.parser.types.MarkdownFileParser;

/**
 * This is the concrete implementation of the Manager layer class.
 * MarkdownParserManager is the abstract parent class, which implements the template pattern for order of steps to be executed.
 * @author anujmehra
 *
 */
@Component("markdownParserManager")
public class MarkdownParserManagerImpl extends MarkdownParserManager{

	/**
	 * Object reference for DatatypesMarkdownFileParserImpl
	 */
	@Autowired
	private MarkdownFileParser datatypesMarkdownFileParser;

	/**
	 * 
	 */
	@Autowired
	private MarkdownFileParser httpMarkdownFileParser;
	//private MarkdownFileParser httpGETMarkdownFileParser;


	/**
	 * This is the concrete implementation of the abstract method 'parseDatatypes' declared in abstract class 'MarkdownParserManager'.
	 * This method parses the markdown file and populates the application cache with all the Datatypes present in the provided markdown file.
	 * @param  lines List<String>  -- Lines present in the markdown file
	 */
	public void parseDatatypes(List<String> lines) throws ParseException
	{
		datatypesMarkdownFileParser.performMarkdownParsing(lines);
	}

	/**
	 * This is the concrete implementation of the abstract method 'parseOperations' declared in abstract class 'MarkdownParserManager'.
	 * This method parses the markdown file and populates the application cache with all the GET Operations present in the provided markdown file.
	 * @param  lines List<String>  -- Lines present in the markdown file
	 */
	public void parseOperations(List<String> lines) throws ParseException
	{
		//Parse for GET
		httpMarkdownFileParser.performMarkdownParsing(lines);

		/*//Parse for PUT
		markdownFileParserFactory.getMarkdownFileParser(HTTPMethods.PUT).performMarkdownParsing(lines);

		//Parse for POST
		markdownFileParserFactory.getMarkdownFileParser(HTTPMethods.POST).performMarkdownParsing(lines);

		//Parse for DELETe
		markdownFileParserFactory.getMarkdownFileParser(HTTPMethods.DELETE).performMarkdownParsing(lines);*/
	}


}
