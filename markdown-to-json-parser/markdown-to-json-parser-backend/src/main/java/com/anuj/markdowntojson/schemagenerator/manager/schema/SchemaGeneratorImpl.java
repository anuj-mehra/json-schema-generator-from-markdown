package com.anuj.markdowntojson.schemagenerator.manager.schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache.ComplexDatatypeCache;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache.CompositeDatatypeCache;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache.JSONSchemaCache;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache.SimpleDatatypeCache;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.cache.TempJSONSchemaCache;
import com.anuj.markdowntojson.schemagenerator.manager.schema.datatypes.util.DatatypeGeneratorConstants;
import com.anuj.markdowntojson.schemagenerator.manager.schema.dto.Node;
import com.anuj.markdowntojson.schemagenerator.manager.schema.dto.NodeElements;
import com.anuj.markdowntojson.schemagenerator.manager.schema.exception.SchemaCreationException;

/**
 * 
 * @author anujmehra
 *
 */
@Component("schemaGenerator")
public class SchemaGeneratorImpl {

	/**
	 * Object reference for SchemaGeneratorUtil.
	 */
	@Autowired
	private SchemaGeneratorUtil schemaGeneratorUtil;

	/**
	 * Object reference for Tree.
	 */
	@Autowired
	private Tree tree;

	/**
	 * 
	 */
	@Autowired
	private SimpleDatatypeCache simpleDatatypeCache;

	/**
	 * 
	 */
	@Autowired
	private ComplexDatatypeCache complexDatatypeCache;

	/**
	 * 
	 */
	@Autowired
	private CompositeDatatypeCache compositeDatatypeCache;

	/**
	 * 
	 */
	@Autowired
	private JSONSchemaCache jsonSchemaCache;

	/**
	 * 
	 */
	@Autowired
	private TempJSONSchemaCache tempJSONSchemaCache;

	/**
	 * 
	 */
	private static final Logger LOG = Logger.getLogger(SchemaGeneratorImpl.class);

	/**
	 * 
	 * @param resourceStringContentList List<String>
	 * @return generatedJSONStream String
	 * @throws SchemaCreationException
	 */
	public String getJSONSchema(List<String> resourceStringContentList) throws SchemaCreationException{

		final StringBuffer generatedJSONStream = new StringBuffer();

		final Set<String> levelOneRequiredElements = new  HashSet<String>();

		final Node<NodeElements> rootNode  = new Node<NodeElements>(new NodeElements("0"), null);

		//Populate JSON Schema Tree
		tree.populateTree(resourceStringContentList,levelOneRequiredElements,rootNode);

		//Generating the JSON Schema from the populated Tree Structure.
		this.generateJSONSchema(rootNode,"JSONObject",generatedJSONStream);

		//This has been done, because after every structure a ',' will be populated. ',' must be removed after last structure.
		if(generatedJSONStream.charAt(generatedJSONStream.length()-1) == ','){
			generatedJSONStream.deleteCharAt(generatedJSONStream.length()-1);
		}

		schemaGeneratorUtil.populateSchemaClosingTags(generatedJSONStream, levelOneRequiredElements);


		LOG.debug("Generated Schema: -- " + generatedJSONStream.toString());

		return generatedJSONStream.toString();
	}//end of method 'populateTree'


	/**
	 * This method performs following two tasks:
	 * 		1.
	 * 		2. 
	 * @param rootNode Node<NodeElements>
	 * @param jsonType String
	 * @param generatedJSONStream StringBuffer
	 * @param forOneOfType Boolean
	 * @return requiredElements List<String>
	 * @throws SchemaCreationException
	 */
	private List<String> generateJSONSchema(Node<NodeElements> node, String jsonType,StringBuffer generatedJSONStream) throws SchemaCreationException{

		final int numberOfImmediateChildNodes = node.getChildren().size();

		if(null != jsonType 
				&& jsonType.equalsIgnoreCase("JSONObject")){
			generatedJSONStream.append('{');
			generatedJSONStream.append("\"type\":\"object\",");
			generatedJSONStream.append(" \"properties\": {");
		}

		if(null != jsonType 
				&& jsonType.equalsIgnoreCase("JSONArray")){
			generatedJSONStream.append('{');
			generatedJSONStream.append("\"type\":\"array\",");
			generatedJSONStream.append(" \"items\": {");
		}


		final List<String> requiredElements = new ArrayList<String>();

		for(int i=0;i<numberOfImmediateChildNodes;i++)
		{
			
			if(node.getChildren().get(i).getNodeElements().getType().equalsIgnoreCase(SchemaGeneratorConstants.CONSTANT_STRUCTURE)
					|| node.getChildren().get(i).getNodeElements().getType().equalsIgnoreCase(SchemaGeneratorConstants.CONSTANT_ONE_OF)) 
			{
				this.populateElementNameID(node.getChildren().get(i).getNodeElements().getAttributeName(),generatedJSONStream);

				generatedJSONStream.append("\"type\": \"object\",");
				generatedJSONStream.append("\"properties\": {");


				final List<String> innerRequiredElements = this.generateJSONSchema(node.getChildren().get(i), node.getChildren().get(i).getNodeElements().getType(),generatedJSONStream);

				
				if(node.getChildren().get(i).getNodeElements().getType().equalsIgnoreCase(SchemaGeneratorConstants.CONSTANT_ONE_OF))
				{
					generatedJSONStream.append("},\"oneOf\" : [" );

					for(final Node<NodeElements> n : node.getChildren().get(i).getChildren())
					{
						generatedJSONStream.append("{\"required\" : [");

						generatedJSONStream.append("\"" + n.getNodeElements().getAttributeName() + "\"");
						generatedJSONStream.append("]},");
					}
					generatedJSONStream.deleteCharAt(generatedJSONStream.length()-1);
					generatedJSONStream.append(']' );

				}else{
					if(null != innerRequiredElements 
							&& !innerRequiredElements.isEmpty())
					{
						generatedJSONStream.append("},\"required\": [" );

						final int numberOfRequiredElements = innerRequiredElements.size();

						for(int k=0;k<numberOfRequiredElements;k++)
						{
							if(k<numberOfRequiredElements-1){
								generatedJSONStream.append(innerRequiredElements.get(k) + ",");
							}else{
								generatedJSONStream.append(innerRequiredElements.get(k));
							}
						}
						generatedJSONStream.append(']' );

					}else{
						generatedJSONStream.append('}');
					}
				}


				//Added if there are any 'Mandatory (M)' elements.
				if(node.getChildren().get(i).getNodeElements().getIsmandatory().equalsIgnoreCase("M")){
					requiredElements.add("\"" + node.getChildren().get(i).getNodeElements().getAttributeName() + "\"");
				}


				//TODO..in case this is last, then don't print ',' ..Test this for JSON object as well..create a test case myself..
				if(i < numberOfImmediateChildNodes - 1)
				{
					generatedJSONStream.append("},");
				}else{
					generatedJSONStream.append('}');
				}
			}
			//example 'Array of Text50' -- will be an array but it won't have any children. So equalsIgnoreCase("ARRAY of") and not startsWith("ARRAY of").
			//'Array of Text50' -- This will be treated as any other non-complex element.
			else if(node.getChildren().get(i).getNodeElements().getType().equalsIgnoreCase(SchemaGeneratorConstants.CONSTANT_ARRAY_OF) )
			{
				
				this.populateElementNameID(node.getChildren().get(i).getNodeElements().getAttributeName(),generatedJSONStream);

				generatedJSONStream.append("\"type\": \"array\",");
				generatedJSONStream.append("\"items\": {");

				generatedJSONStream.append("\"type\": \"object\",");

				generatedJSONStream.append("\"properties\": {");

				final List<String> innerRequiredElements = this.generateJSONSchema(node.getChildren().get(i), node.getChildren().get(i).getNodeElements().getType(),generatedJSONStream);

				if(null != innerRequiredElements 
						&& !innerRequiredElements.isEmpty())
				{
					generatedJSONStream.append("},");
					generatedJSONStream.append("\"required\": [" );

					final int numberOfRequiredElements = innerRequiredElements.size();

					for(int k=0;k<numberOfRequiredElements;k++)
					{
						if(k<numberOfRequiredElements-1){
							generatedJSONStream.append(innerRequiredElements.get(k) + ",");
						}else{
							generatedJSONStream.append(innerRequiredElements.get(k));
						}
					}
					generatedJSONStream.append(']');
				}else{
					generatedJSONStream.append('}');
				}


				generatedJSONStream.append('}');


				//TODO..in case this is last, then don't print ',' ..Test this for JSON object as well..create a test case myself..
				if(i < numberOfImmediateChildNodes - 1)
				{
					generatedJSONStream.append("},");
				}else{
					generatedJSONStream.append('}');
				}

			}/*else if(rootNode.getChildren().get(i).getNodeElements().getType().equalsIgnoreCase(SchemaGeneratorConstants.CONSTANT_ONE_OF) )
			{
				this.populateElementNameID(rootNode.getChildren().get(i).getNodeElements().getAttributeName(), generatedJSONStream , Boolean.FALSE);

				generatedJSONStream.append("\"type\": \"object\",");
				generatedJSONStream.append("\"oneOf\": [");

				this.generateJSONSchema(rootNode.getChildren().get(i), rootNode.getChildren().get(i).getNodeElements().getType(),generatedJSONStream, Boolean.TRUE);

				generatedJSONStream.append(']');

				if(i < numberOfNodes - 1)
				{
					generatedJSONStream.append("},");
				}else{
					generatedJSONStream.append('}');
				}


			}*/
			else if(node.getChildren().get(i).getNodeElements().getType().startsWith(SchemaGeneratorConstants.CONSTANT_ARRAY_OF) 
					&& node.getChildren().get(i).getNodeElements().getType().length() > (SchemaGeneratorConstants.CONSTANT_ARRAY_OF).length() )
			{

				this.populateElementNameID(node.getChildren().get(i).getNodeElements().getAttributeName() , generatedJSONStream);

				generatedJSONStream.append("\"type\": \"array\",");

				generatedJSONStream.append("\"items\": {");

				this.populateElementDetails(node.getChildren().get(i).getNodeElements().getType(), generatedJSONStream);

				//This has been done to handle 'Array of ENUM'
				if(node.getChildren().get(i).getNodeElements().getType().replace(" ", "").equalsIgnoreCase(SchemaGeneratorConstants.CONSTANT_ARRAY_OF_ENUM))
				{
					this.populateEnumValues(node.getChildren().get(i).getNodeElements().getEnumElements(), generatedJSONStream);
				}


				generatedJSONStream.append('}');

				//Added if there are any 'Mandatory (M)' elements.
				if(node.getChildren().get(i).getNodeElements().getIsmandatory().equalsIgnoreCase("M")){
					requiredElements.add("\"" + node.getChildren().get(i).getNodeElements().getAttributeName() + "\"");
				}


				this.populateClosingBrackets(i, numberOfImmediateChildNodes, generatedJSONStream);

			}else{

				this.populateElementNameID(node.getChildren().get(i).getNodeElements().getAttributeName(), generatedJSONStream);

				this.populateElementDetails(node.getChildren().get(i).getNodeElements().getType(), generatedJSONStream);

				if(node.getChildren().get(i).getNodeElements().getType().equalsIgnoreCase(SchemaGeneratorConstants.CONSTANT_ENUM))
				{
					this.populateEnumValues(node.getChildren().get(i).getNodeElements().getEnumElements(), generatedJSONStream);

				}

				//Added if there are any 'Mandatory (M)' elements.
				if(node.getChildren().get(i).getNodeElements().getIsmandatory().equalsIgnoreCase("M")){
					requiredElements.add("\"" + node.getChildren().get(i).getNodeElements().getAttributeName() + "\"");
				}

				this.populateClosingBrackets(i, numberOfImmediateChildNodes, generatedJSONStream);

			}


		}//end of for-loop

		return requiredElements;
	}


	/**
	 * Populating closing brackets.
	 * @param loopCounter Integer
	 * @param numberOfLevelOneNodes Integer
	 * @param generatedJSONStream StringBuffer
	 */
	private void populateClosingBrackets(Integer loopCounter, Integer numberOfLevelOneNodes,StringBuffer generatedJSONStream){
		if(loopCounter < numberOfLevelOneNodes - 1)
		{
			generatedJSONStream.append("},");
		}else{
			generatedJSONStream.append('}');
		}
	}


	/**
	 * This method populates the element's 'id'.
	 * @param attributeName String
	 * @param generatedJSONStream StringBuffer
	 * @param forOneof Boolean
	 */
	private void populateElementNameID(String attributeName, StringBuffer generatedJSONStream){

		generatedJSONStream.append("\""+ attributeName + "\": {");
		generatedJSONStream.append("\"id\":\"" + attributeName + "\",");

	}


	/**
	 * Populating the element's details.
	 * @param elementType String
	 * @param generatedJSONStream StringBuffer
	 * @throws SchemaCreationException
	 */
	private void populateElementDetails(String elementType, StringBuffer generatedJSONStream) throws SchemaCreationException{
		if(elementType.startsWith(SchemaGeneratorConstants.CONSTANT_ARRAY_OF) 
				&& elementType.length() > (SchemaGeneratorConstants.CONSTANT_ARRAY_OF).length())
		{
			final String[] arrayOf = elementType.split(" ");

			this.populateElementDetails(arrayOf[2],generatedJSONStream);
		}
		else if(null != simpleDatatypeCache.getSimpleDatatype(elementType))
		{
			generatedJSONStream.append("\"type\":\"" + simpleDatatypeCache.getSimpleDatatype(elementType) + "\"");
		}else if(null != complexDatatypeCache.getComplexDatatype(elementType))
		{
			generatedJSONStream.append(complexDatatypeCache.getComplexDatatype(elementType));
		}else if(null != compositeDatatypeCache.getCompositeDatatype(elementType))
		{
			generatedJSONStream.append(compositeDatatypeCache.getCompositeDatatype(elementType));
		}else if(null != tempJSONSchemaCache.getJSONSchema(elementType.toUpperCase()))
		{
			generatedJSONStream.append(tempJSONSchemaCache.getJSONSchema(elementType.toUpperCase()));
		}else if(null != jsonSchemaCache.getJSONSchema(elementType.toUpperCase()))
		{
			//TODO:: This is an hack
			if(elementType.toUpperCase().equals("PAYMENT")){
				
				generatedJSONStream.append(jsonSchemaCache.getJSONSchema(elementType.toUpperCase())
						.replaceFirst("\"type\":\"object\", \"properties\": \\{\"order\": \\{\"id\":\"order\"," , "")
						.replaceFirst("\\}\\},\"required\":\\[\"order\"\\]", ""));
			}else{
				generatedJSONStream.append(jsonSchemaCache.getJSONSchema(elementType.toUpperCase()));
			}

		}
		else {
			final String[] temp = elementType.split(DatatypeGeneratorConstants.CONSTANT_REGEX_ALPHA_NUMERIC);

			if(temp.length == 2)
			{
				if(null != simpleDatatypeCache.getSimpleDatatype(temp[0])){
					generatedJSONStream.append("\"type\":\"" + simpleDatatypeCache.getSimpleDatatype(temp[0]) + "\",");
				}else if(null != complexDatatypeCache.getComplexDatatype(temp[0])){
					generatedJSONStream.append(complexDatatypeCache.getComplexDatatype(temp[0]) + ",");
				}

				generatedJSONStream.append("\"maxLength\":" +  temp[1]);
			}else{
				throw new SchemaCreationException("Dependent type not found :" + elementType);
			}

		}
	}//end of method 'populateElementDetails'


	/**
	 * This method populate ENUM values for the given property.
	 * 
	 * @param enumElements List<String>
	 * @param generatedJSONStream StringBuffer
	 */
	private void populateEnumValues(List<String> enumElements, StringBuffer generatedJSONStream)
	{
		generatedJSONStream.append(",\"enum\": [");

		if(null != enumElements){
			for(final String value : enumElements){
				generatedJSONStream.append("\"" + value + "\",");
			}	
			generatedJSONStream.deleteCharAt(generatedJSONStream.length()-1);
		}

		generatedJSONStream.append(']');
	}//end of method 'populateEnumValues'

}