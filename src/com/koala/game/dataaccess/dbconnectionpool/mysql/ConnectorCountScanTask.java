package com.koala.game.dataaccess.dbconnectionpool.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import com.koala.game.logging.KGameLogger;

/**
 * <pre>
 * 此监听管理器主要是处理程序运行过程中未正常关闭连接源引起的连接数爆满从而导致无法正常获取连接的问题。
 * 由任务调度设置任务运行周期时长；可能某些连接源在使用过程中需要较长时间处理，因此该扫描任务设置的连接过期时间较长(初定为30分钟)。
 * 当连接源被外部或此任务关闭时，必须从监听器中移除
 * 
 * <b>注意：</b>需要进行监听的数据源管理器必须先注册到该扫描任务中
 * </pre>
 * 
 * @author huangxc
 */
public class ConnectorCountScanTask extends TimerTask {
	private static final KGameLogger logger = KGameLogger
			.getLogger(ConnectorCountScanTask.class);

	private List<DefineDataSourceManagerIF> dataSourceList = new LinkedList<DefineDataSourceManagerIF>();

	/** 将一个连接池管理器注册到扫描任务中 */
	void registerMoniter(DefineDataSourceManagerIF dataSource) {
		dataSourceList.remove(dataSource);
		dataSourceList.add(dataSource);
	}

	@Override
	public void run() {
		if (dataSourceList.size() <= 0) { // 未注册数据源
			return;
		}
		DefineDataSourceManagerIF dataSource = null;
		for (int i = 0; i < dataSourceList.size(); i++) {
			dataSource = dataSourceList.get(i);
			try {
				scanDataSource(dataSource);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
//			System.out.println("============== [" + dataSource.getSourceName()
//					+ "]未正常关闭连接数["
//					+ DBConnectionFactory.unNormalCloseConnectorCount.get()
//					+ "] ==============");
			logger.info("============== [" + dataSource.getSourceName()
					+ "]未正常关闭连接数["
					+ DBConnectionFactory.unNormalCloseConnectorCount.get()
					+ "] ==============");
		}
	}

	/** 扫描数据源管理器 */
	private void scanDataSource(DefineDataSourceManagerIF dataSource)
			throws SQLException {
		Set<Connection> connSet = DefineDataSourceManagerIF.connectorMap
				.keySet();
		if (connSet == null || connSet.size() <= 0) {
			return;
		}
		Iterator<Connection> connIter = connSet.iterator();

		Connection conn = null;
		long time = 0;
		while (connIter.hasNext()) {
			conn = connIter.next();
			if (conn == null) { // 该连接已失效
				continue;
			}
			if (conn.isClosed()) { // 该连接已关闭
				DefineDataSourceManagerIF.connectorMap.remove(conn); // 从监听器中移除
				continue;
			}
			time = DefineDataSourceManagerIF.connectorMap.get(conn); // 获取连接创建的时间
			if (System.currentTimeMillis() - time >= DBConnectionFactory.SCAN_INTERVAL_TIME) { // 已经超过30分钟没有释放该连接
				if (!conn.getAutoCommit()) { // 正在进行事务操作，强制回滚
					conn.rollback();
				}
				dataSource.closeConnection(conn); // 强制关闭此连接
				// DefineDataSourceManagerIF.connectorMap.remove(conn); //
				// 从监听器中移除

				DefineDataSourceManagerIF.unNormalCloseConnectorCount
						.incrementAndGet();
			}
		}
	}
}