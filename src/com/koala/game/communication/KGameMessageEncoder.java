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
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.koala.game.KGame;
import com.koala.game.KGameMessage;
import com.koala.game.util.StringUtil;
import com.koala.game.util.ZipUtil;

/**
 * KGAME中的编码器，将自定义消息对象{@link KGameMessage}转换成ChannelBuffer让Netty写出去
 * 
 * @author AHONG
 */
public final class KGameMessageEncoder extends OneToOneEncoder {

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel,
			Object msg) throws Exception {

		if (!(msg instanceof KGameMessage)) {
			return msg;
		}

		KGameMessageImpl kmsg = (KGameMessageImpl) msg;
		
		kmsg.closewriteop();//2013-5-6增加发送后不能写的保护
		
//		int capacity = KGameMessage.LENGTH_OF_HEADER + kmsg.getPayloadLength();
//		ChannelBuffer buffer = ChannelBuffers.buffer(capacity);
//		// Header====================================
//		buffer.writeByte(kmsg.getMsgType());
//		buffer.writeByte(kmsg.getClientType());
//		buffer.writeInt(kmsg.getMsgID());
//		buffer.writeInt(kmsg.getPayloadLength());
//		// Payload===================================
//		// Transfers the specified source buffer's data to this buffer starting
//		// at the current writerIndex until the source buffer becomes
//		// unreadable, and increases the writerIndex by the number of the
//		// transferred bytes. This method is basically same with
//		// writeBytes(ChannelBuffer, int, int), except that this method
//		// increases the readerIndex of the source buffer by the number of the
//		// transferred bytes while writeBytes(ChannelBuffer, int, int) does not.
//		buffer.writeBytes(((KGameMessageImpl) kmsg).backingChannelBuffer);
//		return buffer;
		
		ChannelBuffer returnBuf;
		
		int writerIndex = kmsg.backingChannelBuffer.writerIndex();
		int payloadLength = writerIndex - KGameMessage.LENGTH_OF_HEADER;
		
		//2014-2-10增加对消息的加密
		byte enc = kmsg.getEncryption();
		if(enc==KGameMessage.ENCRYPTION_ZIP){
            byte[] in = new byte[kmsg.backingChannelBuffer.readableBytes()-KGameMessage.LENGTH_OF_HEADER];
            kmsg.backingChannelBuffer.getBytes(KGameMessage.LENGTH_OF_HEADER, in);
			byte[] compressed = ZipUtil.compress(in);//ZipUtil.zipBytes(in);
			int payloadlen_zip = compressed.length+4+4;//加两个int表示压缩前后的长度
			returnBuf = ChannelBuffers.buffer(payloadlen_zip + KGameMessage.LENGTH_OF_HEADER);
			returnBuf.writeBytes(kmsg.backingChannelBuffer, 0, KGameMessage.LENGTH_OF_HEADER);
			returnBuf.writeInt(in.length);//原始长度
			returnBuf.writeInt(compressed.length);//压缩后长度
			returnBuf.writeBytes(compressed);//压缩后的内容
			//！！！非常重要：：：发送之前要先更新Payload Length
			returnBuf.setInt(KGameMessage.INDEX_PAYLOADLENGTH, payloadlen_zip);
//			System.out.println("encoder  zip,beforelen=" + payloadLength + ",afterlen=" + payloadlen_zip);
		}
		else if(enc==KGameMessage.ENCRYPTION_BASE64){
			ChannelBuffer payloadbuf_b64 = Base64.encode(kmsg.backingChannelBuffer, KGameMessage.LENGTH_OF_HEADER, payloadLength);
			int payloadlen_b64 = payloadbuf_b64.readableBytes();
//			System.out.println("encoder  b64,beforelen="+payloadLength+",afterlen="+payloadlen_b64);
			returnBuf = ChannelBuffers.buffer(payloadlen_b64 + KGameMessage.LENGTH_OF_HEADER);
			returnBuf.writeBytes(kmsg.backingChannelBuffer, 0, KGameMessage.LENGTH_OF_HEADER);
			returnBuf.writeBytes(payloadbuf_b64, 0, payloadlen_b64);
			//！！！非常重要：：：发送之前要先更新Payload Length
			returnBuf.setInt(KGameMessage.INDEX_PAYLOADLENGTH, payloadlen_b64);
//			System.out.println("encoder  returnBuf.readableBytes="+returnBuf.readableBytes());
		}
		else{//KGameMessage.ENCRYPTION_NONE
			//！！！非常重要：：：发送之前要先更新Payload Length
			kmsg.backingChannelBuffer.setInt(KGameMessage.INDEX_PAYLOADLENGTH, payloadLength);
			returnBuf = kmsg.backingChannelBuffer;
//			if (kmsg.isDuplicate() && payloadLength > 0 && kmsg.isDataChangeAfterDuplicate()) {
//				returnBuf = kmsg.backingChannelBuffer.copy();
//			} else {
//				returnBuf = kmsg.backingChannelBuffer;
//			}
		}
		int checksum = KGame.calculateChecksum(returnBuf, KGameMessage.LENGTH_OF_HEADER, returnBuf.readableBytes());
		returnBuf.setInt(KGameMessage.INDEX_START_CHECKSUM, checksum);
		return returnBuf;
	}

	public static int calculateChecksum(ChannelBuffer buffer, int offset, int bufferTotalLength) {
		int checksum = 0;
		int msgId = buffer.getInt(KGameMessage.INDEX_MSGID);
		byte[] checksumArray = new byte[4];
		if (bufferTotalLength > offset) {
			checksum += ((buffer.getByte(offset) & 0xff) << 24);
			checksumArray[0] = buffer.getByte(offset);
		}
		if (bufferTotalLength > (offset+1)) {
			checksum += ((buffer.getByte(offset + 1) & 0xff) << 16);
			checksumArray[1] = buffer.getByte(offset + 1);
		}
		if (bufferTotalLength == (offset + 3)) {
			checksum += ((buffer.getByte(bufferTotalLength - 1) & 0xff) << 8);
			checksumArray[2] = buffer.getByte(bufferTotalLength - 1);
		} else if (bufferTotalLength > (offset + 3)) {
			checksum += ((buffer.getByte(bufferTotalLength - 2) & 0xff) << 8);
			checksum += ((buffer.getByte(bufferTotalLength - 1) & 0xff));
			checksumArray[2] = buffer.getByte(bufferTotalLength - 2);
			checksumArray[3] = buffer.getByte(bufferTotalLength - 1);
		}
		if (msgId == 1908 && buffer.readableBytes() > KGameMessage.LENGTH_OF_HEADER) {
			byte[] array = new byte[buffer.readableBytes() - KGameMessage.LENGTH_OF_HEADER];
			buffer.getBytes(KGameMessage.LENGTH_OF_HEADER, array);
			System.out.println(StringUtil.format("msgId={}, checksum={}, checksumArray={}, buffer={}", msgId, checksum, Arrays.toString(checksumArray), Arrays.toString(array)));
		}
		return checksum;
//		int checksum = 0;
//		if (bufferTotalLength > offset) {
//			checksum += ((buffer.getByte(offset) & 0xff) << 24);
//		}
//		if (bufferTotalLength > (offset+1)) {
//			checksum += ((buffer.getByte(offset + 1) & 0xff) << 16);
//		}
//		if (bufferTotalLength == (offset + 3)) {
//			checksum += ((buffer.getByte(bufferTotalLength - 1) & 0xff) << 8);
//		} else if (bufferTotalLength > (offset + 3)) {
//			checksum += ((buffer.getByte(bufferTotalLength - 2) & 0xff) << 8);
//			checksum += ((buffer.getByte(bufferTotalLength - 1) & 0xff));
//		}
//		return checksum;
	}
}
