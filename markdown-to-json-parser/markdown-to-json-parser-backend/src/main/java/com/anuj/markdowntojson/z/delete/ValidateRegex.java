package com.anuj.markdowntojson.z.delete;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author anujmehra
 *
 */
public final class ValidateRegex {

	/**
	 * 
	 */
	private ValidateRegex(){
		
	}
	
	/*private static String regex = "^(19|20)\\d\\d[- /.](0[1-9]|1[012])[- /.](0[1-9]|[12][0-9]|3[01])$";

	public static void main(String args[]){
		int input = 123;

		Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher("211-07-14");
        System.out.println("Input String matches regex - "+matcher.matches());


	}*/


	/**
	 * 
	 */
	private static final String regex = "^[{][\"errors\":]$";

	/**
	 * 
	 * @param args
	 */
	public static void main(String... args){

		final Pattern pattern = Pattern.compile(regex);
		//final Matcher matcher = pattern.matcher("{\"errors\":[{\"parameters\":[{\"name\":\"PLUGIN_NAME\",\"value\":\"Plugin Call Process\"}],\"code\":\"INTERNAL_SERVER_ERROR\"}]}");
		final Matcher matcher = pattern.matcher("{\"errors\":");
		System.out.println("Input String matches regex - "+matcher.matches());


	}
}
