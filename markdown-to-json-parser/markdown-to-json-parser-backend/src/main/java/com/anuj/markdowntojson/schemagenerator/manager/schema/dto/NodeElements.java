package com.anuj.markdowntojson.schemagenerator.manager.schema.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author anujmehra
 *
 */
public class NodeElements {

	/**
	 * 
	 */
	private final String level;
	
	/**
	 * 
	 */
	private String attributeName;
	
	/**
	 * 
	 */
	private String type;
	
	/**
	 * 
	 */
	private String ismandatory;
	
	/**
	 * To hold possible enum values in case the element is of type ENUM.
	 */
	private List<String> enumElements;

	/**
	 * Overloaded constructor
	 * @param level
	 */
	public NodeElements(String level){
		this.level = level;
	}

	
	/**
	 * Overloaded constructor
	 * @param level
	 * @param attributeName
	 * @param type
	 * @param ismandatory
	 */
	public NodeElements(String level,String attributeName,String type,String ismandatory){
		this.level = level;
		this.attributeName = attributeName;
		this.type = type;
		this.ismandatory = ismandatory;
	}

	/**
	 * 
	 * @return
	 */
	public String getLevel() {
		return level;
	}

	/**
	 * 
	 * @return
	 */
	public String getAttributeName() {
		return attributeName;
	}

	/**
	 * 
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * 
	 * @return
	 */
	public String getIsmandatory() {
		return ismandatory;
	}

	/**
	 * 
	 * @return
	 */
	public List<String> getEnumElements() {
		return enumElements;
	}

	/**
	 * 
	 * @param enumElements
	 */
	public void setEnumElements(List<String> enumElements) 
	{
		if(null == enumElements){
			enumElements = new ArrayList<String>();
		}
		this.enumElements = enumElements;
	}

	/**
	 * 
	 * @param enumElement
	 */
	public void addEnumElement(String enumElement) 
	{
		if(null == this.enumElements){
			this.enumElements = new ArrayList<String>();
		}
		this.enumElements.add(enumElement);
	}


}
