/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.dataaccess;

import com.koala.game.dataaccess.dbconnectionpool.DBConnectionPoolAdapter;
import com.koala.game.dataaccess.impl.KGamePlayerManagerDataAccessImpl;
import com.koala.game.dataaccess.impl.KGamePlayerManagerDataAccessImpl.PlatformFlowDataSyncTask;

/**
 * 游戏逻辑层获取数据库访问接口KGameDataAccess实例的工厂类
 * 
 * @author zhaizl
 */
public class KGameDataAccessFactory {

	private static KGameDataAccessFactory instance;
	private KGamePlayerManagerDataAccess playerManagerDataAccess;
	
	// public CacheDataSyncManager syncManager;

	private KGameDataAccessFactory() {
	}

	public static KGameDataAccessFactory getInstance() {
		if (instance == null) {
			instance = new KGameDataAccessFactory();
		}
		return instance;
	}

//	public void init() throws Exception {
//		DBConnectionPoolAdapter.init();
//		if (logicDataAccess == null) {
//			logicDataAccess = new KGameLogicDataAccessImpl(
//					DBConnectionPoolAdapter.getHsConnectionPoolManager(),
//					DBConnectionPoolAdapter.getLogicDBConnectionPool(),
//					KGameLogicDataAccessImpl.DATA_ACCESS_MODE_HANDLERSOCKET);
//		}
//		if (playerManagerDataAccess == null) {
//			playerManagerDataAccess = new KGamePlayerManagerDataAccessImpl(
//					DBConnectionPoolAdapter.getPlatformDBConnectionPool());
//		}
//		KGameIdGeneratorFactory.getInstance().init();
//		// syncManager = new CacheDataSyncManager();
//		// syncManager.init();
//	}

	public void initPlatformDB() throws Exception {
		DBConnectionPoolAdapter.initPlatformDbPool();
		if (playerManagerDataAccess == null) {
			playerManagerDataAccess = new KGamePlayerManagerDataAccessImpl(
					DBConnectionPoolAdapter.getPlatformDBConnectionPool());
		}
	}

	public void initLogicDB(int mode) throws Exception {
		DBConnectionPoolAdapter.initLogicDbPool(mode);		
	}


	public KGamePlayerManagerDataAccess getPlayerManagerDataAccess() {
		return playerManagerDataAccess;
	}
		
	public void shutdownCache(){
		PlatformFlowDataSyncTask.isPrepareShutdown = true;
	}

}
