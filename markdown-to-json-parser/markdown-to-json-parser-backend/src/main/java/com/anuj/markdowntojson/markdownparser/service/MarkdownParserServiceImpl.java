package com.anuj.markdowntojson.markdownparser.service;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.exception.ApplicationException;
import com.anuj.markdowntojson.markdownparser.manager.MarkdownParserManager;
import com.anuj.markdowntojson.markdownparser.reader.FileReader;

/**
 * This is the manager class for the 'MarkdownParser' part of the tool.
 * @author anujmehra
 *
 */
@Component("markdownParserService")
public class MarkdownParserServiceImpl {

	/**
	 * Object reference for FileReader
	 */
	@Autowired
	private FileReader fileReader;
	
	/**
	 * Object reference for MarkdownParserManagerImpl
	 */
	@Autowired
	private MarkdownParserManager markdownParserManager;
	
	
	/**
	 * Logic to start parsing the markdown file starts from here.
	 * This method makes a call to the service layer of the 'MarkdownParser' part of the tool.
	 * Once parsing is complete, cache is populated with lines present in different datatypes and operations, present in markdown file. 
	 * @param markdownFileLocation String  - Fully qualified path of the markdown file to be parsed
	 * @throws ApplicationException
	 */
	public void parse(String markdownFileLocation) throws ApplicationException
	{
		final List<String> lines = this.readMarkdownFile(markdownFileLocation);
		markdownParserManager.parseMarkdownFile(lines);
	}//end of method execute
	
	
	/**
	 * This is private method.
	 * This method makes a call to the Utility class FileReader and fetches all the lines present in the input Markdown file.
	 * @param markdownFileLocation String - Fully qualified path of the markdown file to be parsed
	 * @return lines List<String> - All lines present in the markdown file.
	 * @throws ApplicationException
	 */
	private List<String> readMarkdownFile(String markdownFileLocation) throws ApplicationException
	{
		
		return fileReader.readFileLines(markdownFileLocation);
	}//end of method readMArkdownFile
	
	
}
