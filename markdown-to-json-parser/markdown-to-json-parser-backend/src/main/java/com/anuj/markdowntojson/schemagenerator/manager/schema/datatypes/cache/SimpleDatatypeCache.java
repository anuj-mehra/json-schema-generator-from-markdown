package com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 
 * @author anujmehra
 *
 */
@Component("simpleDatatypeCache")
public class SimpleDatatypeCache {

	/**
	 * 
	 */
	private final Map<String, String> simpleDataTypeCacheMap = new LinkedHashMap<String, String>();


	/**
	 * 
	 * @param key
	 * @param value
	 */
	public void updateSimpleDatatypeCache(String key, String value){
		simpleDataTypeCacheMap.put(key, value);
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public  String getSimpleDatatype(String key){
		return simpleDataTypeCacheMap.get(key);
	}

	/**
	 * 
	 * @return
	 */
	public  Map<String,String> getSimpleDatatypeCacheMap(){
		return simpleDataTypeCacheMap;
	}
	
}
