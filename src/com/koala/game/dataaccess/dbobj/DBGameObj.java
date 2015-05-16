/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.dataaccess.dbobj;

/**
 * 
 * @author zhaizl
 */
public abstract class DBGameObj {

	/**
	 * 表示该DBGameObj的DB同步数据操作类型为NONE，不作任何操作
	 */
	public final static int OP_FLAG_NONE = 0;
	/**
	 * 表示该DBGameObj的DB同步数据操作类型为插入数据操作
	 */
	public final static int OP_FLAG_INSERT = 1;
	/**
	 * 表示该DBGameObj的DB同步数据操作类型为更新数据操作
	 */
	public final static int OP_FLAG_UPDATE = 2;
	/**
	 * 表示该DBGameObj的DB同步数据操作类型为删除数据操作
	 */
	public final static int OP_FLAG_DELETE = 3;
	private int op_flag;

	/**
	 * 获取数据库操作位的值：
	 * 
	 * @return <pre>
	 * {@link com.kaola.game.dataaccess.dbobj.DBGameObj#OP_FLAG_NONE} ||
	 * {@link com.kaola.game.dataaccess.dbobj.DBGameObj#OP_FLAG_INSERT} ||
	 * {@link com.kaola.game.dataaccess.dbobj.DBGameObj#OP_FLAG_UPDATE} ||
	 * {@link com.kaola.game.dataaccess.dbobj.DBGameObj#OP_FLAG_DELETE}
	 * </pre>
	 */
	public int getDbOperatingFlag() {
		return op_flag;
	}

	/**
	 * 设置数据库操作位的值：
	 * 
	 * @param flag
	 *            该值只能为：
	 * 
	 *            <pre>
	 * {@link com.kaola.game.dataaccess.dbobj.DBGameObj#OP_FLAG_NONE} ||
	 * {@link com.kaola.game.dataaccess.dbobj.DBGameObj#OP_FLAG_INSERT} ||
	 * {@link com.kaola.game.dataaccess.dbobj.DBGameObj#OP_FLAG_UPDATE} ||
	 * {@link com.kaola.game.dataaccess.dbobj.DBGameObj#OP_FLAG_DELETE}
	 * </pre>
	 */
	public void setDbOperatingFlag(int flag) {
		this.op_flag = flag;
	}
	
	/**
	 * 
	 * @param id
	 */
	public abstract void setId(long id);
}
