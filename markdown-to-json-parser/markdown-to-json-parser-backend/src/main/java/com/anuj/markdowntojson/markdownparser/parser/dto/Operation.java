package com.anuj.markdowntojson.markdownparser.parser.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author anujmehra
 *
 */
public class Operation {

	/**
	 * Name of the operation
	 */
	private String operationName;

	/**
	 * Operation description
	 */
	private String operationDescription;

	/**
	 * Resource URI
	 */
	private String resourceURI;

	/**
	 * Start line for the respective schema
	 */
	private int startIndex;

	/**
	 * End line for the respective schema
	 */
	private int endIndex;

	/**
	 * Schema rows for the respective schema
	 */
	private List<String> schemaRows = new ArrayList<String>();

	/**
	 * 
	 */
	public String getOperationName() {
		return operationName;
	}
	
	/**
	 * 
	 */
	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}
	
	/**
	 * 
	 */
	public String getOperationDescription() {
		return operationDescription;
	}
	
	/**
	 * 
	 */
	public void setOperationDescription(String operationDescription) {
		this.operationDescription = operationDescription;
	}
	
	/**
	 * 
	 */
	public String getResourceURI() {
		return resourceURI;
	}
	
	/**
	 * 
	 */
	public void setResourceURI(String resourceURI) {
		this.resourceURI = resourceURI;
	}
	
	/**
	 * 
	 */
	public int getStartIndex() {
		return startIndex;
	}
	
	/**
	 * 
	 */
	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}
	
	/**
	 * 
	 */
	public int getEndIndex() {
		return endIndex;
	}
	
	/**
	 * 
	 */
	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}
	
	/**
	 * 
	 */
	public List<String> getSchemaRows() {
		return schemaRows;
	}
	
	/**
	 * 
	 */
	public void setSchemaRows(List<String> schemaRows) {
		this.schemaRows = schemaRows;
	}
	
	/**
	 * 
	 */
	public void addSchemaRows(String schemaRow) {
		this.schemaRows.add(schemaRow);
	}

	/**
	 * 
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Operation [operationName=");
		builder.append(operationName);
		builder.append(", operationDescription=");
		builder.append(operationDescription);
		builder.append(", resourceURI=");
		builder.append(resourceURI);
		builder.append(", startIndex=");
		builder.append(startIndex);
		builder.append(", endIndex=");
		builder.append(endIndex);
		builder.append(", schemaRows=");
		builder.append(schemaRows);
		builder.append("]");
		return builder.toString();
	}


}
