package com.koala.game;

import org.jdom.Element;

import com.koala.game.communication.KGameMessageEvent;
import com.koala.game.exception.KGameServerException;

/**
 * 本接口代表一个‘游戏模块’。模块的注册全部通过XML配置文件中{@code <Module>}标签块。 <br>
 * 每个模块必须要在配置文件中注册，引擎在启动的时候将自动注册，模块可接收到从底层传递的事件
 * 
 * @author AHONG
 * 
 */
public interface KGameModule {

	/**
	 * 初始化，加载模块时引擎会调用此方法，并将所需要的数据通过参数传递。
	 * 如果游戏逻辑抛出异常，启动失败，检查配置后再试
	 * 
	 * @param moduleName
	 *            模块的名称，实现类中记录起来就好，在{@link #getModuleName()}中返回
	 * @param isPlayerSessionListener
	 *            是否实现了{@link KGamePlayerSessionListener}接口
	 * @param eModuleSelfDefiningConfig
	 *            XML解析器jdom的{@link org.jdom.Element}对象类型，传递XML中
	 *            {@code <ModuleConfigs>} 标签定义的数据
	 * @throws KGameServerException
	 */
	void init(String moduleName, boolean isPlayerSessionListener,
			Element eModuleSelfDefiningConfig) throws KGameServerException;

	boolean isPlayerSessionListener();

	/**
	 * 返回模块名称
	 * 
	 * @return 模块名称
	 */
	String getModuleName();

	/**
	 * 服务器启动完成后调用，此时代表服务器的通信模块、每个功能模块都已经加载并初始化完毕； <br>
	 * 个别模块可能在这个时候需要加载些特殊数据或做些标记之类……
	 */
	void serverStartCompleted() throws KGameServerException;

	/**
	 * <p>
	 * 通信底层消息分发，每个模块直接做消息处理即可，本方法在线程池中调用执行属多线程环境。
	 * </p>
	 * <p>
	 * <strong>！注意返回值：如果返回true代表此消息已经被当前模块处理掉，无须再分发给其它模块；反之，
	 * 返回false代表这个消息不属于当前模块的消息， 让分发底层把这条消息发给其它模块试试。记得判断
	 * {@link KGameMessage#getMsgType()} 返回值，如果不属于自己模块的类型直接返回false！如下：</strong>
	 * </p>
	 * 
	 * <pre>
	 * 	{@code @Override}
	 * public boolean messageReceived(KGameMessageEvent msgEvent) throws KGameServerException{
	 *  	KGameMessage kmsg = msgEvent.getMessage();
	 *  	if (kmsg.getMsgType() != KGameMessage.MTYPE_GAMELOGIC) {
	 * 	      return false;
	 *  	}
	 *  	KGamePlayerSession ps = msgEvent.getPlayerSession();
	 *  	int msgID = kmsg.getMsgID();
	 *  	switch(msgID){
	 *  	  case 1000:
	 *  	    //do something
	 *  	    break;
	 *  	  default:// 其它没处理到的消息最大的可能就不是本模块的消息
	 *  	    return false;
	 *  	}
	 *  	return true;
	 * }
	 * </pre>
	 * 
	 * @param msgEvent
	 *            收到的消息事件，通过{@link KGameMessageEvent#getMessage()}方法即可获取
	 *            {@link KGameMessage}对象实例
	 * @return 如果返回<b>true</b>代表此消息已经被当前模块处理掉，无须再分发给其它模块；反之，返回<b>false</b>
	 *         代表这个消息不属于当前模块的消息， 让分发底层把这条消息发给其它模块试试
	 */
	boolean messageReceived(KGameMessageEvent msgEvent)
			throws KGameServerException;

	/**
	 * 当服务器遇到某些模块相关的异常时，通过本方法通知
	 * 
	 * @param ex
	 *            异常体
	 */
	void exceptionCaught(KGameServerException ex);

	/**
	 * 服务器关闭前的通知，个别模块可能需要做些关闭、状态保存之类的操作
	 */
	void serverShutdown() throws KGameServerException;
	
	/**
	 * 当服务器启动时，底层会询问每个模块是否初始化完毕，如果全部初始化完毕将最终开启对外连接。
	 * @return 返回本模块是否初始化完毕
	 */
	boolean isInitFinished();
}
