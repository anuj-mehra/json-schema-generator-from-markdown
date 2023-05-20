package com.anuj.markdowntojson.springconfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * 
 * @author anujmehra
 *
 */
@Configuration
@ComponentScan(basePackages = "com.anuj.markdowntojson")
//@PropertySource("file:resources/environment.properties")
@EnableAspectJAutoProxy
public class SpringBeanConfig {

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
}
