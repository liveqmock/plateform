/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.communication;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;

/**
 * 通道管道生成的工厂类【游戏逻辑无须理会】
 * 
 * @author AHONG
 */
public final class KGameServerChannelPipelineFactory implements
		ChannelPipelineFactory {

	private KGameCommunication communication;
	private ChannelHandler handler;
	private ExecutionHandler executionHandler;
	private IdleStateHandler idleStateHandler;

	public KGameServerChannelPipelineFactory(KGameCommunication communication,ChannelHandler handler,
			ExecutionHandler executionHandler, IdleStateHandler idleStateHandler) {
		this.communication=communication;
		this.handler = handler;
		this.executionHandler =executionHandler;
		this.idleStateHandler = idleStateHandler;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {

		ChannelPipeline pipeline = Channels.pipeline();

		// 心跳相关设置
		pipeline.addLast("timeout", idleStateHandler);
		pipeline.addLast("heartbeat", new KGameHeartbeat(communication));

		// 自定义编码解码器
		pipeline.addLast("kgame_decoder", new KGameMessageDecoder());
		pipeline.addLast("kgame_encoder", new KGameMessageEncoder());
 
		// 注意！！！！！在这里加入一个消息处理线程池，这是性能的关键！！！
		pipeline.addLast("kgame_execution", executionHandler);

		// 这里，我们将KGameServerCommunicationHandler处理器添加至默认的ChannelPipeline通道。
		// 任何时候当服务器接收到一个新的连接，一个新的ChannelPipeline管道对象将被创建，
		// 并且所有在这里添加的ChannelHandler对象将被添加至这个新的ChannelPipeline管道对象。
		// 这很像是一种浅拷贝操作（a shallow-copy operation）；
		// 所有的Channel通道以及其对应的ChannelPipeline实例将分享相同的KGameServerCommunicationHandler实例。
		// KGameServerCommunicationHandler handler = new
		// KGameServerCommunicationHandler();
		pipeline.addLast("handler", handler);

		return pipeline;
	}
}
