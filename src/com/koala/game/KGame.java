package com.koala.game;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffer;

import com.koala.game.communication.KGameCommunication;
import com.koala.game.communication.KGameHttpRequestSender.KGameHttpRequestResult;
import com.koala.game.communication.KGameMessagePayloadImpl;
import com.koala.game.gameserver.KGameServer;
import com.koala.game.player.KGamePlayerManager;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimer;
import com.koala.game.timer.KGameTimerTask;
import com.koala.game.util.StringUtil;

/**
 * API函数集合，绝大部分函数是引擎底层方法的<strong>快捷方式</strong>
 * 
 * @author AHONG
 * 
 */
public class KGame {

	public static KGameModule getGameModule(String moduleName) {
		return KGameServer.getInstance().getGameModule(moduleName);
	}

	/**
	 * 新建一个待发送的消息
	 * <p><b>!!!注意!!!如果要用到ZIP或base64加密，建立消息后需要调用{@link KGameMessage#setEncryption(byte)}方法，具体常量在{@link KGameMessage}中ENCRYPTION_开头的。（@20140210增加的功能）</b></p>
	 * 
	 * @param msgType
	 *            消息类型 {@link KGameMessage#MTYPE_PLATFORM} /
	 *            {@link KGameMessage#MTYPE_GAMELOGIC}
	 * @param clientType
	 *            客户端类型 {@link KGameMessage#CTYPE_ANDROID} /
	 *            {@link KGameMessage#CTYPE_IPAD} /
	 *            {@link KGameMessage#CTYPE_IPHONE} /
	 *            {@link KGameMessage#CTYPE_PC}
	 * @param msgID
	 *            消息ID （程序自定义）
	 * @return 一个可操作的消息实例
	 */
	public static KGameMessage newMessage(byte msgType, byte clientType,
			int msgID) {
		return KGameCommunication.newMessage(msgType, clientType, msgID);
	}

	/**
	 * 新建一个待发送的逻辑消息
	 * <p><b>!!!注意!!!如果要用到ZIP或base64加密，建立消息后需要调用{@link KGameMessage#setEncryption(byte)}方法，具体常量在{@link KGameMessage}中ENCRYPTION_开头的。（@20140210增加的功能）</b></p>
	 * @param clientType
	 *            客户端类型 {@link KGameMessage#CTYPE_ANDROID} /
	 *            {@link KGameMessage#CTYPE_IPAD} /
	 *            {@link KGameMessage#CTYPE_IPHONE} /
	 *            {@link KGameMessage#CTYPE_PC}
	 * @param msgID
	 *            消息ID （程序自定义）
	 * @return 一个可操作的消息实例
	 */
	public static KGameMessage newLogicMessage(byte clientType, int msgID) {
		return KGameCommunication.newMessage(KGameMessage.MTYPE_GAMELOGIC,
				clientType, msgID);
	}

	/**
	 * 新建一个待发送的逻辑消息
	 * <p><b>!!!注意!!!如果要用到ZIP或base64加密，建立消息后需要调用{@link KGameMessage#setEncryption(byte)}方法，具体常量在{@link KGameMessage}中ENCRYPTION_开头的。（@20140210增加的功能）</b></p>
	 * 
	 * @param msgID
	 *            消息ID （程序自定义）
	 * @return 一个可操作的消息实例
	 */
	public static KGameMessage newLogicMessage(int msgID) {
		return KGameCommunication.newMessage(KGameMessage.MTYPE_GAMELOGIC,
				KGameMessage.CTYPE_ANDROID, msgID);
	}

	/**
	 * 获得一条PING消息，一般情况下游戏逻辑无须使用
	 * 
	 * @return 一个待发送的消息实例
	 */
	public static KGameMessage getPingMessage() {
		return KGameCommunication.getPingMessage();
	}

	/**
	 * 通过玩家ID获取玩家会话对象{@link KGamePlayerSession}
	 * 
	 * @param playerID
	 *            玩家ID
	 * @return 如果在线返回对应的玩家会话对象{@link KGamePlayerSession}，不在线则返回null
	 */
	public static KGamePlayerSession getPlayerSession(long playerID) {
		return KGameServer.getInstance().getPlayerManager()
				.getPlayerSession(playerID);
	}

	/**
	 * 玩家管理器
	 * 
	 * @return
	 */
	public static KGamePlayerManager getPlayerManager() {
		return KGameServer.getInstance().getPlayerManager();
	}

	/**
	 * 时效任务，定时器
	 * 
	 * @return
	 */
	public static KGameTimer getTimer() {
		return KGameServer.getInstance().getTimer();
	}

	/**
	 * 向定时器{@link KGameTimer}提交一个新的定时任务{@link KGameTimerTask}
	 * 。加入后引擎底层将这个任务包装成一个报时信号{@link KGameTimeSignal}，当时间到达时产生信号，并通过
	 * {@link KGameTimerTask#onTimeSignal(KGameTimeSignal)}向使用者回调通知。<br>
	 * <br>
	 * 效果跟{@link #getTimer()}然后调用
	 * {@link KGameTimer#newTimeSignal(KGameTimerTask, long, TimeUnit)}是一样的.
	 * 
	 * @param task
	 *            待执行的定时任务
	 * @param delay
	 *            第一次执行延迟，从提交这一刻开始算起
	 * @param unit
	 *            延迟的时间单位
	 * @return 新的报时信号，报时信号中包含一些常用的方法
	 * @throws IllegalStateException
	 *             if this timer has been {@linkplain #stop() stopped} already
	 */
	public static KGameTimeSignal newTimeSignal(KGameTimerTask task,
			long delay, TimeUnit unit) {
		return KGameServer.getInstance().getTimer()
				.newTimeSignal(task, delay, unit);
	}

	/**
	 * 取得当前GS的ID
	 * 
	 * @return
	 */
	public static int getGSID() {
		return KGameServer.getInstance().getGSID();
	}
	
	/**
	 * 
	 * 获取服务器第一次启动的时间
	 * 
	 * @return
	 */
	public static long getGSFirstStartTime() {
		return KGameServer.getInstance().getGSFirstStartTime();
	}

	/**
	 * 把一个按约定格式写入的byte[]转换成可读取的对象。具体格式跟{@link KGameMessage}一致
	 * 
	 * @param payloadBytes
	 *            待包装的字节数组
	 * @param offset
	 *            数据起点
	 * @param length
	 *            数据长度
	 * @return
	 */
	public static KGameMessagePayload wrappedMessagePayload(byte[] payloadBytes,
			int offset, int length) {
		return new KGameMessagePayloadImpl(payloadBytes, offset, length);
	}
	
	public static int calculateChecksum(ChannelBuffer buffer, int offset, int bufferTotalLength) {
//		int checksum = 0;
//		int msgId = buffer.getInt(KGameMessage.INDEX_MSGID);
//		byte[] checksumArray = new byte[4];
//		if (bufferTotalLength > offset) {
//			checksum += ((buffer.getByte(offset) & 0xff) << 24);
//			checksumArray[0] = buffer.getByte(offset);
//		}
//		if (bufferTotalLength > (offset+1)) {
//			checksum += ((buffer.getByte(offset + 1) & 0xff) << 16);
//			checksumArray[1] = buffer.getByte(offset + 1);
//		}
//		if (bufferTotalLength == (offset + 3)) {
//			checksum += ((buffer.getByte(bufferTotalLength - 1) & 0xff) << 8);
//			checksumArray[2] = buffer.getByte(bufferTotalLength - 1);
//		} else if (bufferTotalLength > (offset + 3)) {
//			checksum += ((buffer.getByte(bufferTotalLength - 2) & 0xff) << 8);
//			checksum += ((buffer.getByte(bufferTotalLength - 1) & 0xff));
//			checksumArray[2] = buffer.getByte(bufferTotalLength - 2);
//			checksumArray[3] = buffer.getByte(bufferTotalLength - 1);
//		}
//		if (msgId == 1902 && buffer.readableBytes() > KGameMessage.LENGTH_OF_HEADER) {
//			byte[] array = new byte[buffer.readableBytes() - KGameMessage.LENGTH_OF_HEADER];
//			buffer.getBytes(KGameMessage.LENGTH_OF_HEADER, array);
//			System.out.println(StringUtil.format("msgId={}, checksum={}, checksumArray={}, buffer={}", msgId, checksum, Arrays.toString(checksumArray), Arrays.toString(array)));
//		}
//		return checksum;
		int checksum = 0;
		if (bufferTotalLength > offset) {
			checksum += ((buffer.getByte(offset) & 0xff) << 24);
		}
		if (bufferTotalLength > (offset+1)) {
			checksum += ((buffer.getByte(offset + 1) & 0xff) << 16);
		}
		if (bufferTotalLength == (offset + 3)) {
			checksum += ((buffer.getByte(bufferTotalLength - 1) & 0xff) << 8);
		} else if (bufferTotalLength > (offset + 3)) {
			checksum += ((buffer.getByte(bufferTotalLength - 2) & 0xff) << 8);
			checksum += ((buffer.getByte(bufferTotalLength - 1) & 0xff));
		}
		return checksum;
	}
	/**
	 * 
	 * 发送一个http请求
	 * 
	 * @param urlAddress http请求地址
	 * @param paraMap 参数列表
	 * @param needEncodeKeys 需要使用URLEncoder.encode的参数
	 * @param charSetName 字符编码（传NULL表示使用UTF-8）
	 * @return
	 * @throws Exception
	 */
	public static Future<KGameHttpRequestResult> sendPostRequestUseJSON(String urlAddress, Map<String, Object> paraMap, List<String> needEncodeKeys, String charSetName) throws Exception {
		if(charSetName == null || charSetName.length() == 0) {
			charSetName = "UTF-8";
		}
		return KGameServer.getInstance().getHttpSender().sendPostRequestUseJSON(urlAddress, paraMap, needEncodeKeys, charSetName);
	}
	
	/**
	 * 
	 * 发送一个http请求
	 * 
	 * @param urlAddress http请求地址
	 * @param paraMap 参数列表
	 * @param needEncodeKeys 需要使用URLEncoder.encode的参数
	 * @param charSetName 字符编码（传NULL表示使用UTF-8）
	 * @return
	 * @throws Exception
	 */
	public static Future<KGameHttpRequestResult> sendPostRequest(String urlAddress, Map<String, Object> paraMap, List<String> needEncodeKeys, String charSetName) throws Exception {
		if(charSetName == null || charSetName.length() == 0) {
			charSetName = "UTF-8";
		}
		return KGameServer.getInstance().getHttpSender().sendPostRequest(urlAddress, paraMap, needEncodeKeys, charSetName);
	}
}
