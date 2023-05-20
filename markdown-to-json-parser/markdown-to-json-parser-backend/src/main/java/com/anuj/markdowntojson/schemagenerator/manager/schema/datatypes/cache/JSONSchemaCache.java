package com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 
 * @author anujmehra
 *
 */
@Component("jsonSchemaCache")
public class JSONSchemaCache {

	/**
	 * 
	 */
	private final Map<String, String> schemaTreeForest = new LinkedHashMap<String, String>();
	
	/**
	 * 
	 * @param key
	 * @param value
	 */
	public void updateJSONSchemaCache(String key, String value){
		schemaTreeForest.put(key, value);
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	public String getJSONSchema(String key){
		return schemaTreeForest.get(key);
	}
	
	/**
	 * 
	 * @return
	 */
	public Map<String,String> getJSONSchemaCacheMap(){
		return schemaTreeForest;
	}
	
}
