package com.anuj.markdowntojson.schemagenerator.manager.schema.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * @author anujmehra
 *
 */
public class ComplexDatatypeDTO {

	/**
	 * 
	 */
	@JsonInclude(value=Include.ALWAYS)
	private String type;
	
	/**
	 * 
	 */
	@JsonInclude(value=Include.NON_DEFAULT)
	private int maxLength;
	
	/**
	 * 
	 */
	@JsonInclude(value=Include.NON_NULL)
	private String mandatory;
	
	/**
	 * 
	 */
	@JsonInclude(value=Include.NON_NULL)
	private String pattern;
	
	/**
	 * 
	 */
	@JsonInclude(value=Include.NON_NULL)
	private String constant;
	

	/**
	 * 
	 */
	public String getType() {
		return type;
	}

	/**
	 * 
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * 
	 */
	public int getMaxLength() {
		return maxLength;
	}

	/**
	 * 
	 */
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	/**
	 * 
	 */
	public String getMandatory() {
		return mandatory;
	}

	/**
	 * 
	 */
	public void setMandatory(String mandatory) {
		this.mandatory = mandatory;
	}

	/**
	 * 
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * 
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * 
	 */
	public String getConstant() {
		return constant;
	}

	/**
	 * 
	 */
	public void setConstant(String constant) {
		this.constant = constant;
	}


	/**
	 * 
	 */
	@Override
	public String toString(){
		
		return " { type:" + this.type + ", maxLength:" + this.maxLength + ", pattern:" + this.pattern + ", constant:" + this.constant + "}";
	}
	
}
