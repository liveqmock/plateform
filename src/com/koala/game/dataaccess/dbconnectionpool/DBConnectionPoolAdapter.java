/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.dataaccess.dbconnectionpool;

import com.koala.game.dataaccess.dbconnectionpool.handlersocket.HSConnectionPoolManager;
import com.koala.game.dataaccess.dbconnectionpool.mysql.DBConnectionFactory;
import com.koala.game.dataaccess.dbconnectionpool.mysql.DefineDataSourceManagerIF;
import com.koala.game.util.StringUtil;

/**
 * 数据库连接池管理器，可以通过方法{@link #getDBConnectionPool()}获取mysql连接池，通过
 * {@link #getHsConnectionPoolManager()} 获取handlerSocket连接池
 * 
 * @author Administrator
 */
public class DBConnectionPoolAdapter {
	
	/**
	 * 读数据采用HandlerSocket方式
	 */
	public final static int DATA_ACCESS_MODE_HANDLERSOCKET = 0;

	/**
	 * 读数据采用mysql方式
	 */
	public final static int DATA_ACCESS_MODE_MYSQL = 1;

	private static HSConnectionPoolManager hsMamager;
	private static final String logicDBConPoolUrl = "./res/config/dbconfig/proxool_pool_mysql.properties";
	private static final String platformDBConPoolUrl = "./res/config/dbconfig/proxool_pool_mysql_platform.properties";
	private static DefineDataSourceManagerIF logicDBConPool;
	private static DefineDataSourceManagerIF platformDBConPool;
	private static boolean isInitLogicPool = false;
	private static boolean isInitPlatformPool = false;

	/**
	 * 初始化逻辑DB的mysql和handlerSocket连接池
	 * 
	 * @throws Exception
	 */
	public static void initLogicDbPool(int mode) throws Exception {
		if (!isInitLogicPool) {
			if (mode == DATA_ACCESS_MODE_HANDLERSOCKET) {
				hsMamager = HSConnectionPoolManager.getInstance();
				hsMamager.initHsConnectionPool();
			}
			logicDBConPool = DBConnectionFactory.getInstance().newProxoolDataSourceInstance(logicDBConPoolUrl);
			if (hsMamager != null) {
				if (!hsMamager.getDBName().equals(logicDBConPool.getDBName())) {
					throw new RuntimeException(StringUtil.format("handler socket的db（{}）与mysql配置的db（{}）不一致！", hsMamager.getDBName(), logicDBConPool.getDBName()));
				}
			}
		}
	}
	
	/**
	 * 初始化平台DB的mysql和handlerSocket连接池
	 * 
	 * @throws Exception
	 */
	public static void initPlatformDbPool() throws Exception {
		if (!isInitPlatformPool) {
			platformDBConPool = DBConnectionFactory.getInstance()
					.newProxoolDataSourceInstance(platformDBConPoolUrl);
		}
	}

	public static HSConnectionPoolManager getHsConnectionPoolManager() {
		return hsMamager;
	}

	public static DefineDataSourceManagerIF getLogicDBConnectionPool() {
		return logicDBConPool;
	}

	public static DefineDataSourceManagerIF getPlatformDBConnectionPool() {
		return platformDBConPool;
	}

}
