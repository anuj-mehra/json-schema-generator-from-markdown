package com.anuj.markdowntojson.cache.operations;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 
 * @author anujmehra
 *
 */
public interface OperationsCache{
	
	/**
	 * 
	 * @param key
	 * @param value
	 */
	void updateOperationsCache(String key, List<String> value);

	/**
	 * 
	 * @param key
	 * @return
	 */
	List<String> getOperations(String key);

	/**
	 * 
	 * @return
	 */
	Map<String,List<String>> getOperationsCacheMap();

	/**
	 * 
	 * @return
	 */
	Set<String> getOperationsNames();
	
}
