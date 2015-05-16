/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.dataaccess.dbconnectionpool.handlersocket.hstablestruct;

/**
 * 
 * @author Administrator
 */
public class HsTable {
	private String tableName;
	private String primaryKeyName;
	private String[] indexKeyNames;
	private String[] indexFieldNames;
	private HsField[] fields;

	public HsTable(String tableName, String primaryKeyName,
			String[] indexKeyNames, String[] indexFieldNames, HsField[] fields) {
		this.tableName = tableName;
		this.primaryKeyName = primaryKeyName;
		this.indexKeyNames = indexKeyNames;
		this.indexFieldNames = indexFieldNames;
		this.fields = fields;
	}

	public HsField[] getFields() {
		return fields;
	}

	public String[] getIndexKeyNames() {
		return indexKeyNames;
	}

	public String[] getIndexFieldNames() {
		return indexFieldNames;
	}

	public String getPrimaryKeyName() {
		return primaryKeyName;
	}

	public String getTableName() {
		return tableName;
	}

	public String[] getFieldNames() {
		String[] fieldNames = new String[fields.length];
		for (int i = 0; i < fieldNames.length; i++) {
			fieldNames[i] = fields[i].getFieldName();
		}
		return fieldNames;
	}
}
