package com.koala.game.dataaccess.dbconnectionpool.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public interface DefineDataSourceManagerIF {

	/**
	 * <pre>
	 * 连接源映射表
	 * 主要用于避免在运行过程中存在某些连接无法关闭或忘记关闭造成连接数爆满问题
	 * 由连接数扫描任务对这些数据库连接进行监听管理
	 * 
	 * <b>注意：</b>获取连接时必须将连接存放到该映射表中
	 * 
	 * key : 连接源对象  value : 连接源建立时间
	 * </pre>
	 */
	public final ConcurrentHashMap<Connection, Long> connectorMap = new ConcurrentHashMap<Connection, Long>();
	/**
	 * 记录非正常关闭的连接总数(只记录相应连接池的连接源)
	 */
	public final AtomicInteger unNormalCloseConnectorCount = new AtomicInteger();

	/**
	 * 获取该连接池数据源的名字
	 * 
	 * @return String
	 */
	public String getSourceName();

	/**
	 * 获取一个连接源
	 * 
	 * @return Connection
	 */
	public Connection getConnection();

	/**
	 * 打开启连接池监听
	 */
	public void onListener();

	/**
	 * <pre>
	 * 创建一个可滚动的预执行语句
	 * <b>注意：</b>一般用于查询结果集
	 * </pre>
	 * 
	 * @param conn
	 *            连接源
	 * @param sql
	 *            预执行的SQL语句
	 * 
	 * @return PreparedStatement
	 * 
	 * @throws SQLException
	 *             数据库异常
	 */
	public PreparedStatement setResultSetScrollProperty(Connection conn,
			String sql) throws SQLException;

	/**
	 * 创建一个可滚动的执行语句
	 * 
	 * @param conn
	 *            连接源
	 * 
	 * @return Statement
	 * 
	 * @throws SQLException
	 *             数据库异常
	 */
	public Statement setResultSetScrollProperty(Connection conn)
			throws SQLException;

	/**
	 * 创建一个用于写操作的执行语句(用于更新或插入操作)
	 * 
	 * @param conn
	 *            连接源
	 * @param sql
	 *            预执行语句
	 * 
	 * @return PrepardStatement
	 * 
	 * @throws SQLException
	 *             数据库异常
	 */
	public PreparedStatement writeStatement(Connection conn, String sql)
			throws SQLException;

	/**
	 * 创建一个用于写操作的执行语句(用于更新或插入操作)
	 * 
	 * @param conn
	 *            连接源
	 * 
	 * @return Statement
	 * 
	 * @throws SQLException
	 *             数据库异常
	 */
	public Statement writeStatement(Connection conn) throws SQLException;

	/**
	 * 创建一个可返回主键的预执行语句
	 * 
	 * @param conn
	 *            连接源
	 * @param sql
	 *            预执行的SQL语句
	 * 
	 * @return PreparedStatement
	 * 
	 * @throws SQLException
	 *             数据库异常
	 */
	public PreparedStatement returnGeneratedPrimaryKey(Connection conn,
			String sql) throws SQLException;

	/**
	 * 执行预加载的查询
	 * 
	 * @param pstmt
	 *            预加载的语句
	 * 
	 * @return ResultSet
	 * 
	 * @throws SQLException
	 *             数据库异常
	 */
	public ResultSet executeQuery(PreparedStatement pstmt) throws SQLException;

	/**
	 * 执行查询语句
	 * 
	 * @param stmt
	 *            可执行的语句
	 * @param sql
	 *            查询SQL语句
	 * 
	 * @return ResultSet
	 * 
	 * @throws SQLException
	 *             数据库异常
	 */
	public ResultSet executeQuery(Statement stmt, String sql)
			throws SQLException;

	/**
	 * 执行预加载的写操作
	 * 
	 * @param pstmt
	 *            预加载写语句
	 * 
	 * @return int
	 * 
	 * @throws SQLException
	 *             数据库异常
	 */
	public int executeUpdate(PreparedStatement pstmt) throws SQLException;

	/**
	 * 
	 * @param stmt
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public int executeUpdate(Statement stmt, String sql) throws SQLException;

	/**
	 * 
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public int[] executeBatchUpdate(Statement stmt) throws SQLException;

	/**
	 * 
	 * @param pstmt
	 * @return
	 * @throws SQLException
	 */
	public int[] executeBatchUpdate(PreparedStatement pstmt)
			throws SQLException;

	/**
	 * 
	 * @param stmt
	 * @param seconds
	 * @throws SQLException
	 */
	public void setQueryTimeout(Statement stmt, int seconds)
			throws SQLException;

	/**
	 * 
	 * @param pstmt
	 * @param seconds
	 * @throws SQLException
	 */
	public void setQueryTimeout(PreparedStatement pstmt, int seconds)
			throws SQLException;

	/**
	 * 
	 * @param rs
	 */
	public void closeResultSet(ResultSet rs);

	/**
	 * 
	 * @param stmt
	 */
	public void closeStatement(Statement stmt);

	/**
	 * 
	 * @param pstmt
	 */
	public void closePreparedStatement(PreparedStatement pstmt);

	/**
	 * 关闭连接时必须从监听器中移除该连接保持的空间
	 * 
	 * @param conn
	 */
	public void closeConnection(Connection conn);

	/**
	 * 关闭连接时必须从监听器中移除该连接保持的空间
	 * 
	 * @param stmt
	 * @param conn
	 */
	public void closeDataSources(Statement stmt, Connection conn);

	/**
	 * 关闭连接时必须从监听器中移除该连接保持的空间
	 * 
	 * @param pstmt
	 *            预加载语句
	 * @param conn
	 *            数据连接源
	 */
	public void closeDataSources(PreparedStatement pstmt, Connection conn);
	
	/**
	 * 
	 * 获取数据库的名字
	 * 
	 * @return
	 */
	public String getDBName();

	public void shutdown();
}