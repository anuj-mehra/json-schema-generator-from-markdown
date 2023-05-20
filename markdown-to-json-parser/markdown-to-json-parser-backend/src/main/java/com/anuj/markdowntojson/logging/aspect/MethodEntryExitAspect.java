package com.anuj.markdowntojson.logging.aspect;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Aspect class for entry exit methods.
 * An DEBUG/INFO log gets generated after entry and before exit of every public method in the application.
 * @author anujmehra
 *
 */
@Aspect
public class MethodEntryExitAspect {

	/**
	 * 
	 */
	private static final Logger LOG = Logger.getLogger(MethodEntryExitAspect.class);

	/**
	 * 
	 */
	@Pointcut("execution(public * com.anuj.markdownparser.service.*.*(..))")
	private void aroundServiceLoggingPointcut(){}
	
	@Pointcut("execution(public * com.anuj.markdownparser.schemagenerator..*.*(..))")
	private void aroundSchemaGeneratorLoggingPointcut(){}
	
	
	@Pointcut("execution(public * com.anuj.markdownparser.markdownparser..*.*(..)) &&"
			+ "!execution(public * com.anuj.markdownparser.markdownparser.parser.dto.*.*(..)) &&"
			+ "!execution(public * com.anuj.markdownparser.markdownparser.parser.helper.*.*(..))")
	private void aroundMarkdownParserLoggingPointcut(){}

	
	@Before("aroundMarkdownParserLoggingPointcut()")
	public void logBeforeMethod(JoinPoint joinPoint){
		LOG.debug(" ********** " + joinPoint.getTarget().getClass() + " : "+ joinPoint.getSignature().getName() + " : Method Entry  **********");
	}
	
	@After("aroundMarkdownParserLoggingPointcut()")
	public void logAfterMethod(JoinPoint joinPoint){
		LOG.debug(" ********** " + joinPoint.getTarget().getClass() + " : "+ joinPoint.getSignature().getName() + " : Method Exit  **********");
	}
	
	@Before("aroundSchemaGeneratorLoggingPointcut()")
	public void logBeforeMethod2(JoinPoint joinPoint){
		LOG.debug(" ********** " + joinPoint.getTarget().getClass() + " : "+ joinPoint.getSignature().getName() + " : Method Entry  **********");
	}
	
	@After("aroundSchemaGeneratorLoggingPointcut()")
	public void logAfterMethod2(JoinPoint joinPoint){
		LOG.debug(" ********** " + joinPoint.getTarget().getClass() + " : "+ joinPoint.getSignature().getName() + " : Method Exit  **********");
	}
	
	@Before("aroundServiceLoggingPointcut()")
	public void logBeforeMethod3(JoinPoint joinPoint){
		LOG.info(" ********** " + joinPoint.getTarget().getClass() + " : "+ joinPoint.getSignature().getName() + " : Method Entry  **********");
	}
	
	@After("aroundServiceLoggingPointcut()")
	public void logAfterMethod3(JoinPoint joinPoint){
		LOG.info(" ********** " + joinPoint.getTarget().getClass() + " : "+ joinPoint.getSignature().getName() + " : Method Exit  **********");
	}
	
}
