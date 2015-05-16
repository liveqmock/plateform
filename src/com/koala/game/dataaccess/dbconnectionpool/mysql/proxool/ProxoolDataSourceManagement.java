package com.koala.game.dataaccess.dbconnectionpool.mysql.proxool;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.logicalcobwebs.proxool.ConnectionPoolDefinitionIF;
import org.logicalcobwebs.proxool.ConnectionPoolStatisticsIF;
import org.logicalcobwebs.proxool.ProxoolDataSource;
import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.ProxoolFacade;
import org.logicalcobwebs.proxool.configuration.PropertyConfigurator;

import com.koala.game.dataaccess.dbconnectionpool.mysql.DBConnectionFactory;
import com.koala.game.dataaccess.dbconnectionpool.mysql.DefineDataSourceManagerIF;
import com.koala.game.logging.KGameLogger;

/**
 * <pre>
 * 已处理的问题：
 * 1、多线程初始化同一别名的连接池保护
 * 2、非多线程，但同一配置文件的初始化
 * 3、非多线程，但同一别名的初始化
 * 
 * 未处理的问题(可处理，但实际上不应该出现)：
 * 1、不同别名，但连接源相同
 * 2、相同别名，但连接源不同
 * </pre>
 * 
 * @author huangxc
 */
public class ProxoolDataSourceManagement implements DefineDataSourceManagerIF {
	private static final KGameLogger logger = KGameLogger
			.getLogger(ProxoolDataSourceManagement.class);

	private static ProxoolDataSourceManagement instance = null;
	private String currentAliasName = "default_db";
	private ProxoolDataSource dataSource = null;
	private ConnectionPoolStatisticsIF cpsif = null;
	private boolean scanRunning = true;
	private String _dbName;

	// 默认配置文件的路径，可修改
	protected String default_config_path = "./resource/config/dbcp/proxool_datasource_config.properties";
	// 存在的意义：避免相同的配置文件重复注册连接池
	private final static Map<String, ProxoolDataSourceManagement> instanceMap = new ConcurrentHashMap<String, ProxoolDataSourceManagement>();
	/**
	 * 存在的意义：避免相同别名的连接池重复初始化连接源(别名相同但连接源不同的情况是低级错误，使用者不应犯此错误，这里不对此类情况作任何处理)，
	 * 暂时不处理别名不同但连接源相同的情况(各自管理独立的连接池)
	 */
	private final Map<String, ProxoolDataSource> dataSourceMap = new ConcurrentHashMap<String, ProxoolDataSource>();
	private final ConcurrentMap<String, FutureTask> taskMap = new ConcurrentHashMap<String, FutureTask>();

	private ProxoolDataSourceManagement() {
		loaderAliasName();
		start();
	}

	private ProxoolDataSourceManagement(String config) {
		default_config_path = config;
		loaderAliasName();
		start();
	}

	public static ProxoolDataSourceManagement getInstance() {
		if (instance == null) {
			instance = new ProxoolDataSourceManagement();
		}
		return instance;
	}

	public static ProxoolDataSourceManagement newInstance(String config) {
		ProxoolDataSourceManagement newInstance = instanceMap.get(config);
		if (newInstance == null) {
			logger.info("data source manager is null...execute init...");
			newInstance = new ProxoolDataSourceManagement(config);
			instanceMap.put(config, newInstance);
		}
		return newInstance;
	}

	/**
	 * <pre>
	 * 以别名为Key的数据源初始化方法
	 * 
	 * 为安全起见，此方法必须保证在同时有两个或以上线程对同一别名的连接池进行初始化时不会重复注册，
	 * 当某线程进入此验证方法时，处理的步骤是：
	 * 
	 * 1、检查是否已有其他线程正在对该连接池进行初始化(不同于注册)
	 * 2、当取得对连接源进行初始化的权限时验证是否由当前线程来执行(可能同时有其他线程取得权限)
	 * </pre>
	 */
	private void start() {
		FutureTask task = taskMap.get(currentAliasName); // 检查是否有其他线程正在对该别名的连接池进行初始化，这里命中率是非常低(评估比例大概为0.0001%甚至更小，接近0%)
		if (task == null) { // 夺得初始化权限(命中率接近100%)
			Callable call = new Callable() { // 执行初始化的方法

				@Override
				public Object call() throws Exception {
					initialConnectionPool(); // 初始化连接池
					taskMap.remove(currentAliasName);
					return null;
				}
			};
			task = new FutureTask(call); // 建立一个数据加载任务

			FutureTask target = taskMap.putIfAbsent(currentAliasName, task); // 是否由当前线程对数据源执行初始化(这里其实只做一个保护，执行的命中率接近100%)

			if (target != null) { // 此线程不需要对数据进行加载 这里命中率相当低(接近0%)
				task = target;
			} else {
				try { // 执行任务
					task.run();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		try {
			task.get(); // 等待注册完成
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		} catch (ExecutionException ex) {
			ex.printStackTrace();
		}
	}

	private void loaderAliasName() {
		FileInputStream is = null;
		try {
			is = new FileInputStream(default_config_path);
			Properties prop = new Properties();
			prop.load(is);
			is.close();
			Iterator<Object> iter = prop.keySet().iterator();
			while (iter.hasNext()) {
				String key = (String) iter.next();
				if (key.endsWith("alias")) {
					currentAliasName = prop.getProperty(key);
					break;
				}
			}
			logger.info("initial connection pool, alias name is "
					+ currentAliasName);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	private void initialConnectionPool() {
		dataSource = dataSourceMap.get(currentAliasName);
		if (dataSource != null) {
			logger.info("别名为：" + currentAliasName + "的数据库连接池已存在，不重新设置初始化......");
			return;
		}
		configConnectionPool();
	}

	@Override
	public String getSourceName() {
		return currentAliasName;
	}

	private void configConnectionPool() {
		dataSource = new ProxoolDataSource();
		dataSourceMap.put(currentAliasName, dataSource);
		try {
			PropertyConfigurator.configure(default_config_path);
			ConnectionPoolDefinitionIF cpdif = ProxoolFacade
					.getConnectionPoolDefinition(currentAliasName);

			dataSource.setAlias(cpdif.getAlias());
			dataSource.setDriver(cpdif.getDriver());
			dataSource.setDriverUrl(cpdif.getUrl());
			dataSource.setUser(cpdif.getUser());
			dataSource.setPassword(cpdif.getPassword());

			dataSource.setHouseKeepingSleepTime((int) cpdif
					.getHouseKeepingSleepTime());
			dataSource.setHouseKeepingTestSql(cpdif.getHouseKeepingTestSql());

			dataSource.setMaximumActiveTime(cpdif.getMaximumActiveTime());
			dataSource.setMaximumConnectionCount(cpdif
					.getMaximumConnectionCount());
			dataSource.setMinimumConnectionCount(cpdif
					.getMinimumConnectionCount());
			dataSource.setMaximumConnectionLifetime((int) cpdif
					.getMaximumConnectionLifetime());
			dataSource.setOverloadWithoutRefusalLifetime((int) cpdif
					.getOverloadWithoutRefusalLifetime());

			dataSource.setPrototypeCount(cpdif.getPrototypeCount());
			dataSource.setRecentlyStartedThreshold((int) cpdif
					.getRecentlyStartedThreshold());
			dataSource.setSimultaneousBuildThrottle(cpdif
					.getSimultaneousBuildThrottle());

			dataSource.setStatisticsLogLevel(cpdif.getStatisticsLogLevel());
			dataSource.setTrace(cpdif.isTrace());

			dataSource.setTestBeforeUse(true);
			dataSource.setTestAfterUse(true);
			_dbName = dataSource.getDriverUrl().substring(
					dataSource.getDriverUrl().lastIndexOf("/") + 1);
		} catch (ProxoolException ex) {
			ex.printStackTrace();
		}

		onListener();
	}

	@Override
	public void onListener() {
		try {
			cpsif = ProxoolFacade.getConnectionPoolStatistics(currentAliasName);
		} catch (ProxoolException ex) {
			ex.printStackTrace();
		}
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (scanRunning) {
					int activeCount = cpsif.getActiveConnectionCount();
					int availableCount = cpsif.getAvailableConnectionCount();
					long totalCount = cpsif.getConnectionCount();

					// System.out.println("\r\n当前数据库：" + currentAliasName
					// + "\r\n已分配总连接数：" + totalCount + "  使用中的连接数："
					// + activeCount + "  可用连接数：" + availableCount);
					logger.info("\r\n当前数据库：" + currentAliasName
							+ "\r\n已分配总连接数：" + totalCount + "  使用中的连接数："
							+ activeCount + "  可用连接数：" + availableCount + 
							"  ，connectorMap size："+DefineDataSourceManagerIF.connectorMap.size());
					try {
						Thread.sleep(6000 * 5);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
			}
		}).start();
	}

	@Override
	public Connection getConnection() {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		if (conn != null) {
			connectorMap.put(conn, System.currentTimeMillis());
		} else {
			for (int i = 0; i < 5; i++) {

				try {
					conn = dataSource.getConnection();
				} catch (Exception ex) {
					logger.error(ex.getMessage(), ex);
				}
				if (conn != null) {
					connectorMap.put(conn, System.currentTimeMillis());
					return conn;
				}
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		return conn;
	}

	@Override
	public PreparedStatement setResultSetScrollProperty(Connection conn,
			String sql) throws SQLException {
		return conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
	}

	@Override
	public Statement setResultSetScrollProperty(Connection conn)
			throws SQLException {
		return conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
	}

	@Override
	public PreparedStatement writeStatement(Connection conn, String sql)
			throws SQLException {
		return conn.prepareStatement(sql);
	}

	@Override
	public Statement writeStatement(Connection conn) throws SQLException {
		return conn.createStatement();
	}

	@Override
	public PreparedStatement returnGeneratedPrimaryKey(Connection conn,
			String sql) throws SQLException {
		return conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	}

	@Override
	public ResultSet executeQuery(PreparedStatement pstmt) throws SQLException {
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()) {
			rs.previous();
		} else {
			return null;
		}
		return rs;
	}

	@Override
	public ResultSet executeQuery(Statement stmt, String sql)
			throws SQLException {
		ResultSet rs = stmt.executeQuery(sql);
		if (rs.next()) {
			rs.previous();
		} else {
			return null;
		}
		return rs;
	}

	@Override
	public int executeUpdate(PreparedStatement pstmt) throws SQLException {
		return pstmt.executeUpdate();
	}

	@Override
	public int executeUpdate(Statement stmt, String sql) throws SQLException {
		return stmt.executeUpdate(sql);
	}

	@Override
	public int[] executeBatchUpdate(Statement stmt) throws SQLException {
		int[] result = stmt.executeBatch();
		return result;
	}

	@Override
	public int[] executeBatchUpdate(PreparedStatement pstmt)
			throws SQLException {
		int[] result = pstmt.executeBatch();
		return result;
	}

	@Override
	public void setQueryTimeout(Statement stmt, int seconds)
			throws SQLException {
		stmt.setQueryTimeout(seconds);
	}

	@Override
	public void setQueryTimeout(PreparedStatement pstmt, int seconds)
			throws SQLException {
		pstmt.setQueryTimeout(seconds);
	}

	@Override
	public void closeResultSet(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			rs = null;
		}
	}

	@Override
	public void closeStatement(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			stmt = null;
		}
	}

	@Override
	public void closePreparedStatement(PreparedStatement pstmt) {
		if (pstmt != null) {
			try {
				pstmt.close();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			pstmt = null;
		}
	}

	@Override
	public void closeConnection(Connection conn) {
		try {
			if (conn != null && !conn.isClosed()) {
				conn.close();
				DefineDataSourceManagerIF.connectorMap.remove(conn);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void closeDataSources(Statement stmt, Connection conn) {
		try {
			if (stmt != null) {
				stmt.close();
				stmt = null;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		closeConnection(conn);
	}

	@Override
	public void closeDataSources(PreparedStatement pstmt, Connection conn) {
		try {
			if (pstmt != null) {
				pstmt.close();
				pstmt = null;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		closeConnection(conn);
	}

	@Override
	public String getDBName() {
		return _dbName;
	}

	public static void main(String[] args) {
		DefineDataSourceManagerIF dataSourceMysql = DBConnectionFactory
				.getInstance().newProxoolDataSourceInstance(
						"./resource/dbconfig/rdb_proxool.properties");
		System.out.println(dataSourceMysql.getConnection());

		DefineDataSourceManagerIF dataSourceH2 = DBConnectionFactory
				.getInstance().newProxoolDataSourceInstance(
						"./resource/dbconfig/mdb_proxool.properties");
		System.out.println(dataSourceH2.getConnection());
	}

	@Override
	public void shutdown() {
		scanRunning = false;
	}
}