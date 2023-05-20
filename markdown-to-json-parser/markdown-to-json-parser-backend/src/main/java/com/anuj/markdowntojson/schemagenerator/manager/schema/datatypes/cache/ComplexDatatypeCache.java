package com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 
 * @author anujmehra
 *
 */
@Component("complexDatatypeCache")
public class ComplexDatatypeCache {

	/**
	 * 
	 */
	private final Map<String, String> complexDataTypeCacheMap = new LinkedHashMap<String, String>();


	/**
	 * 
	 * @param key
	 * @param value
	 */
	public void updateComplexDatatypeCache(String key, String value){
		complexDataTypeCacheMap.put(key, value);
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public String getComplexDatatype(String key){
		return complexDataTypeCacheMap.get(key);
	}

	/**
	 * 
	 * @return
	 */
	public Map<String,String> getComplexDatatypeCacheMap(){
		return complexDataTypeCacheMap;
	}
	
}
