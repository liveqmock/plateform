/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.dataaccess.dbconnectionpool.handlersocket.hstablestruct;

/**
 * 
 * @author Administrator
 */
public class HsField {
	public final static String FIELD_TYPE_NUMBER = "NUM";
	public final static String FIELD_TYPE_VERCHAR_OR_TEXT = "STRING";
	public final static String FIELD_TYPE_DATETIME = "DATETIME";
	public final static String FIELD_TYPE_FLOAT = "FLOAT";

	private String fieldName;
	private String fieldType;

	public HsField(String fieldName, String fieldType) {
		this.fieldName = fieldName;
		this.fieldType = fieldType;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getFieldType() {
		return fieldType;
	}

}
