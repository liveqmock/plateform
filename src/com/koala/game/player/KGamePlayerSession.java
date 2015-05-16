package com.koala.game.player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.slf4j.Logger;

import com.koala.game.KGameMessage;
import com.koala.game.KGameMsgCompressConfig;
import com.koala.game.KGameProtocol;
import com.koala.game.communication.KGameChannelAttachment;
import com.koala.game.dataaccess.KGameDBException;
import com.koala.game.dataaccess.KGameDataAccessFactory;
import com.koala.game.dataaccess.dbobj.DBPlayer;
import com.koala.game.frontend.FEStatusMonitor;
import com.koala.game.frontend.KGameFrontend;
import com.koala.game.gameserver.GSStatusMonitor;
import com.koala.game.gameserver.KGameServer;
import com.koala.game.logging.KGameLogger;

/**
 * 代表一个‘玩家会话’，包含有‘玩家{@link KGamePlayer}’数据和‘通道’数据.<br>
 * (1)通过{@link #send(KGameMessage)}方法可以向对应的客户端发送消息<br>
 * (2)通过{@link #getAttachment()}和 {@link #setAttachment(Object)}可以往
 * {@link KGamePlayerSession}绑上一个附件 <br>
 * <b> ！！！注意：本对象代表一个玩家的会话，有可能存在会话建立了但尚未加载到/绑定到‘玩家{@link KGamePlayer}
 * ’对象的情况，但此时发消息等都是允许的。 </b> <br>
 * 
 * @author AHONG
 * 
 */
public final class KGamePlayerSession {

	private static final Logger logger = KGameLogger.getLogger(KGamePlayerSession.class);
	private static final Logger _pingLogger = KGameLogger.getLogger("pingLogger");
	
	private Channel channel;
	private KGamePlayer player;
	/** 关键开关：是否已经通过账号验证，ture表示已经成功登陆 */
	private final AtomicBoolean authenticate = new AtomicBoolean(false);
	private Object attachment;
	private byte clientType;
	private String curLoginDeviceModel;
	private int lastSelectGsId=-1;
	private long disconnectTime; // 2014-08-15 记录断连时间，为延迟移除和断线重连服务
	
	// 加速器检测的相关变量 BEGIN
//	private long _totalIntervalMillisOfPingByClient; // ping总共间隔时间（毫秒）
////	private boolean _firstRecord; // 是否本次会话期间的第一记录
//	private long _clientLastPingMillis; // 客户端上次ping的时间（毫秒）
	private long _serverMillisOfLastPing; // 客户端上次ping的时候，服务器的时间（毫秒）
	private long _totalIntervalMillisOfPingInServer; // 服务器记录的，客户端总共ping的时间间隔总和
	private long _millisWhenSessionCreated; // playerSession创建的时间 @2014-06-19 12:28
	private int totalPingCount; // 本次会话期间一共ping的次数
	private List<Long> _clientLastPingRecord = new ArrayList<Long>(_checkInterval);
	private List<Long> _serverLastPingRecord = new ArrayList<Long>(_checkInterval);
	
	private static int _checkInterval = 5;
	private static float _allowMistake = 1.1f;
	private static boolean _openBan = true;
	// 加速器检测的相关变量 END

	public KGamePlayerSession(Channel channel) {
		this.channel = channel;
		this._millisWhenSessionCreated = System.currentTimeMillis();
//		logger.debug("serverTime={}", _serverMillisOfLastPing);
	}

	public KGamePlayerSession(KGamePlayer player, Channel channel) {
		this(channel);
		this.player = player;
//		this.channel = channel;
	}

//	@Override
//	public String toString() {
//		return String
//				.format("[KGamePlayerSession: id=%s,name=%s,bindplayer?%b,channel?%b,authenticate?%b,attachment?%b ]",
//						player == null ? -1 : player.getID(),
//						player == null ? "" : player.getPlayerName(), player,
//						channel, authenticate.get(), attachment);
//	}

	@Override
	public String toString() {
		return "KGamePlayerSession [channel=" + channel + ", player=" + player
				+ ", authenticate=" + authenticate + ", attachment="
				+ attachment + ", clientType=" + clientType
				+ ", curLoginDeviceModel=" + curLoginDeviceModel
				+ ", lastSelectGsId=" + lastSelectGsId + "]";
	}

	public void setAuthenticationPassed(boolean pass) {
		authenticate.set(pass);
	}

	public void setAttachment(Object obj) {
		this.attachment = obj;
	}

	public Object getAttachment() {
		return attachment;
	}

	/**
	 * 是否已经通过身份验证
	 * 
	 * @return
	 */
	public boolean isAuthenticationPassed() {
		return authenticate.get();
	}

	public Channel getChannel() {
		return channel;
	}

	public void bindChannel(Channel channel) { // 2014-08-14 改为public
		// 如果本身channel不为null，不能简单地重新赋值
		this.channel = channel;
	}

	public boolean loadAndBindPlayer(String playerName) {
		DBPlayer dbPlayer = null;
		try {
			dbPlayer = KGameDataAccessFactory.getInstance()
					.getPlayerManagerDataAccess().loadDBPlayer(playerName);
		} catch (KGameDBException e) {
			e.printStackTrace();
		}
		if (dbPlayer != null) {
			return bindPlayer(new KGamePlayer(dbPlayer));
		}
		return false;
	}

	public boolean decodeAndBindPlayer(DBPlayer dbplayer) {
		return bindPlayer(new KGamePlayer(dbplayer));
	}

	protected boolean bindPlayer(KGamePlayer player) {
		if (player != null) {
			this.player = player;
			if (channel != null) {
				channel.setAttachment(new KGameChannelAttachment(player.getID()));
			}
			return true;
		}
		return false;
	}

	public ChannelFuture close() {
		// 保存Player数据

		// 关闭channel
		if (channel != null) {
			return channel.close();
		}
		return null;
	}

	public String getIPAddress() {
		if (channel != null) {
			InetSocketAddress sa = (InetSocketAddress) channel
					.getRemoteAddress();
			if (sa != null) {
				InetAddress ia = sa.getAddress();
				if(ia != null){
					return ia.getHostAddress();
				}
			}
		}
		return "127.0.0.1";
	}
	
	public KGamePlayer getBoundPlayer() {
		return player;
	}

	public byte getClientType() {
		return clientType;
	}

	public void setClientType(byte clientType) {
		this.clientType = clientType;
	}
	
	/**
	 * 获取断连时间，当断连原因是以下情况：
	 * {@link KGameProtocol#CAUSE_PLAYEROUT_IDLE}
	 * {@link KGameProtocol#CAUSE_PLAYEROUT_UNKNOWN}
	 * {@link KGameProtocol#CAUSE_PLAYEROUT_DISCONNECT}
	 * {@link KGameProtocol#CAUSE_PLAYEROUT_EXCEPTION}
	 * 此时间不会为0。
	 * 当session是新连接上（登录或者重连），这个时间会是0
	 * 其他情况，这个时间会是0
	 * @return
	 */
	public long getDisconnectTime() {
		return this.disconnectTime;
	}
	
	/**
	 * 设置断连时间，当断连原因是：
	 * {@link KGameProtocol#CAUSE_PLAYEROUT_IDLE}
	 * {@link KGameProtocol#CAUSE_PLAYEROUT_UNKNOWN}
	 * {@link KGameProtocol#CAUSE_PLAYEROUT_DISCONNECT}
	 * {@link KGameProtocol#CAUSE_PLAYEROUT_EXCEPTION}
	 * 此时间会被设为断连那一刻的时间
	 * 当断线重连，会被设为0
	 * @param time
	 */
	public void setDisconnectTime(long time) {
		this.disconnectTime = time;
	}

	public boolean send(KGameMessage msg) {
		if(disconnectTime > 0) {
			// 2014-08-14 添加 如果断连时间大于0，则不发送消息，因为此时，channel已经断开了
			return false;
		}
		// 2014-06-09 针对游戏逻辑增加，当消息的长度超过一定长度的时候，自动压缩消息体的内容
		if (msg != null && KGameMsgCompressConfig.getMsgLengthForAutoCompress() < msg.getPayloadLength()) {
			msg.setEncryption(KGameMsgCompressConfig.getAutoCompressType());
		}
		// 2014-06-09 END
		return write(msg);
	}

	public boolean write(KGameMessage msg) {
		// TODO KGamePlayerSession的消息发送处理；是否应该放到发送队列再由专门的线程池执行？
		if (msg != null) {
			msg.setByte(KGameMessage.INDEX_CLIENTTYPE, getClientType());

			channel.write(msg);

			if (KGameServer.getInstance() != null) {
				GSStatusMonitor.commcounter.writtenAmountPerMID(msg.getMsgID(),
						msg.getPayloadLength() + KGameMessage.LENGTH_OF_HEADER);
			}
			if (KGameFrontend.getInstance() != null) {
				FEStatusMonitor.commcounter.writtenAmountPerMID(msg.getMsgID(),
						msg.getPayloadLength() + KGameMessage.LENGTH_OF_HEADER);
			}
			
			return true;
		}
		return false;
	}

	public String getCurLoginDeviceModel() {
		return curLoginDeviceModel;
	}

	public void setCurLoginDeviceModel(String curLoginDeviceModel) {
		this.curLoginDeviceModel = curLoginDeviceModel;
	}

	public int getLastSelectGsId() {
		return lastSelectGsId;
	}

	public void setLastSelectGsId(int lastSelectGsId) {
		this.lastSelectGsId = lastSelectGsId;
	}

	/** 返回false表示作弊，可断开客户端 */
	public boolean checkPingRate(int pingJudgeMin, int allowPingPerMin, long pingclienttime) {
		/*
		 * 2014-06-17 更新 by perry
		 * 防作弊思路：
		 * 1、客户端是5秒进行一次ping
		 * 2、客户端每次ping的时候，会将客户端的当前时间（一个相对于0的毫秒数）带在ping消息发送到服务器，
		 *    例如，5000,10000,15000...一直顺延下去
		 * 3、第一次ping的时候，记录服务器收到ping的当前时间（_serverMillisOfLastPing）、客户端ping的时间（_clientLastPingMillis） ，
		 *    以此作为基准，初始化两个计时器（_totalIntervalMillisOfPingInServer和_totalIntervalMillisOfPingByClient）
		 * 4、从第二次ping开始，用客户端时间计算_totalIntervalMillisOfPingByClient这个计时器，用服务器时间
		 * 	  计算_totalIntervalMillisOfPingInServer
		 * 5、当ping的次数达到规定的次数（这里是5的倍数），进行比较，如果服务器的计时器，此时比客户端的计时器还要小，可以判断作弊。
		 *    这是因为，本来服务器收到ping的时间，由于网络延迟的因素，前后两个ping的间隔时间，应该比客户端要大。例如客户端两条
		 *    ping消息之间的消息是5000和10000，服务器收到的时候，可能是5500和11000，客户端的时间间隔5秒，到服务器这里，可能已经间了
		 *    6秒，所以不存在客户端间隔5秒的时候，服务器才间隔了4秒，这明显是有问题。因为加速器的原理是将时钟频率加快，所以ping的频率也
		 *    加快，即客户端显示过了5秒，实际上没有过。
		 * 6、由于两个间隔的时间是一直不断累加的，所以可以较好地控制网络拥堵时，一下子收到n条消息的影响。
		 */
		totalPingCount++;
//		if (_clientLastPingMillis == 0) {
////			_firstRecord = true;
//			_clientLastPingMillis = /*ct*/pingclienttime;
//			// 2014-06-19 添加了_serverMillisOfLastPing取值的选取判断，特定情况下，_serverMillisOfLastPing要选取session的创建时间作为基准
////			long currentTimeMillis = System.currentTimeMillis();
////			long difference = currentTimeMillis - _millisWhenSessionCreated;
////			if (difference > pingclienttime * _checkInterval) {
////				/* 如果当前时间与session创建时间之间的差值大于_checkInterval次客户端ping的时间总和，证明这里存在严重延迟（有可能是debug）
////				 * 为了保证不会误封，这里将第一次ping的服务器时间记录为session的创建时间
////				 */
////				_serverMillisOfLastPing = _millisWhenSessionCreated;
////			} else {
////				_serverMillisOfLastPing = currentTimeMillis;
////			}
//			_serverMillisOfLastPing = _millisWhenSessionCreated; // 2014-07-21 把第一次ping的时间都设置为session创建的时间，以防出现严重网络延迟，前面的ping消息一起到
//			_totalIntervalMillisOfPingInServer = 0;
//			_totalIntervalMillisOfPingByClient = 0;
////			logger.info("服务器lastPingTimeMillis={}", _serverMillisOfLastPing);
//		} 
		if(totalPingCount == 1) {
			_serverMillisOfLastPing = _millisWhenSessionCreated; // 2014-07-21 把第一次ping的时间都设置为session创建的时间，以防出现严重网络延迟，前面的ping消息一起到
			_totalIntervalMillisOfPingInServer = pingclienttime;
			_clientLastPingRecord.add(pingclienttime);
			_serverLastPingRecord.add(_totalIntervalMillisOfPingInServer);
		} else {
//			long current = System.currentTimeMillis();
//			_totalIntervalMillisOfPingByClient += (/*ct*/pingclienttime - _clientLastPingMillis);  // 计算收到ping的时候，客户端的ping间隔
//			_totalIntervalMillisOfPingInServer += current - _serverMillisOfLastPing; // 计算收到ping的时候，服务器的ping间隔
//			_clientLastPingRecord.add(_totalIntervalMillisOfPingByClient);
//			_serverLastPingRecord.add(_totalIntervalMillisOfPingInServer);
//			_serverMillisOfLastPing = current; // 设置当前时间，作为下次的基准
//			_clientLastPingMillis = /* ct */pingclienttime; // 设置当前ping时间，作为下次的基准
////			if(_firstRecord) {
////				_firstRecord = false;
////				// 由于网络延迟，可能会出现，第一条ping消息和第二条ping消息同时到达的情况，或者两者之间相距很短的情况
////				// 这里就会导致_totalIntervalMillisOfPingInServer += current - _serverMillisOfLastPing; 可能会得出0的情况
////				// 由于第一次ping和第二次ping的间隔很重要，会作为基准，所以，这里先假定，第一二次ping之间，客户端不会作弊
////				// 所以在出现头两次ping之间，服务器的间隔比客户端的间隔要短的时候，这里选择相信客户端
////				// 判断是否头两次ping之间
////				if(_serverLastPingRecord.get(0) < _totalIntervalMillisOfPingByClient) {
////					// 这里暂时确保把服务器与客户端之间的头两次ping的间隔设为一致
////					_serverMillisOfLastPing -= _totalIntervalMillisOfPingByClient;
////					_totalIntervalMillisOfPingInServer = _totalIntervalMillisOfPingByClient;
////					_serverLastPingRecord.set(0, _totalIntervalMillisOfPingByClient);
////				}
////			}
////			logger.info("服务器lastPingTimeMillis={}", _serverMillisOfLastPing);
			long current = System.currentTimeMillis();
			_totalIntervalMillisOfPingInServer += current - _serverMillisOfLastPing; // 计算收到ping的时候，服务器的ping间隔
			_clientLastPingRecord.add(pingclienttime);
			_serverLastPingRecord.add(_totalIntervalMillisOfPingInServer);
			_serverMillisOfLastPing = current; // 设置当前时间，作为下次的基准
			if (totalPingCount % _checkInterval == 0) {
				// 每隔一定次数检查一次，降低消息拥堵时的影响
				_pingLogger.info("ping记录：channel={}, client={}, server={}, playerId={}", this.channel.hashCode(), _clientLastPingRecord, _serverLastPingRecord,
						(this.player == null ? 0 : this.player.getID()));
//				if (Math.round(_totalIntervalMillisOfPingInServer * _allowMistake) < _totalIntervalMillisOfPingByClient) {
//					logger.warn("疑似作弊案例：totalIntervalMillis(C)={}, totalIntervalMills(S)={}, playerId={}", _totalIntervalMillisOfPingByClient, _totalIntervalMillisOfPingInServer, (this.player == null ? 0 : this.player.getID()));
//					if (_openBan) {
//						return false;
//					}
//				}
				long totalClient = _clientLastPingRecord.get(_clientLastPingRecord.size() - 1);
				if (Math.round(_totalIntervalMillisOfPingInServer * _allowMistake) < totalClient) {
					logger.warn("疑似作弊案例：totalIntervalMillis(C)={}, totalIntervalMills(S)={}, playerId={}", _totalIntervalMillisOfPingInServer, totalClient, (this.player == null ? 0 : this.player.getID()));
					if (_openBan) {
						return false;
					}
				}
				_clientLastPingRecord.clear();
				_serverLastPingRecord.clear();
			}
		}
		return true;
	}
}
