/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.dataaccess.dbconnectionpool.handlersocket;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.google.code.hs4j.HSClient;
import com.google.code.hs4j.IndexSession;
import com.google.code.hs4j.impl.HSClientImpl;
import com.koala.game.dataaccess.dbconnectionpool.handlersocket.hstablestruct.HsField;
import com.koala.game.dataaccess.dbconnectionpool.handlersocket.hstablestruct.HsTable;
import com.koala.game.logging.KGameLogger;

/**
 * 
 * @author zhaizl
 */
public class HSConnectionPoolManager {
	private static final KGameLogger logger = KGameLogger
			.getLogger(HSConnectionPoolManager.class);

	private static HSConnectionPoolManager instance;
	private boolean isInitial = false;
	private HashMap<String, HsTable> hsTablesMap;
	private HashMap<String, IndexSession> hsIndexSessionMap;
	private HSClient hsClient;
	private String db_name;
	private String db_url;
	private int db_port;
	private int pool_size;
	private long connectionTimeout;

	public static HSConnectionPoolManager getInstance() {
		if (instance == null) {
			instance = new HSConnectionPoolManager();
		}
		return instance;
	}

	/**
	 * 初始化hs连接池，根据TableSturctConfig.xml配置文件配置表的数据结构、ip、port以及连接池大小
	 * 
	 * @throws Exception
	 */
	public void initHsConnectionPool() throws Exception {

		if (!isInitial) {
			initTables();

			System.out.println("---------------------------------------------------------------------------------------------------------------prepare init db, dbutl:" + db_url + ", port:" + db_port+ ", db_name:" + db_name);
			
			hsClient = new HSClientImpl(db_url, db_port, pool_size);
			hsClient.setOpTimeout(connectionTimeout);

			//初始化IndexSession的Map集合

			hsIndexSessionMap = new HashMap<String, IndexSession>();

			Iterator<HsTable> hsIt = hsTablesMap.values().iterator();
			while (hsIt.hasNext()) {
				HsTable table = hsIt.next();
//				System.out.println("table.getTableName()："
//						+ table.getTableName());

				IndexSession primaryIds = hsClient.openIndexSession(db_name,
						table.getTableName(), "PRIMARY", table.getFieldNames());

				hsIndexSessionMap
						.put("ids_" + table.getTableName(), primaryIds);

				for (int i = 0; i < table.getIndexKeyNames().length; i++) {
					
					try {
						IndexSession ids = hsClient.openIndexSession(db_name,
								table.getTableName(), table.getIndexKeyNames()[i],
								table.getFieldNames());
						hsIndexSessionMap.put("ids_" + table.getTableName() + "_"
								+ table.getIndexFieldNames()[i], ids);
					} catch (Exception e) {
						logger.error("open HS IndexSession error,table:{},indexKey:{}",table.getTableName(),table.getIndexKeyNames()[i],e);
						throw e;
					}
				}
			}

		}
	}

	public IndexSession getHsConnection(String idx_key) {
		return hsIndexSessionMap.get(idx_key);
	}
	
	public String getDBName() {
		return db_name;
	}

	/**
	 * 初始化hs连接池所需的数据库表结构的数据结构
	 * 
	 * @throws Exception
	 */
	private void initTables() throws Exception {
		hsTablesMap = new HashMap<String, HsTable>();

		SAXBuilder builder = new SAXBuilder();
		File file = new File("./res/config/dbconfig/TableSturctConfig.xml");
		Document doc = null;

		try {
			doc = builder.build(file);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		Element rootE = doc.getRootElement();

		db_name = rootE.getAttributeValue("name");
		db_url = rootE.getAttributeValue("ip");
		db_port = Integer.parseInt(rootE.getAttributeValue("port"));
		pool_size = Integer.parseInt(rootE.getAttributeValue("pool_size"));
		connectionTimeout = Integer.parseInt(rootE
				.getAttributeValue("connection_timeout"));

		List<Element> tableEList = rootE.getChildren("table");

		for (Element tableE : tableEList) {
			String tableName = tableE.getAttributeValue("name");
			String primaryKey = tableE.getAttributeValue("primary_key");
			List<Element> fieldEList = tableE.getChildren("field");
			HsField[] fields = new HsField[fieldEList.size()];
			for (int i = 0; i < fields.length; i++) {
				Element fieldE = fieldEList.get(i);
				String fieldName = fieldE.getAttributeValue("name");
				String fieldType = fieldE.getAttributeValue("type");
				fields[i] = new HsField(fieldName, fieldType);
			}

			List<Element> indexEList = tableE.getChildren("Index");
			String[] indexFieldName = new String[indexEList.size()];
			String[] indexKeyName = new String[indexEList.size()];
			for (int i = 0; i < indexFieldName.length; i++) {
				Element indexE = indexEList.get(i);
				indexKeyName[i] = indexE.getAttributeValue("name");
				indexFieldName[i] = indexE.getAttributeValue("fieldName");
			}
			HsTable table = new HsTable(tableName, primaryKey, indexKeyName,
					indexFieldName, fields);
			hsTablesMap.put(tableName, table);
		}
	}

	/**
	 * 关闭hs连接池
	 */
	private void shutDown() {
	}

	public static void main(String[] args) {
		HSConnectionPoolManager manager = new HSConnectionPoolManager();
		// try {
		// manager.initTables();
		// } catch (Exception ex) {
		// ex.printStackTrace();
		// }
		// Iterator<HsTable> tablesIt = manager.hsTablesMap.values().iterator();
		// while (tablesIt.hasNext()) {
		// HsTable table = tablesIt.next();
		// System.out.println("table:" + table.getTableName() + "  priKey:" +
		// table.getPrimaryKeyName()+"  idx_name:"+
		// table.getIndexKeyNames()[0]);;
		// }
		try {
			manager.initHsConnectionPool();
			Set<String> keys = manager.hsIndexSessionMap.keySet();

			for (String key : keys) {
				System.out.println("key:" + key + "  col:"
						+ manager.hsIndexSessionMap.get(key).getIndexId());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
