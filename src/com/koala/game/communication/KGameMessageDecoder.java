/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.communication;

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;

import com.koala.game.KGame;
import com.koala.game.KGameMessage;
import com.koala.game.logging.KGameLogger;
import com.koala.game.util.ZipUtil;

/**
 * 消息解码器，采用POJO方式将ChannelBuffer转换成自定义消息{@link KGameMessage}格式。<br>
 * 在整个KGAME里面只要使用{@link KGameMessage}对象无须出现ChannelBuffer
 * 
 * @author AHONG
 */
public final class KGameMessageDecoder extends FrameDecoder {
//	org.jboss.netty.handler.codec.base64.Base64
	private static final Logger _logger = KGameLogger.getLogger(KGameMessageDecoder.class);
	private static final int _200KB = 204800;
	private boolean _nevenPrint = true;
	
	private void clearBuffer(ChannelBuffer buffer) {
		int readableBytes = buffer.readableBytes();
		if (readableBytes > 0) {
			buffer.skipBytes(readableBytes);
		}
	}
	
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
			ChannelBuffer buffer) throws Exception {
		
//		byte[] data = new byte[buffer.readableBytes()];
//		buffer.getBytes(buffer.readerIndex(), data);
//		if (data.length < 20480) {
//			_logger.info("channel:{}, buffer.readerIndex={}, buffer.readableBytes()={}, buffer:{}", channel.hashCode(), buffer.readerIndex(), buffer.readableBytes() , Arrays.toString(data));
//		}

//		 System.out.println("@decode buffer.readableBytes() = "+buffer.readableBytes());

		// Make sure if the length field was received.
		if (buffer.readableBytes() < KGameMessageImpl.LENGTH_OF_HEADER) {
			// The length field was not received yet - return null.
			// This method will be invoked again when more packets are
			// received and appended to the buffer.
//			_logger.error("channel:{}, buffer.readableBytes()({}) < KGameMessageImpl.LENGTH_OF_HEADER({})", channel.hashCode(), buffer.readableBytes(), KGameMessageImpl.LENGTH_OF_HEADER);
			return null;
		}
		
		KGameMessageImpl msg;

		// 记录当前读取的位置
		// Mark the current buffer position before reading the length field
		// because the whole frame might not be in the buffer yet.
		// We will reset the buffer position to the marked position if
		// there's not enough bytes in the buffer.
		buffer.markReaderIndex();

		byte msgType = buffer.readByte();// 消息类型
		byte clientType = buffer.readByte();// 客户端类型
		int msgID = buffer.readInt();// 消息ID

		// KGameMessageImpl msg = new KGameMessageImpl(buffer.readByte(),
		// buffer.readByte(),
		// buffer.readInt());
		// System.out.println(msg);

		int payloadLength = buffer.readInt();// 内容长度
//		System.out.println(msgType+","+clientType+","+msgID+","+payloadLength);
		int checksum = buffer.readInt(); // 校验
		if (payloadLength > 0) {
			// Wait until the whole data is available.
			// Make sure if there's enough bytes in the buffer.
			if (payloadLength > _200KB) {
				try {
					_logger.warn("服务器收到一条payloadLength超过200k的消息！payloadLength={}, buffer.readableBytes()={}, channel={}, msgId={}", payloadLength, buffer.readableBytes(), channel.hashCode(), msgID);
					if (_nevenPrint) {
						_nevenPrint = false;
						byte[] data = new byte[buffer.readableBytes()];
						buffer.getBytes(buffer.readerIndex(), data);
						if (data.length < 20480) {
							_logger.info("channel:{}, buffer:{}", channel.getId(), Arrays.toString(data));
						}
					}
					this.clearBuffer(buffer);
				} finally {
					channel.close(); // 2015-01-21 修改：收到这种类型的消息，直接断开channel的连接，因为这时候流已经乱了
				}
				return null;
			}
			if (buffer.readableBytes() < payloadLength) {
				// payload数据还没完整，重置读取位置
				// The whole bytes were not received yet - return null.
				// This method will be invoked again when more packets are
				// received and appended to the buffer.

				// Reset to the marked position to read the length field again
				// next time.
				buffer.resetReaderIndex();
				return null;
			}
			if (payloadLength > 0) {
				ChannelBuffer copyBuffer = buffer.copy(buffer.readerIndex(), payloadLength); // buffer本身是全局数据，可能一直在受netty的影响，在传进去校验的时候会发生变化，所以copy一份出来，免得数据出现差异
				int checkResult = KGame.calculateChecksum(copyBuffer, 0, payloadLength);
				copyBuffer.clear(); // 校验完之后及时清理
				copyBuffer = null;
				if (checkResult != checksum) {
					try {
						byte[] array = new byte[payloadLength];
						buffer.getBytes(buffer.readerIndex(), array);
						_logger.error("检验不通过！消息id：{}，客户端校验码：{}，服务器校验码：{}，readerIndex={}，数据：{}", msgID, checksum, checkResult, buffer.readerIndex(), array);
						clearBuffer(buffer);
					} finally {
						channel.close();
					}
					return null;
				}
			} else {
				if (checksum != 0) {
					try {
						_logger.error("消息校验不通过！payloadLength为0，但是客户端校验码不为0：{}！", checksum);
						this.clearBuffer(buffer);
					} finally {
						channel.close();
					}
					return null;
				}
			}
			
//			System.out.println("================decode=================");
//			System.out.println("msgType="+(msgType&0x0F));
//			System.out.println("clientType="+(clientType&0x0F));
//			System.out.println("encryption="+((msgType>>>4)&0x0F));
//			System.out.println("msgID="+msgID);
//			System.out.println("payloadLength="+payloadLength);
//			System.out.println("================decode=================");

			// There's enough bytes in the buffer. Read it.
			
			//2014-02-10增加对base64、zip等支持
			byte enc = (byte) ((msgType>>>4)&0x0F);
			if(enc==KGameMessage.ENCRYPTION_ZIP){
				int len_uncompressed = buffer.readInt();
				int len_compressed = buffer.readInt();
				System.out.println("decoder  unzip,len_uncompressed="+len_uncompressed+",len_compressed="+len_compressed);
				byte[] bytes_compressed = new byte[len_compressed];
				buffer.readBytes(bytes_compressed);
				byte[] bytes_uncompressed = ZipUtil.decompress(bytes_compressed);//ZipUtil.unzipBytes(bytes_compressed, len_uncompressed);
				ChannelBuffer buf_tmp = ChannelBuffers.buffer(len_uncompressed);
				buf_tmp.writeBytes(bytes_uncompressed);
				System.out.println("decoder  unzip,bytes_uncompressed.length="+bytes_uncompressed.length+",buf_tmp.readableBytes="+buf_tmp.readableBytes());
				msg = new KGameMessageImpl(msgType, clientType, msgID, buf_tmp.readableBytes(), checksum, buf_tmp);
			}
			else if(enc==KGameMessage.ENCRYPTION_BASE64){
//				ChannelBuffer buf_b64 = ChannelBuffers.buffer(payloadLength);
//				buffer.readBytes(buf_b64, payloadLength);
				ChannelBuffer buf_ub64 = Base64.decode(buffer,buffer.readerIndex(),payloadLength);
				buffer.skipBytes(payloadLength);//上面的方法只是get出来没有read数据的
				int payloadlen_ub64 = buf_ub64.readableBytes();
				System.out.println("decoder  base64,payloadLength="+payloadLength+",payloadlen_ub64="+payloadlen_ub64);
				msg = new KGameMessageImpl(msgType, clientType, msgID, payloadlen_ub64, checksum, buf_ub64);
			}
			else{
				msg = new KGameMessageImpl(msgType, clientType, msgID, payloadLength, checksum, buffer);
//				_logger.info("消息解析完毕, channel={}, buffer.readerIndex={}", channel.hashCode(), buffer.readerIndex());
			}
			
			//返回含payload的MSG
			//return new KGameMessageImpl(msgType, clientType, msgID, payloadLength, buffer);
		}else{
			if(payloadLength < 0) {
				_logger.error("payloadLength < 0, msgId={}, msgType={}, clientType={}, payloadLength={}", msgID, msgType, clientType, payloadLength);
			}
			//返回没有payload的MSG
			msg = new KGameMessageImpl(msgType, clientType, msgID, 0, checksum, null);
		}
		//返回没有payload的MSG
		//return new KGameMessageImpl(msgType, clientType, msgID, 0, null);
		return msg;
	}
	
//	private int calculateChecksum(ChannelBuffer buffer, int length) {
//		int checksum = 0;
//		int offset = KGameMessage.LENGTH_OF_HEADER;
//		if (length > offset) {
//			checksum += ((buffer.getByte(offset) & 0xff) << 24);
//		}
//		if (length > (offset+1)) {
//			checksum += ((buffer.getByte(offset + 1) & 0xff) << 16);
//		}
//		if (length == (offset + 3)) {
//			checksum += ((buffer.getByte(length - 1) & 0xff) << 8);
//		} else if (length > (offset + 3)) {
//			checksum += ((buffer.getByte(length - 2) & 0xff) << 8);
//			checksum += ((buffer.getByte(length - 1) & 0xff));
//		}
//		return checksum;
//	}
}
