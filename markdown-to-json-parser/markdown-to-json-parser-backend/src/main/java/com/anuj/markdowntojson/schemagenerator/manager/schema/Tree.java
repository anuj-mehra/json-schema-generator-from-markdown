package com.anuj.markdowntojson.schemagenerator.manager.schema;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anuj.markdowntojson.schemagenerator.manager.schema.dto.Node;
import com.anuj.markdowntojson.schemagenerator.manager.schema.dto.NodeElements;

/**
 * 
 * @author anujmehra
 *
 */
@Component("tree")
public class Tree {

	/**
	 * 
	 */
	@Autowired
	private SchemaGeneratorUtil schemaGeneratorUtil;

	/**
	 * 
	 * @param lines List<String>
	 * @param levelOneRequiredElements Set<String>
	 * @param rootNode Node<NodeElements>
	 */
	public void populateTree(List<String> lines, Set<String> levelOneRequiredElements, Node<NodeElements> rootNode) {

		int maxDepth = 1;

		final Set<String> complexNodes = new  HashSet<String>();

		final Map<Integer, Node<NodeElements>> currentHeadLevelMap = new HashMap<Integer, Node<NodeElements>>();

		final Set<String> lineParsed = new HashSet<String>();

		final int schemaSize = lines.size();

		//Looping to find the max depth and complex nodes and required elements at Level 1.
		for(int i=0;i<schemaSize;i++)
		{
			String currentLine = lines.get(i);

			currentLine = currentLine.replace("|", ",");

			String[] lineData = currentLine.split(",");

			lineData = schemaGeneratorUtil.trimValue(lineData);

			if(Integer.valueOf(lineData[1]) > maxDepth){
				maxDepth = Integer.valueOf(lineData[1]);
			}

			//This is being done for any node in the complete schema.
			//A complex node can be at any level i.e. 1,2,3...
			if(lineData[3].equalsIgnoreCase(SchemaGeneratorConstants.CONSTANT_STRUCTURE) 
					|| lineData[3].equalsIgnoreCase(SchemaGeneratorConstants.CONSTANT_ARRAY_OF)
					|| lineData[3].equalsIgnoreCase(SchemaGeneratorConstants.CONSTANT_ONE_OF))  //example 'Array of Text50' -- will be an array but it won't have any children.
			{
				complexNodes.add(currentLine);
			}

			if(Integer.valueOf(lineData[1]) == 1 
					&& lineData[4].equalsIgnoreCase("M"))
			{
				levelOneRequiredElements.add("\"" + lineData[2] + "\"");
			}
		}

		int currentDepth = 1;

		while(currentDepth <= maxDepth)
		{ 
			for(int i=0;i<schemaSize;i++)
			{
				String currentLine = lines.get(i);

				currentLine = currentLine.replace("|", ",");

				String[] lineData = currentLine.split(",");

				lineData = schemaGeneratorUtil.trimValue(lineData);

				final NodeElements nodeElements = new NodeElements(lineData[1],lineData[2],lineData[3],lineData[4]);

				//Now finding the enum values
				if(lineData[3].equalsIgnoreCase(SchemaGeneratorConstants.CONSTANT_ENUM)
						|| lineData[3].replace(" ", "").equalsIgnoreCase(SchemaGeneratorConstants.CONSTANT_ARRAY_OF_ENUM))
				{
					
					final String enums = lines.get(i).substring(lines.get(i).toUpperCase().indexOf(SchemaGeneratorConstants.CONSTANT_ENUM_VALUES));
					final String[] enumElements = enums.substring(enums.indexOf("[") +1, enums.indexOf("]")).split(",");
					
					for(final String str : enumElements){
						nodeElements.addEnumElement(str.trim());
					}
				}

				if(!lineParsed.contains( i + currentLine))
				{

					if(Integer.valueOf(lineData[1]) == currentDepth)
					{
						lineParsed.add( i + currentLine);

						final Node<NodeElements> child = new Node<NodeElements>(nodeElements,rootNode);
						rootNode.addChild(child);
						currentHeadLevelMap.put(1, child);

					}else{
						if(Integer.valueOf(lineData[1])  == currentDepth + 1){

							lineParsed.add( i + currentLine);

							final Node<NodeElements> head = currentHeadLevelMap.get(1);
							final Node<NodeElements> child = new Node<NodeElements>(nodeElements,head);
							head.addChild(child);
						}
					}

				}else{
					if(Integer.valueOf(lineData[1]) == currentDepth  && complexNodes.contains(currentLine))
					{
						//need a way to fetch this 
						//The node that will now be head node has already been added... we have to extract that node from the tree?????
						final Node<NodeElements> parent = this.getParentNode(rootNode, lineData[1], lineData[2]);
						currentHeadLevelMap.put(1,parent);

						//Now in the next iteration, elements will be added as children of this node.
					}
				}

			}//end of for-loop

			currentDepth++;

		}//end of while-loop


	}//end of method 'populateTree'


	/**
	 * In the Node class for comparing, we must also add the line number
	 * @param rootNode Node<NodeElements>
	 * @param level String
	 * @param attributeName String
	 * @return Node<NodeElements>
	 */
	private Node<NodeElements> getParentNode(Node<NodeElements> rootNode, String level, String attributeName){

		Node<NodeElements> returnNode = null;

		for(final Node<NodeElements> currentNode: rootNode.getChildren())
		{
			if(currentNode.getNodeElements().getAttributeName().equals(attributeName)
					&& currentNode.getNodeElements().getLevel().equals(level))
			{
				returnNode =  currentNode;
				break;
			}

			returnNode = this.getParentNode(currentNode,level,attributeName);

			if(returnNode != null){
				break;
			}
		}

		return returnNode;
	}//end of method 'getParentNode'.


}//end of class 'Tree'
