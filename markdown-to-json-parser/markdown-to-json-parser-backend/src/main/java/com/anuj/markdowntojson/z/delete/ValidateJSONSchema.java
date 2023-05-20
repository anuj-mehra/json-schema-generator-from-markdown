package com.anuj.markdowntojson.z.delete;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.anuj.markdowntojson.exception.ApplicationException;
import com.anuj.markdowntojson.markdownparser.reader.FileReader;

public class ValidateJSONSchema {

	public static void main(String args[]){
	
		FileReader fileReader = new FileReader();
	
		String jsonStream = null;
		try {
			jsonStream = fileReader.readFileDataAsString("E:/Erste/svn/code/MarkdownParser/MarkdownParser/src/main/java/at/sitsolutions/osp/z/delete/validateJSON.json");
		} catch (ApplicationException e) {
			
			e.printStackTrace();
		}
		
		boolean flag1 = ValidateJSONSchema.isValidJSONArray(jsonStream);
		
		boolean flag2 = ValidateJSONSchema.isValidJSONObject(jsonStream);
		
		System.out.println(flag1);
		System.out.println(flag2);
	
	}
	
	
	public static boolean isValidJSONArray(String jsonString){

		boolean status = true;

		try{
			JSONArray jsonArray = new JSONArray(jsonString);
		}catch(JSONException jsonException){
			//jsonException.printStackTrace();
			status = false;
		}

		return status;
	}
	
	
	public static boolean isValidJSONObject(String jsonString){

		boolean status = true;
		try{
			JSONObject jsonObject = new JSONObject(jsonString);
		}catch(JSONException jsonException){
			jsonException.printStackTrace();
			status = false;
		}

		return status;
	}
	
}
