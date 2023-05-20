package com.anuj.markdowntojson.markdownparser.reader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.exception.ApplicationException;

/**
 * 
 * @author anujmehra
 *
 */
@Component("fileReader")
public class FileReader {

	/**
	 * Logger Object
	 */
	private final static Logger LOG = Logger.getLogger(FileReader.class);
	
	/**
	 * This method expects the fully qualified path of the file to be read.
	 * @param filePath
	 * @return
	 * @throws IOException 
	 */
	public List<String> readFileLines(String filePath) throws ApplicationException{

		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get(filePath),Charset.forName("UTF-8"));//.defaultCharset());
		} catch (IOException e) {
			LOG.error("IOException occured:Unable to read the file at: "+filePath);
			throw new ApplicationException("IOException occured:Unable to read file at: "+filePath , e);
		}

		return lines;
	}


	/**
	 * 
	 * @param directory
	 * @return
	 */
	public List<String> readAllFileNames(String directory){
		
		final List<String> fileNames = new ArrayList<String>();

		final File[] files = new File(directory).listFiles();

		for (final File file : files) {
		    if (file.isFile()) {
		    	fileNames.add(file.getName());
		    }
		}
		
		return fileNames;
	}
	
	/**
	 * 
	 * @param fileName
	 * @return
	 * @throws ApplicationException
	 */
	public String readFileDataAsString(String fileName) throws ApplicationException {

		byte[] fileData = null;
		try {
			fileData = Files.readAllBytes(Paths.get(fileName));
		} catch (IOException e) {
			LOG.error("IOException occured : FileReader : readFileDataAsString :: " , e);
			throw new ApplicationException("IOException occured : FileReader : readFileDataAsString :: " , e);
		}
		
		return new String(fileData);
	}

}
