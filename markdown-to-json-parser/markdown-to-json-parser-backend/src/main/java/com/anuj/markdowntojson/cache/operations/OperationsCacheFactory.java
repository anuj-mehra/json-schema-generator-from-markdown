package com.anuj.markdowntojson.cache.operations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.util.HTTPMethods;

/**
 * 
 * @author anujmehra
 *
 */
@Component("operationsCacheFactory")
public class OperationsCacheFactory {

	/**
	 * 
	 */
	@Autowired
	private OperationsCache getOperationsCache;
	
	/**
	 * 
	 */
	@Autowired
	private OperationsCache putOperationsCache;
	
	/**
	 * 
	 */
	@Autowired
	private OperationsCache postOperationsCache;
	
	/**
	 * 
	 */
	@Autowired
	private OperationsCache deleteOperationsCache;
	
	/**
	 * 
	 */
	@Autowired
	private OperationsCache tempOperationsCache;
	
	/**
	 * 
	 * @param httpMethodType
	 * @return
	 */
	public OperationsCache getOperationsCache(HTTPMethods httpMethod){
	
		OperationsCache operationsCache = null;
		
		if(null != httpMethod){
			if(httpMethod.equals(HTTPMethods.GET)){
				operationsCache=  getOperationsCache;
			}else if(httpMethod.equals(HTTPMethods.PUT)){
				operationsCache =  putOperationsCache;
			}else if(httpMethod.equals(HTTPMethods.POST)){
				operationsCache =  postOperationsCache;
			}else if(httpMethod.equals(HTTPMethods.DELETE)){
				operationsCache =  deleteOperationsCache;
			}
		}else{
			operationsCache =  tempOperationsCache;
		}
		
		return operationsCache;
	}//end of method getOperationsCache
	
	
}//end of class OperationsCacheFactory
