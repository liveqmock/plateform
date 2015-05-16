package com.koala.game.dataaccess.dbconnectionpool.mysql;

import java.text.ParseException;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

import com.koala.game.dataaccess.dbconnectionpool.mysql.proxool.ProxoolDataSourceManagement;

public class DBConnectionFactory {

	private static DBConnectionFactory instance = null;
	static AtomicInteger unNormalCloseConnectorCount = new AtomicInteger();
	private ConnectorCountScanTask task = null;
	/** 任务中每个连接的最大活动时间 */
	static final long SCAN_INTERVAL_TIME = 1000 * 60 * 30; // 默认值30分钟，可重新配置
	/** 执行扫描任务的间隔时间 */
	private static final long TASK_INTERVAL_TIME = 1000 * 60 * 10; // 默认值10分钟，可重新配置

	private DBConnectionFactory() {
		try {
			startConnectorScanTask();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public static DBConnectionFactory getInstance() {
		if (instance == null) {
			instance = new DBConnectionFactory();
		}
		return instance;
	}

	/**
	 * 需要对连接源进行监控时必须显式启动该时效任务
	 * 
	 * @throws ParseException
	 */
	public void startConnectorScanTask() throws ParseException {
		Date date = new Date(System.currentTimeMillis());

		task = new ConnectorCountScanTask();
//		System.out.println("[" + task.getClass().getName() + "]此任务首次运行时间："
//				+ date + " 间隔时间：" + TASK_INTERVAL_TIME);
		Timer timer = new Timer();
		timer.schedule(task, date, TASK_INTERVAL_TIME);
	}

	/**
	 * 将需要进行监控的连接池注册到监控任务中
	 * 
	 * @param dataSource
	 *            连接池实例
	 */
	public void registerConnectorScanInstance(
			DefineDataSourceManagerIF dataSource) {
		task.registerMoniter(dataSource);
	}

	/**
	 * <pre>
	 * 获取默认Proxool数据源实例
	 * 默认路径：./resource/config/dbcp/proxool_datasource_config.properties
	 * </pre>
	 * 
	 * @return ProxoolDataSourceManagement Proxool数据源实例
	 */
	public DefineDataSourceManagerIF getDefaultProxoolDataSourceInstance() {
		return ProxoolDataSourceManagement.getInstance();
	}

	/**
	 * 根据指定配置文件路径获取Proxool数据源实例
	 * 
	 * @param config
	 *            String 相关配置文件的路径
	 * 
	 * @return ProxoolDataSourceManagement Proxool数据源实例 已存在时直接返回
	 *         不存在执行数据源初始化等一系列操作
	 */
	public DefineDataSourceManagerIF newProxoolDataSourceInstance(String config) {
		System.out.println("config path is " + config);
		return ProxoolDataSourceManagement.newInstance(config);
	}
}