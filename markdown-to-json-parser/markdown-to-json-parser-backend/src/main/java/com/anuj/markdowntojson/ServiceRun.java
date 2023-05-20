package com.anuj.markdowntojson;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.anuj.markdowntojson.service.JSONSchemaServiceImpl;
import com.anuj.markdowntojson.springconfig.SpringBeanConfig;

/**
 * 
 * @author anujmehra
 *
 */
public final class ServiceRun {

	private ServiceRun(){

	}
	
	/**
	 * 
	 * @return
	 */
	public static JSONSchemaServiceImpl getExecutableService(){

		final ApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringBeanConfig.class);
		return (JSONSchemaServiceImpl)applicationContext.getBean("jsonSchemaService");
	}
	
}
