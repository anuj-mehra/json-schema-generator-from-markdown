package com.anuj.markdowntojson.markdownparser.parser.types;

import java.util.List;

import com.anuj.markdowntojson.exception.ParseException;

/**
 * This is the interface class
 * @author anujmehra
 *
 */
public interface MarkdownFileParser {

	/**
	 * 
	 * @param lines List<String>
	 * @throws ParseException 
	 */
	void performMarkdownParsing(List<String> lines) throws ParseException;
}
