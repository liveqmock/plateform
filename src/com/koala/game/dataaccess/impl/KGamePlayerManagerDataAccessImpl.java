package com.koala.game.dataaccess.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;

import com.koala.game.KGame;
import com.koala.game.dataaccess.InvalidUserInfoException;
import com.koala.game.dataaccess.KGameDBException;
import com.koala.game.dataaccess.KGamePlayerManagerDataAccess;
import com.koala.game.dataaccess.PlayerAuthenticateException;
import com.koala.game.dataaccess.dbconnectionpool.mysql.DefineDataSourceManagerIF;
import com.koala.game.dataaccess.dbobj.DBPlayer;
import com.koala.game.dataaccess.dbobj.flowdata.DBChargeRecord;
import com.koala.game.dataaccess.dbobj.flowdata.DBPresentPointRecord;
import com.koala.game.exception.KGameServerException;
import com.koala.game.logging.KGameLogger;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimerTask;

public class KGamePlayerManagerDataAccessImpl extends DataAccessTools implements
		KGamePlayerManagerDataAccess {

	private static final KGameLogger logger = KGameLogger
			.getLogger(KGamePlayerManagerDataAccessImpl.class);

	private DefineDataSourceManagerIF platformDBConPool;

	private static ConcurrentHashMap<Long, LoginRecod> loginRecordMap;

	// private int serverId;

	public static PlatformFlowDataSyncTask syncTask;
	
	private static final List<Character> _ALLOW_CHARS = Arrays.asList('@', '.');
	
	private static int _MIN_LENGTH_OF_PLAYER_NAME = 4;
	private static int _MAX_LENGTH_OF_PLAYER_NAME = 50;

	private static int _MIN_LENGTH_OF_PASSWORD = 4;
	private static int _MAX_LENGTH_OF_PASSWORD = 18;
	
	private static boolean _LOGIN_CHECK_PLAYER_NAME = false;
	
	public KGamePlayerManagerDataAccessImpl(
			DefineDataSourceManagerIF platformDBConPool) {
		this.platformDBConPool = platformDBConPool;
		loginRecordMap = new ConcurrentHashMap<Long, KGamePlayerManagerDataAccessImpl.LoginRecod>();
		syncTask = new PlatformFlowDataSyncTask(platformDBConPool);
	}

	@Override
	public long registerNewPassport(String playerName, String password,
			int promoID, int securityQuestionIdx, int securityAnswerIdx,
			String remark) throws KGameDBException {

		return 0;
	}

	@Override
	public DBPlayer registerNewPassport(String playerName, String password,
			String mobileNum, int promoID, int parentPromoId,
			int securityQuestionIdx, String securityAnswer, String attribute,
			String remark) throws InvalidUserInfoException, KGameDBException {
		long[] restTime = initRestTime();

		checkPlayerName(playerName);
		checkPassword(password);

		String proMask = promoID + "_" + playerName;

		String sql = "insert into player(player_name,promo_mask,password,mobile_num,register_time,security_question_idx,"
				+ "security_answer,promo_id,parent_promo_id,remark,attribute) values('"
				+ playerName.toLowerCase()
				+ "','"
				+ proMask.toLowerCase()
				+ "','"
				+ password.toLowerCase()
				+ "',"
				+ (mobileNum == null ? "" : "'" + mobileNum + "'")
				+ ",?,"
				+ securityQuestionIdx
				+ ","
				+ (securityAnswer == null ? "" : "'" + securityAnswer + "'")
				+ ","
				+ promoID
				+ ","
				+ parentPromoId
				+ ","
				+ (remark == null ? "" : "'" + remark + "'")
				+ ","
				+ (attribute == null ? "" : "'" + attribute + "'") + ")";

		Connection con = null;
		con = platformDBConPool.getConnection();

		checkAppliedUserName(playerName, con, restTime);

		this.getRestTimeout(restTime);

		long playerId;

		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			platformDBConPool.setQueryTimeout(ps,
					this.getStatmentTimeout(restTime));
			long createTime = System.currentTimeMillis();
			ps.setTimestamp(1, new Timestamp(createTime));

			if (platformDBConPool.executeUpdate(ps) == 1) {
				rs = ps.getGeneratedKeys();
				rs.next();
				playerId = rs.getLong(1);
				DBPlayer player = new DBPlayer();
				player.setPlayerId(playerId);
				player.setPlayerName(playerName);
				player.setPassword(password);
				player.setMobileNum(mobileNum);
				player.setCreateTimeMillis(createTime);
				player.setPromoId(promoID);
				player.setParentPromoId(parentPromoId);
				player.setPromoMask(proMask);
				player.setAttribute(attribute);
				player.setRemark(remark);
				return player;
			} else {
				throw new KGameDBException("数据库发生未知异常", null,
						KGameDBException.CAUSE_DB_SHUTDOWN_WITH_UNKNOW_REASON,
						"注册账号失败，数据库发生未知异常");
			}

		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closeResultSet(rs);
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}

	}

	@Override
	public DBPlayer registerNewPassportByPromoMask(String promoMask,
			String mobileNum, int promoID, int parentPromoId,
			int securityQuestionIdx, String securityAnswer, String attribute,
			String remark) throws InvalidUserInfoException, KGameDBException {
		long[] restTime = initRestTime();

		// checkPassword(password);

		String playerName = parentPromoId + "_" + promoMask;
		String password = (new Random().nextInt(999999 - 100000) + 100000) + "";

		String sql = "insert into player(player_name,promo_mask,password,mobile_num,register_time,security_question_idx,"
				+ "security_answer,promo_id,parent_promo_id,remark,attribute) values('"
				+ playerName.toLowerCase()
				+ "','"
				+ promoMask.toLowerCase()
				+ "','"
				+ password.toLowerCase()
				+ "',"
				+ (mobileNum == null ? "" : "'" + mobileNum + "'")
				+ ",?,"
				+ securityQuestionIdx
				+ ","
				+ (securityAnswer == null ? "" : "'" + securityAnswer + "'")
				+ ","
				+ promoID
				+ ","
				+ parentPromoId
				+ ","
				+ (remark == null ? "" : "'" + remark + "'")
				+ ","
				+ (attribute == null ? "" : "'" + attribute + "'") + ")";

		Connection con = null;
		con = platformDBConPool.getConnection();

		// checkAppliedUserName(playerName, con, restTime);

		// this.getRestTimeout(restTime);

		long playerId = checkAppliedPromoMask(promoMask, parentPromoId, con,
				restTime);

		if (playerId != -1) {
			return null;
		}

		this.getRestTimeout(restTime);

		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			platformDBConPool.setQueryTimeout(ps,
					this.getStatmentTimeout(restTime));
			long createTime = System.currentTimeMillis();
			ps.setTimestamp(1, new Timestamp(createTime));

			if (platformDBConPool.executeUpdate(ps) == 1) {
				rs = ps.getGeneratedKeys();
				rs.next();
				playerId = rs.getLong(1);
				DBPlayer player = new DBPlayer();
				player.setPlayerId(playerId);
				player.setPlayerName(playerName);
				player.setPassword(password);
				player.setMobileNum(mobileNum);
				player.setCreateTimeMillis(createTime);
				player.setPromoId(promoID);
				player.setParentPromoId(parentPromoId);
				player.setPromoMask(promoMask);
				player.setAttribute(attribute);
				player.setRemark(remark);
				return player;
			} else {
				throw new KGameDBException("数据库发生未知异常", null,
						KGameDBException.CAUSE_DB_SHUTDOWN_WITH_UNKNOW_REASON,
						"注册账号失败，数据库发生未知异常");
			}

		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closeResultSet(rs);
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}
	}

	@Override
	public boolean login(String pname, String password)
			throws PlayerAuthenticateException, KGameDBException {
		return false;
	}

	@Override
	public boolean login(long pid, String password)
			throws PlayerAuthenticateException, KGameDBException {

		return false;
	}

	@Override
	public DBPlayer loginByName(String pname, String password)
			throws InvalidUserInfoException, PlayerAuthenticateException,
			KGameDBException {

		if (_LOGIN_CHECK_PLAYER_NAME) {
			checkPlayerName(pname);
		}
		checkPassword(password);

		String sql = "select * from player where player_name = '"
				+ pname.toLowerCase() + "'";

		return processPlayerLogin(sql, password.toLowerCase(), true);
	}

	@Override
	public DBPlayer loginByPromoMask(String promoMask, int promoId)
			throws InvalidUserInfoException, PlayerAuthenticateException,
			KGameDBException {
		String sql = "select * from player where promo_mask = '"
				+ promoMask.toLowerCase() + "' and promo_id = " + promoId;
		return processPlayerLogin(sql, "", false);
	}

	@Override
	public DBPlayer loginById(long pid, String password)
			throws InvalidUserInfoException, PlayerAuthenticateException,
			KGameDBException {
		checkPassword(password);

		String sql = "select * from player where player_id = " + pid;

		return processPlayerLogin(sql, password.toLowerCase(), true);
	}

	@Override
	public void logout(long playerId) throws PlayerAuthenticateException,
			KGameDBException {

		Connection con = null;
		PreparedStatement ps = null;

		long onlineTime = 0;
		long logoutTime = System.currentTimeMillis();
		if (loginRecordMap.containsKey(playerId)) {
			LoginRecod record = loginRecordMap.get(playerId);
			record.logoutTime = logoutTime;
			record.onlineTime = (record.logoutTime - record.loginTime);
			syncTask.addLogoutRecordQueue(record);
			onlineTime = record.onlineTime;
		}

		String updatePlayerSql = "update player set lastest_logout_time = ?,"
				+ "total_online_time = total_online_time + " + onlineTime
				+ " where player_id = ?";
		// String insertLoginRecordSql =
		// "insert into login_record(player_id,login_time,logout_time,online_time,"
		// + "server_id) values (?,?,?,?,?)";

		long[] restTime = initRestTime();

		try {
			con = platformDBConPool.getConnection();
			ps = con.prepareStatement(updatePlayerSql);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));

			ps.setTimestamp(1, new Timestamp(logoutTime));
			ps.setLong(2, playerId);
			ps.executeUpdate();

		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}

	}

	@Override
	public void checkDuplicationOfUserName(String appliedUserName)
			throws InvalidUserInfoException, KGameDBException {

		checkPlayerName(appliedUserName);

		Connection con = null;
		try {
			con = platformDBConPool.getConnection();

			checkAppliedUserName(appliedUserName, con, initRestTime());
		} finally {
			platformDBConPool.closeConnection(con);
		}
	}

	@Override
	public String reclaimPasswordOf(String userName, int questionId,
			String answer) throws PlayerAuthenticateException, KGameDBException {

		String sql = "select player_name,password,security_question_idx,security_answer from player where player_name = '"
				+ userName.toLowerCase() + "'";

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		long[] restTime = initRestTime();

		con = platformDBConPool.getConnection();

		try {
			ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));
			rs = platformDBConPool.executeQuery(ps);

			if (rs != null) {
				rs.next();
				String dbAnswer = rs.getString("security_answer");
				int dbQuestionId = rs.getInt("security_question_idx");
				if (!(answer.toLowerCase()).equals(dbAnswer.toLowerCase())
						|| dbQuestionId != questionId) {
					throw new PlayerAuthenticateException("问题对应的答案错误！", null,
							PlayerAuthenticateException.CAUSE_UNMATCHED_ANSWER,
							"验证问题对应的答案错误，请输入正确的答案。");
				} else {
					return rs.getString("password");
				}
			} else {
				throw new PlayerAuthenticateException("该账号不存在！", null,
						PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND,
						"该账号不存在，请使用别的账号进行登录。");
			}
		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closeResultSet(rs);
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}

	}

	@Override
	public DBPlayer loadDBPlayer(String playerName) throws KGameDBException {

		String sql = "select * from player where player_name = '"
				+ playerName.toLowerCase() + "'";

		DBPlayer dbPlayer = null;

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		long[] restTime = initRestTime();

		try {
			con = platformDBConPool.getConnection();
			ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));
			rs = platformDBConPool.executeQuery(ps);
			if (rs != null) {
				rs.next();

				dbPlayer = new DBPlayer();

				dbPlayer.setPlayerId(rs.getLong("player_id"));
				dbPlayer.setPlayerName(rs.getString("player_name"));
				dbPlayer.setPassword(rs.getString("password"));
				dbPlayer.setType(rs.getInt("type"));
				dbPlayer.setCreateTimeMillis(rs.getTimestamp("register_time") != null ? rs
						.getTimestamp("register_time").getTime() : 0);
				dbPlayer.setLastestLoginTimeMillis(System.currentTimeMillis());
				dbPlayer.setLastestLogoutTimeMillis(rs
						.getTimestamp("lastest_logout_time") != null ? rs
						.getTimestamp("lastest_logout_time").getTime() : 0);
				dbPlayer.setMobileNum(rs.getString("mobile_num"));
				dbPlayer.setTotalLoginCount(rs.getLong("total_login_count") + 1);
				dbPlayer.setTotalOnlineTimeMillis(rs
						.getLong("total_online_time"));
				dbPlayer.setPromoId(rs.getInt("promo_id"));
				dbPlayer.setSecurityQuestionIdx(rs
						.getInt("security_question_idx"));
				dbPlayer.setSecurityAnswerIdx(rs.getInt("security_answer_idx"));
				dbPlayer.setSecurityAnswer(rs.getString("security_answer"));
				dbPlayer.setRemark(rs.getString("remark"));
				dbPlayer.setAttribute(rs.getString("attribute"));
				dbPlayer.setParentPromoId(rs.getInt("parent_promo_id"));

				return dbPlayer;

			} else {
				throw new KGameDBException("该账号不存在！", null,
						KGameDBException.CAUSE_RECORD_NOT_EXIST, "需要加载的账号不存在。");
			}
		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closeResultSet(rs);
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);

		}
	}

	@Override
	public boolean changePassword(String playerName, String oldPassword,
			String newPassword) throws InvalidUserInfoException,
			PlayerAuthenticateException, KGameDBException {
		checkPlayerName(playerName);
		checkPassword(oldPassword);
		checkPassword(newPassword);

		String sql = "select player_name,password from player where player_name = '"
				+ playerName.toLowerCase() + "'";
		String updateSql = "update player set password = " + newPassword
				+ " where player_name = '" + playerName + "'";

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		long[] restTime = initRestTime();

		con = platformDBConPool.getConnection();

		try {
			ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));
			rs = platformDBConPool.executeQuery(ps);

			if (rs != null) {
				rs.next();
				String dbPassword = rs.getString("password");
				if (!(oldPassword.toLowerCase()).equals(dbPassword
						.toLowerCase())) {
					throw new PlayerAuthenticateException("验证密码错误！", null,
							PlayerAuthenticateException.CAUSE_WRONG_PASSWORD,
							"验证密码错误，请输入正确的密码。");
				} else {
					rs.close();
					ps.close();

					getRestTimeout(restTime);

					ps = con.prepareStatement(updateSql);
					platformDBConPool.setQueryTimeout(ps,
							getStatmentTimeout(restTime));
					ps.execute();

					return true;
				}
			} else {
				throw new PlayerAuthenticateException("该账号不存在！", null,
						PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND,
						"该账号不存在，请使用别的账号进行登录。");
			}
		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closeResultSet(rs);
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}
	}

	@Override
	public boolean changeSecurityQuestionAnswer(long playerId,
			int securityQuestionIdx, String securityAnswer)
			throws PlayerAuthenticateException, KGameDBException {

		return false;
	}

	@Override
	public void verifyPlayerPassport(String playerName, String password)
			throws PlayerAuthenticateException, KGameDBException {

		String sql = "select player_name,password from player where player_name = '"
				+ playerName.toLowerCase() + "'";

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		long[] restTime = initRestTime();

		con = platformDBConPool.getConnection();

		try {
			ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));
			rs = platformDBConPool.executeQuery(ps);

			if (rs != null) {
				rs.next();
				String dbPassword = rs.getString("password");
				if (!(password.toLowerCase()).equals(dbPassword.toLowerCase())) {
					throw new PlayerAuthenticateException("该账号密码错误！", null,
							PlayerAuthenticateException.CAUSE_WRONG_PASSWORD,
							"验证密码错误，请输入正确的密码。");
				}
			} else {
				throw new PlayerAuthenticateException("该账号不存在！", null,
						PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND,
						"该账号不存在，请使用别的账号进行登录。");
			}
		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closeResultSet(rs);
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}
	}

	@Override
	public DBPlayer verifyPlayerPassportByPromoMask(String promoMask,
			int parentPromoId) throws PlayerAuthenticateException,
			KGameDBException {
		// String sql = "select * from player where promo_mask = '"
		// + promoMask.toLowerCase() + "' and promo_id = " + promoId;
		String sql = "select * from player where promo_mask = '"
				+ promoMask.toLowerCase() + "' and parent_promo_id = "
				+ parentPromoId; // 2013-10-13 改用parent_promo_id

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		long[] restTime = initRestTime();

		con = platformDBConPool.getConnection();

		try {
			ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));
			rs = platformDBConPool.executeQuery(ps);

			if (rs != null && rs.next()) {
				DBPlayer dbPlayer = new DBPlayer();

				dbPlayer.setPlayerId(rs.getLong("player_id"));
				dbPlayer.setPlayerName(rs.getString("player_name"));
				dbPlayer.setPromoMask(rs.getString("promo_mask"));
				dbPlayer.setPassword(rs.getString("password"));
				dbPlayer.setType(rs.getInt("type"));
				dbPlayer.setCreateTimeMillis(rs.getTimestamp("register_time") != null ? rs
						.getTimestamp("register_time").getTime() : 0);
				dbPlayer.setLastestLoginTimeMillis(System.currentTimeMillis());
				dbPlayer.setLastestLogoutTimeMillis(rs
						.getTimestamp("lastest_logout_time") != null ? rs
						.getTimestamp("lastest_logout_time").getTime() : 0);
				dbPlayer.setMobileNum(rs.getString("mobile_num"));
				dbPlayer.setTotalLoginCount(rs.getLong("total_login_count") + 1);
				dbPlayer.setTotalOnlineTimeMillis(rs
						.getLong("total_online_time"));
				dbPlayer.setPromoId(rs.getInt("promo_id"));
				dbPlayer.setSecurityQuestionIdx(rs
						.getInt("security_question_idx"));
				dbPlayer.setSecurityAnswerIdx(rs.getInt("security_answer_idx"));
				dbPlayer.setSecurityAnswer(rs.getString("security_answer"));
				dbPlayer.setRemark(rs.getString("remark"));
				dbPlayer.setAttribute(rs.getString("attribute"));

				return dbPlayer;
			} else {
				throw new PlayerAuthenticateException("该账号不存在！", null,
						PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND,
						"该账号不存在，请使用别的账号进行登录。");
			}
		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closeResultSet(rs);
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}

	}

	@Override
	public void updatePlayerAttributeById(long playerId, String attribute)
			throws PlayerAuthenticateException, KGameDBException {

		String updateSql = "update player set attribute=? where player_id = ?";

		Connection con = null;
		PreparedStatement ps = null;

		long[] restTime = initRestTime();

		con = platformDBConPool.getConnection();

		try {
			ps = con.prepareStatement(updateSql);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));
			ps.setString(1, attribute);
			ps.setLong(2, playerId);

			if (ps.executeUpdate() == 1) {
				return;
			} else {
				throw new PlayerAuthenticateException("修改属性的目标账号不存在！", null,
						PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND,
						"该账号不存在，请使用别的账号。");
			}
		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}
	}

	@Override
	public void updatePlayerAttributeByName(String playerName, String attribute)
			throws PlayerAuthenticateException, KGameDBException {
		String updateSql = "update player set attribute=? where player_name = ?";

		Connection con = null;
		PreparedStatement ps = null;

		long[] restTime = initRestTime();

		con = platformDBConPool.getConnection();

		try {
			ps = con.prepareStatement(updateSql);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));
			ps.setString(1, attribute);
			ps.setString(2, playerName);

			if (ps.executeUpdate() == 1) {
				return;
			} else {
				throw new PlayerAuthenticateException("修改属性的目标账号不存在！", null,
						PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND,
						"该账号不存在，请使用别的账号。");
			}
		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}

	}

	@Override
	public void updatePlayerRemarkById(long playerId, String remark)
			throws PlayerAuthenticateException, KGameDBException {
		String updateSql = "update player set remark=? where player_id = ?";

		Connection con = null;
		PreparedStatement ps = null;

		long[] restTime = initRestTime();

		con = platformDBConPool.getConnection();

		try {
			ps = con.prepareStatement(updateSql);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));
			ps.setString(1, remark);
			ps.setLong(2, playerId);

			if (ps.executeUpdate() == 1) {
				return;
			} else {
				throw new PlayerAuthenticateException("修改属性的目标账号不存在！", null,
						PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND,
						"该账号不存在，请使用别的账号。");
			}
		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}

	}

	@Override
	public void updatePlayerRemarkByName(String playerName, String remark)
			throws PlayerAuthenticateException, KGameDBException {
		String updateSql = "update player set remark=? where player_name = ?";

		Connection con = null;
		PreparedStatement ps = null;

		long[] restTime = initRestTime();

		con = platformDBConPool.getConnection();

		try {
			ps = con.prepareStatement(updateSql);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));
			ps.setString(1, remark);
			ps.setString(2, playerName);

			if (ps.executeUpdate() == 1) {
				return;
			} else {
				throw new PlayerAuthenticateException("修改属性的目标账号不存在！", null,
						PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND,
						"该账号不存在，请使用别的账号。");
			}
		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}

	}

	@Override
	public void addChargeReocrd(long player_id, long role_id, String role_name,
			int role_level, byte is_first_charge, float rmb, int charge_point,
			String card_num, String card_password, long charge_time,
			int charge_type, int promo_id, int parent_promo_id, int channel_id,
			String desc) {
		// DBChargeRecord record = new DBChargeRecord(player_id, role_id,
		// role_name, role_level, is_first_charge, rmb, charge_point,
		// card_num, card_password, charge_time, charge_type,
		// promo_id,parent_promo_id,
		// channel_id, KGame.getGSID(), desc);
		// syncTask.addChargeRecordQueue(record);

		String insertChargeRecordSql = "insert into charge_record(player_id,role_id,"
				+ "role_name,role_level,is_first_charge,rmb,charge_point,card_num,card_password"
				+ ",charge_time,charge_type,promo_id,parent_promo_id,channel_id,server_id,descr) values (?,?,?,?,?"
				+ ",?,?,?,?,?,?,?,?,?,?,?)";
		Connection con = null;
		PreparedStatement ps = null;

		try {
			con = platformDBConPool.getConnection();
			ps = con.prepareStatement(insertChargeRecordSql);

			try {
				ps.setLong(1, player_id);
				ps.setLong(2, role_id);
				ps.setString(3, role_name);
				ps.setInt(4, role_level);
				ps.setByte(5, is_first_charge);
				ps.setBigDecimal(6, new BigDecimal(Float.toString(rmb)));
				ps.setInt(7, charge_point);
				ps.setString(8, card_num);
				ps.setString(9, card_password);
				ps.setTimestamp(10, new Timestamp(charge_time));
				ps.setInt(11, charge_type);
				ps.setInt(12, promo_id);
				ps.setInt(13, parent_promo_id);
				ps.setInt(14, channel_id);
				ps.setInt(15, KGame.getGSID());
				ps.setString(16, (desc != null) ? desc : "");

				ps.execute();
			} catch (SQLException ex) {
				logger.error(
						"###ERROR###: PlatformFlowDataSyncTask插入充值流水发生异常，账号："
								+ "{},金额:{},卡号：{},密码：{},充值通道：{}", ex,
						player_id, rmb, card_num, card_password, channel_id);
			}

		} catch (SQLException ex) {
			logger.error("###ERROR###: PlatformFlowDataSyncTask插入充值流水发生异常！", ex);
		} finally {
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}
	}

	@Override
	public void addServerOnlineRecord(int area_id, int server_id,
			int online_count, long record_time, int connectCount) {
		String insertServerOnlineRecordSql = "insert into server_online_record(area_id,server_id,"
				+ "record_time,online_count,connect_count) values (?,?,?,?,?)";
		Connection con = null;
		PreparedStatement ps = null;

		try {

			con = platformDBConPool.getConnection();

			ps = platformDBConPool.writeStatement(con,
					insertServerOnlineRecordSql);

			ps.setInt(1, area_id);
			ps.setInt(2, server_id);
			ps.setTimestamp(3, new Timestamp(record_time));
			ps.setInt(4, online_count);
			ps.setInt(5, connectCount);
			ps.execute();

		} catch (SQLException ex) {
			logger.error(
					"###ERROR###: PlatformFlowDataSyncTask插入登录流水发生异常，大区ID："
							+ area_id + "，游戏区ID：" + server_id + "connectCount:"
							+ connectCount, ex);
		} finally {
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}

	}

	@Override
	public void addPresentPointRecord(long player_id, long role_id,
			String role_name, int present_point, int type, String desc,
			long present_time, int promo_id, int parent_promo_id) {
		DBPresentPointRecord record = new DBPresentPointRecord(player_id,
				role_id, role_name, present_point, type, desc, present_time,
				promo_id, parent_promo_id, KGame.getGSID());

		syncTask.addPresentPointRecordQueue(record);

	}

	private DBPlayer processPlayerLogin(String loginSql, String password,
			boolean isNeedCheckPwd) throws PlayerAuthenticateException,
			KGameDBException {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		long[] restTime = initRestTime();

		boolean verifyAccountResult = true;

		DBPlayer dbPlayer = null;

		try {
			con = platformDBConPool.getConnection();

			ps = con.prepareStatement(loginSql,
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));

			rs = platformDBConPool.executeQuery(ps);

			if (rs != null && rs.next()) {

				String dbPassword = rs.getString("password");

				if (!isNeedCheckPwd
						|| password.equals(dbPassword.toLowerCase())) {

					dbPlayer = new DBPlayer();

					dbPlayer.setPlayerId(rs.getLong("player_id"));
					dbPlayer.setPlayerName(rs.getString("player_name"));
					dbPlayer.setPromoMask(rs.getString("promo_mask"));
					dbPlayer.setPassword(dbPassword);
					dbPlayer.setType(rs.getInt("type"));
					dbPlayer.setCreateTimeMillis(rs
							.getTimestamp("register_time") != null ? rs
							.getTimestamp("register_time").getTime() : 0);
					dbPlayer.setLastestLoginTimeMillis(System
							.currentTimeMillis());
					dbPlayer.setLastestLogoutTimeMillis(rs
							.getTimestamp("lastest_logout_time") != null ? rs
							.getTimestamp("lastest_logout_time").getTime() : 0);
					dbPlayer.setMobileNum(rs.getString("mobile_num"));
					dbPlayer.setTotalLoginCount(rs.getLong("total_login_count") + 1);
					dbPlayer.setTotalOnlineTimeMillis(rs
							.getLong("total_online_time"));
					dbPlayer.setPromoId(rs.getInt("promo_id"));
					dbPlayer.setParentPromoId(rs.getInt("parent_promo_id"));
					dbPlayer.setSecurityQuestionIdx(rs
							.getInt("security_question_idx"));
					dbPlayer.setSecurityAnswerIdx(rs
							.getInt("security_answer_idx"));
					dbPlayer.setSecurityAnswer(rs.getString("security_answer"));
					dbPlayer.setRemark(rs.getString("remark"));
					dbPlayer.setAttribute(rs.getString("attribute"));

					rs.close();
					ps.close();

				} else {// 密码校验失败，忽略大小写
					verifyAccountResult = false;
					throw new PlayerAuthenticateException("该账号密码错误！", null,
							PlayerAuthenticateException.CAUSE_WRONG_PASSWORD,
							"验证密码错误，请输入正确的密码。");
				}

			} else {// 账号不存在
				verifyAccountResult = false;
				throw new PlayerAuthenticateException("该账号不存在！", null,
						PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND,
						"该账号不存在，请使用别的账号进行登录。");
			}

			if (verifyAccountResult && dbPlayer != null) {
				String updateSql = "update player set lastest_login_time = ? , "
						+ "total_login_count = total_login_count + 1 where player_id = "
						+ dbPlayer.getPlayerId();

				getRestTimeout(restTime);

				ps = con.prepareStatement(updateSql);
				platformDBConPool.setQueryTimeout(ps,
						getStatmentTimeout(restTime));
				ps.setTimestamp(1,
						new Timestamp(dbPlayer.getLastestLoginTimeMillis()));
				ps.execute();

				if (!dbPlayer.getPlayerName().equals("gm10086")) {
					LoginRecod record = new LoginRecod();
					record.playerId = (dbPlayer.getPlayerId());
					record.loginTime = (dbPlayer.getLastestLoginTimeMillis());
					record.promo_id = (dbPlayer.getPromoId());
					record.parent_promo_id = (dbPlayer.getParentPromoId());
					record.serverId = KGame.getGSID();

					loginRecordMap.put(record.playerId, record);
					syncTask.addLoginRecordQueue(record);
				}

				return dbPlayer;
			} else {
				throw new PlayerAuthenticateException("该账号不存在或密码错误！", null,
						PlayerAuthenticateException.CAUSE_WRONG_PASSWORD,
						"登录失败，该账号不存在或密码错误。");
			}

		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closeResultSet(rs);
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);
		}
	}

	private void checkAppliedUserName(String appliedUserName, Connection con,
			long[] restTime) throws InvalidUserInfoException, KGameDBException {
		String sql = "select count(*) from player where player_name = ?";

		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(sql.toString(),
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));
			ps.setString(1, appliedUserName.toLowerCase());

			rs = platformDBConPool.executeQuery(ps);
			if (rs != null) {
				rs.next();
				if (rs.getInt(1) != 0) {
					throw new InvalidUserInfoException("指定的帐号名:"
							+ appliedUserName + "已存在", null,
							InvalidUserInfoException.PLAYER_NAME_INVALID,
							"尝试注册账号但指定的帐号名:" + appliedUserName
									+ "已存在,请重新输入帐号名!");
				}
			}

		} catch (SQLException ex) {
			throw new KGameDBException("数据库发生未知异常", ex,
					KGameDBException.CAUSE_DB_SHUTDOWN_WITH_UNKNOW_REASON,
					"注册账号失败，数据库发生未知异常");
		} finally {
			platformDBConPool.closeResultSet(rs);
			platformDBConPool.closePreparedStatement(ps);
		}
	}

	private long checkAppliedPromoMask(String promoMask, int promoId,
			Connection con, long[] restTime) throws InvalidUserInfoException,
			KGameDBException {
		String sql = "select * from player where promo_mask = ? and parent_promo_id =?";

		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(sql.toString(),
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));
			ps.setString(1, promoMask.toLowerCase());
			ps.setInt(2, promoId);

			rs = platformDBConPool.executeQuery(ps);
			if (rs != null) {
				rs.next();
				// if (rs.getInt(1) != 0) {
				// throw new InvalidUserInfoException("指定的帐号名:"
				// + appliedUserName + "已存在", null,
				// InvalidUserInfoException.PLAYER_NAME_INVALID,
				// "尝试注册账号但指定的帐号名:" + appliedUserName
				// + "已存在,请重新输入帐号名!");
				return rs.getLong("player_id");
				// }
			}

			return -1;

		} catch (SQLException ex) {
			throw new KGameDBException("数据库发生未知异常", ex,
					KGameDBException.CAUSE_DB_SHUTDOWN_WITH_UNKNOW_REASON,
					"注册账号失败，数据库发生未知异常");
		} finally {
			platformDBConPool.closeResultSet(rs);
			platformDBConPool.closePreparedStatement(ps);
		}
	}

	private void checkPlayerName(String playerName)
			throws InvalidUserInfoException {
		if (playerName == null) {
			throw new InvalidUserInfoException("用户名长度不符合要求（4 - 50 个字符）", null,
					InvalidUserInfoException.PLAYER_NAME_INVALID_LENGTH,
					"输入的用户名不能为空。");
		}
		if (playerName.length() < _MIN_LENGTH_OF_PLAYER_NAME) {
			throw new InvalidUserInfoException("用户名长度不符合要求（4 - 50 个字符）", null,
					InvalidUserInfoException.PLAYER_NAME_INVALID_LENGTH,
					"输入的用户名过短。");
		}
		if (playerName.length() > _MAX_LENGTH_OF_PLAYER_NAME) {
			throw new InvalidUserInfoException("用户名长度不符合要求（4 - 50 个字符）", null,
					InvalidUserInfoException.PLAYER_NAME_INVALID_LENGTH,
					"输入的用户名过长。");
		}

		for (int i = 0; i < playerName.length(); i++) {
			char c = playerName.charAt(i);
			// if(c<48 || (c>57&&c<65)||(c>90&&c<97)||c>122){
			/*if (c < 48 || (c > 57 && c < 65) || (c > 90 && c < 95) || (c == 96)
					|| c > 122) {*/
			if(_ALLOW_CHARS.contains(c)) {
				continue;
			}
			if (c < 48 || (c > 57 && c < 65) || (c > 90 && c < 95) || (c == 96) || c > 122) {
				// 2015-01-09 增加允许64这个ASCII的字符通过（“@”有些渠道是用邮箱注册的）
				throw new InvalidUserInfoException(
						"用户名使用的字符非法，用户名只能使用英文字符加数字字符的组合。", null,
						InvalidUserInfoException.PLAYER_NAME_INVALID,
						"输入的用户名字符非法，用户名只能使用英文字符加数字字符的组合");
			}
		}

	}

	private void checkPassword(String password) throws InvalidUserInfoException {
		if (password == null) {
			throw new InvalidUserInfoException("密码长度不符合要求（6 - 18 个字符）", null,
					InvalidUserInfoException.PLAYER_PASSWORD_INVALID_LENGTH,
					"输入的密码不能为空。");
		}
		if (password.length() < _MIN_LENGTH_OF_PASSWORD) {
			throw new InvalidUserInfoException("密码长度不符合要求（6 - 18 个字符）", null,
					InvalidUserInfoException.PLAYER_PASSWORD_INVALID_LENGTH,
					"输入的密码过短。");
		}
		if (password.length() > _MAX_LENGTH_OF_PASSWORD) {
			throw new InvalidUserInfoException("密码长度不符合要求（6 - 18 个字符）", null,
					InvalidUserInfoException.PLAYER_PASSWORD_INVALID_LENGTH,
					"输入的密码过长。");
		}

		for (int i = 0; i < password.length(); i++) {
			char c = password.charAt(i);
			if (c < 48 || (c > 57 && c < 65) || (c > 90 && c < 95) || (c == 96)
					|| c > 122) {
				throw new InvalidUserInfoException(
						"用户名使用的字符非法，用户名只能使用英文字符加数字字符的组合。",
						null,
						InvalidUserInfoException.PLAYER_PASSWORD_INVALID_LENGTH,
						"输入的密码字符非法，密码只能使用英文字符、数字字符、下划线的组合");
			}
		}
	}

	/**
	 * 登录流水数据类
	 * 
	 * @author Administrator
	 * 
	 */
	private class LoginRecod {
		public long id;
		public long playerId;
		public long loginTime;
		public long logoutTime;
		public long onlineTime;
		public int serverId;
		public int promo_id;
		public int parent_promo_id;

	}

	// public static class ServerOnlineRecord {
	// public int area_id;
	// public int server_id;
	// public int online_count;
	// public long record_time;
	// public String desc;
	//
	// public ServerOnlineRecord(int area_id, int server_id, int online_count,
	// long record_time, String desc) {
	// super();
	// this.area_id = area_id;
	// this.server_id = server_id;
	// this.online_count = online_count;
	// this.record_time = record_time;
	// this.desc = desc;
	// }
	// }

	public static class PlatformFlowDataSyncTask implements KGameTimerTask {

		public static volatile boolean isPrepareShutdown = false;

		private final int delayTime = 60;

		private DefineDataSourceManagerIF platformDBConPool;

		private ConcurrentLinkedQueue<LoginRecod> loginRecordSqlQueue = new ConcurrentLinkedQueue<LoginRecod>();

		private ConcurrentLinkedQueue<LoginRecod> logoutRecordSqlQueue = new ConcurrentLinkedQueue<LoginRecod>();

		private ConcurrentLinkedQueue<DBChargeRecord> chargeRecordSqlQueue = new ConcurrentLinkedQueue<DBChargeRecord>();

		private ConcurrentLinkedQueue<DBPresentPointRecord> presentPointRecordSqlQueue = new ConcurrentLinkedQueue<DBPresentPointRecord>();

		public PlatformFlowDataSyncTask(
				DefineDataSourceManagerIF platformDBConPool) {
			super();
			this.platformDBConPool = platformDBConPool;
		}

		public void addLoginRecordQueue(LoginRecod record) {
			loginRecordSqlQueue.add(record);
		}

		public void addLogoutRecordQueue(LoginRecod record) {
			logoutRecordSqlQueue.add(record);
		}

		public void addChargeRecordQueue(DBChargeRecord record) {
			chargeRecordSqlQueue.add(record);
		}

		public void addPresentPointRecordQueue(DBPresentPointRecord record) {
			presentPointRecordSqlQueue.add(record);
		}

		@Override
		public String getName() {

			return PlatformFlowDataSyncTask.class.getName();
		}

		@Override
		public int[] onTimeSignal(KGameTimeSignal timeSignal)
				throws KGameServerException {

			long sysTime = System.currentTimeMillis();

			int loginCount = syscLoginRecord();

			int logoutCount = syscLogoutRecord();

			int chargeCount = syscChargeRecord();

			int presentCount = syscPresentPointRecord();

			try {
				if (isPrepareShutdown) {
					if (loginRecordMap.size() > 0) {
						long defaultOnlineTime = 30 * 60 * 1000;
						for (LoginRecod record : loginRecordMap.values()) {
							if (record.logoutTime == 0) {
								record.logoutTime = record.loginTime
										+ defaultOnlineTime;
								record.onlineTime = defaultOnlineTime;
								logoutRecordSqlQueue.add(record);
							}
						}

						logoutCount += syscLogoutRecord();
					}
				}
			} catch (Exception e) {
				logger.error(
						"###ERROR###: PlatformFlowDataSyncTask插入流水发生异常!!!", e);
			}

			sysTime = System.currentTimeMillis() - sysTime;

			int[] result = new int[] { loginCount, logoutCount, chargeCount,
					presentCount };

			return result;
		}

		@Override
		public void done(KGameTimeSignal timeSignal) {
			// long sysTime = 0;
			// try {
			// sysTime = (Long) timeSignal.get();
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// } catch (ExecutionException e) {
			// e.printStackTrace();
			// }
			//
			// long currentDelayTime = (delayTime * 1000 - sysTime > 0 ?
			// (delayTime * 1000 - sysTime)
			// / 1000 + (sysTime % (delayTime * 1000) > 0 ? 1 : 0)
			// : 0);
		}

		@Override
		public void rejected(RejectedExecutionException e) {
			// TODO Auto-generated method stub

		}

		private int syscLoginRecord() {

			try {
				int loginRecordSize = loginRecordSqlQueue.size();
				int sysCount = 0;
				if (loginRecordSize > 0) {
					String insertLoginRecordSql = "insert into login_record(player_id,login_time,"
							+ "server_id,promo_id,parent_promo_id) values (?,?,?,?,?)";
					Connection con = null;
					PreparedStatement ps = null;
					ResultSet rs = null;

					try {
						con = platformDBConPool.getConnection();

						if (con != null) {

							ps = con.prepareStatement(insertLoginRecordSql,
									Statement.RETURN_GENERATED_KEYS);

							for (int i = 0; i < loginRecordSize; i++) {
								LoginRecod record = loginRecordSqlQueue.poll();
								try {
									ps.setLong(1, record.playerId);
									ps.setTimestamp(2, new Timestamp(
											record.loginTime));
									ps.setInt(3, record.serverId);
									ps.setInt(4, record.promo_id);
									ps.setInt(5, record.parent_promo_id);

									if (platformDBConPool.executeUpdate(ps) == 1) {
										rs = ps.getGeneratedKeys();
										rs.next();
										long id = rs.getLong(1);
										if (loginRecordMap
												.containsKey(record.playerId)) {
											record.id = id;
										}
										sysCount++;
									}

									rs.close();
									ps.clearParameters();
								} catch (SQLException ex) {
									logger.error(
											"###ERROR###: PlatformFlowDataSyncTask插入登录流水发生异常，账号："
													+ record.playerId, ex);
								}
							}
						}

					} catch (SQLException ex) {
						logger.error(
								"###ERROR###: PlatformFlowDataSyncTask插入登录流水发生异常！",
								ex);
					} catch (Exception e) {
						logger.error(
								"###ERROR###: PlatformFlowDataSyncTask插入登录流水发生异常！",
								e);
					} finally {
						platformDBConPool.closePreparedStatement(ps);
						platformDBConPool.closeConnection(con);
					}
				}
				return sysCount;
			} catch (Exception e) {
				logger.error(
						"###ERROR###: PlatformFlowDataSyncTask插入登录流水发生异常！", e);
			}
			return 0;
		}

		private int syscLogoutRecord() {
			try {
				int logoutRecordSize = logoutRecordSqlQueue.size();
				int sysCount = 0;
				if (logoutRecordSize > 0) {
					String updateLoginRecordSql = "update login_record set logout_time=?,online_time=? "
							+ "where id = ?";

					Connection con = null;
					PreparedStatement ps = null;

					try {
						con = platformDBConPool.getConnection();

						if (con != null) {

							ps = con.prepareStatement(updateLoginRecordSql);

							for (int i = 0; i < logoutRecordSize; i++) {
								LoginRecod record = logoutRecordSqlQueue.poll();
								try {
									ps.setTimestamp(1, new Timestamp(
											record.logoutTime));
									ps.setLong(2, record.onlineTime);
									ps.setLong(3, record.id);

									if (ps.executeUpdate() == 1) {
										sysCount++;
									}

									ps.clearParameters();
								} catch (SQLException ex) {
									logger.error(
											"###ERROR###: PlatformFlowDataSyncTask更新登出流水发生异常，账号："
													+ record.playerId, ex);
								}

								if (loginRecordMap.containsKey(record.playerId)) {
									loginRecordMap.remove(record.playerId);
								}
							}
						}

					} catch (SQLException ex) {
						logger.error(
								"###ERROR###: PlatformFlowDataSyncTask更新登出流水发生异常！",
								ex);
					} catch (Exception e) {
						logger.error(
								"###ERROR###: PlatformFlowDataSyncTask插入登出流水发生异常！",
								e);
					} finally {
						platformDBConPool.closePreparedStatement(ps);
						platformDBConPool.closeConnection(con);
					}

				}
				return sysCount;
			} catch (Exception e) {
				logger.error(
						"###ERROR###: PlatformFlowDataSyncTask插入登出流水发生异常！", e);
			}
			return 0;
		}

		private int syscChargeRecord() {
			try {
				int chargeRecordSize = chargeRecordSqlQueue.size();
				int sysCount = 0;
				if (chargeRecordSize > 0) {

					String insertChargeRecordSql = "insert into charge_record(player_id,role_id,"
							+ "role_name,role_level,is_first_charge,rmb,charge_point,card_num,card_password"
							+ ",charge_time,charge_type,promo_id,parent_promo_id,channel_id,server_id,descr) values (?,?,?,?,?"
							+ ",?,?,?,?,?,?,?,?,?,?,?)";
					Connection con = null;
					PreparedStatement ps = null;

					try {
						con = platformDBConPool.getConnection();
						if (con != null) {
							ps = con.prepareStatement(insertChargeRecordSql);

							for (int i = 0; i < chargeRecordSize; i++) {
								DBChargeRecord record = chargeRecordSqlQueue
										.poll();
								try {
									ps.setLong(1, record.player_id);
									ps.setLong(2, record.role_id);
									ps.setString(3, record.role_name);
									ps.setInt(4, record.role_level);
									ps.setByte(5, record.is_first_charge);
									ps.setInt(6, record.rmb);
									ps.setInt(7, record.charge_point);
									ps.setString(8, record.card_num);
									ps.setString(9, record.card_password);
									ps.setTimestamp(10, new Timestamp(
											record.charge_time));
									ps.setInt(11, record.charge_type);
									ps.setInt(12, record.promo_id);
									ps.setInt(13, record.parent_promo_id);
									ps.setInt(14, record.channel_id);
									ps.setInt(15, record.server_id);
									ps.setString(16,
											(record.desc != null) ? record.desc
													: "");

									if (ps.execute()) {
										sysCount++;
									}

									ps.clearParameters();
								} catch (SQLException ex) {
									logger.error(
											"###ERROR###: PlatformFlowDataSyncTask插入充值流水发生异常，账号："
													+ "{},金额:{},卡号：{},密码：{},充值通道：{}",
											ex, record.player_id, record.rmb,
											record.card_num,
											record.card_password,
											record.channel_id);
								}
							}
						}

					} catch (SQLException ex) {
						logger.error(
								"###ERROR###: PlatformFlowDataSyncTask插入充值流水发生异常！",
								ex);
					} catch (Exception e) {
						logger.error(
								"###ERROR###: PlatformFlowDataSyncTask插入充值流水发生异常！",
								e);
					} finally {
						platformDBConPool.closePreparedStatement(ps);
						platformDBConPool.closeConnection(con);
					}
				}
				return sysCount;
			} catch (Exception e) {
				logger.error(
						"###ERROR###: PlatformFlowDataSyncTask插入登录流水发生异常！", e);
			}
			return 0;
		}

		private int syscPresentPointRecord() {
			try {
				int presentPointRecordSize = presentPointRecordSqlQueue.size();
				int sysCount = 0;
				if (presentPointRecordSize > 0) {
					String insertPresentPointRecordSql = "insert into present_game_point_record(player_id,role_id,"
							+ "role_name,present_point,type,present_time,server_id,promo_id,parent_promo_id,descr) "
							+ "values (?,?,?,?,?,?,?,?,?,?)";
					Connection con = null;
					PreparedStatement ps = null;

					try {
						con = platformDBConPool.getConnection();
						if (con != null) {
							ps = con.prepareStatement(insertPresentPointRecordSql);

							for (int i = 0; i < presentPointRecordSize; i++) {
								DBPresentPointRecord record = presentPointRecordSqlQueue
										.poll();
								try {
									ps.setLong(1, record.player_id);
									ps.setLong(2, record.role_id);
									ps.setString(3, record.role_name);
									ps.setInt(4, record.present_point);
									ps.setInt(5, record.type);
									ps.setTimestamp(6, new Timestamp(
											record.present_time));
									ps.setInt(7, record.server_id);
									ps.setInt(8, record.promo_id);
									ps.setInt(9, record.parent_promo_id);
									ps.setString(10,
											(record.desc != null) ? record.desc
													: "");
									if (ps.execute()) {
										sysCount++;
									}

									ps.clearParameters();
								} catch (SQLException ex) {
									logger.error(
											"###ERROR###: PlatformFlowDataSyncTask插入赠送点数流水发生异常，账号："
													+ "{},角色名：{},赠送点数：{},类型：{}",
											ex, record.player_id,
											record.role_name,
											record.present_point, record.type);
								}
							}
						}

					} catch (SQLException ex) {
						logger.error(
								"###ERROR###: PlatformFlowDataSyncTask插入赠送点数流水发生异常！",
								ex);
					} catch (Exception e) {
						logger.error(
								"###ERROR###: PlatformFlowDataSyncTask插入赠送点数流水发生异常！",
								e);
					} finally {
						platformDBConPool.closePreparedStatement(ps);
						platformDBConPool.closeConnection(con);
					}

				}
				return sysCount;
			} catch (Exception e) {
				logger.error(
						"###ERROR###: PlatformFlowDataSyncTask插入赠送点数流水发生异常！", e);
			}
			return 0;
		}
	}

	@Override
	public DBPlayer loadDBPlayer(long playerId) throws KGameDBException {
		String sql = "select * from player where player_id = " + playerId;

		DBPlayer dbPlayer = null;

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		long[] restTime = initRestTime();

		try {
			con = platformDBConPool.getConnection();
			ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			platformDBConPool.setQueryTimeout(ps, getStatmentTimeout(restTime));
			rs = platformDBConPool.executeQuery(ps);
			if (rs != null) {
				rs.next();

				dbPlayer = new DBPlayer();

				dbPlayer.setPlayerId(rs.getLong("player_id"));
				dbPlayer.setPlayerName(rs.getString("player_name"));
				dbPlayer.setPassword(rs.getString("password"));
				dbPlayer.setType(rs.getInt("type"));
				dbPlayer.setCreateTimeMillis(rs.getTimestamp("register_time") != null ? rs
						.getTimestamp("register_time").getTime() : 0);
				dbPlayer.setLastestLoginTimeMillis(System.currentTimeMillis());
				dbPlayer.setLastestLogoutTimeMillis(rs
						.getTimestamp("lastest_logout_time") != null ? rs
						.getTimestamp("lastest_logout_time").getTime() : 0);
				dbPlayer.setMobileNum(rs.getString("mobile_num"));
				dbPlayer.setTotalLoginCount(rs.getLong("total_login_count") + 1);
				dbPlayer.setTotalOnlineTimeMillis(rs
						.getLong("total_online_time"));
				dbPlayer.setPromoId(rs.getInt("promo_id"));
				dbPlayer.setSecurityQuestionIdx(rs
						.getInt("security_question_idx"));
				dbPlayer.setSecurityAnswerIdx(rs.getInt("security_answer_idx"));
				dbPlayer.setSecurityAnswer(rs.getString("security_answer"));
				dbPlayer.setRemark(rs.getString("remark"));
				dbPlayer.setAttribute(rs.getString("attribute"));

				return dbPlayer;

			} else {
				throw new KGameDBException("该账号不存在！", null,
						KGameDBException.CAUSE_RECORD_NOT_EXIST, "需要加载的账号不存在。");
			}
		} catch (SQLException ex) {
			throw new KGameDBException(ex.getMessage(), ex,
					KGameDBException.CAUSE_UNKNOWN_ERROR, "数据访问发生异常。");
		} finally {
			platformDBConPool.closeResultSet(rs);
			platformDBConPool.closePreparedStatement(ps);
			platformDBConPool.closeConnection(con);

		}
	}

}
