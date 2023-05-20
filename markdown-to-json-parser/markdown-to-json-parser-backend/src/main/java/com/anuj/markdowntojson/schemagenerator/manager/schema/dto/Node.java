package com.anuj.markdowntojson.schemagenerator.manager.schema.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author anujmehra
 *
 * @param <T>
 */
public class Node<T> {

	/**
	 * 
	 */
	private Node<T> parent = null;
	
	/**
	 * 
	 */
	private final List<Node<T>> children;
	
	/**
	 * 
	 */
	private final T nodeElements;

	/**
	 * 
	 */
	public Node(T nodeElements, Node<T> parent){

		children = new ArrayList<Node<T>>();

		this.parent = parent;
		this.nodeElements = nodeElements;
	}

	/**
	 * 
	 */
	public void addChild(Node<T> child)
	{
		children.add(child);
	}

	/**
	 * 
	 */
	public List<Node<T>> getChildren(){
		return children;
	}

	/**
	 * 
	 */
	public Node<T> getParent() {
		return parent;
	}

	/**
	 * 
	 */
	public void setParent(Node<T> parent) {
		this.parent = parent;
	}

	/**
	 * 
	 */
	public T getNodeElements(){
		return nodeElements;
	}

	/*public boolean equals(Object o){
		// If the object is compared with itself then return true  
		if (o == this) {
			return true;
		}

		 Check if o is an instance of Node or not
          "null instanceof [type]" also returns false 
		if (!(o instanceof Node)) {
			return false;
		}

		// typecast o to Complex so that we can compare data members 
		Node n = (Node) o;

		// Compare the data members and return accordingly 
		if((n.getNodeElements().getLevel().equals(this.getNodeElements().getLevel()))
				&& (n.getNodeElements().getAttributeName().equals(this.getNodeElements().getAttributeName()))){
			return true;
		}else{
			return false;
		}
	}*/

}
