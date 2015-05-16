/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.communication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.util.CharsetUtil;

import com.koala.game.KGameMessage;
import com.koala.game.logging.KGameLogger;

/**
 * KOALA游戏约定的通信消息结构，含消息头和内容，采用二进制传输方式
 * 
 * @author AHONG
 */
class KGameMessageImpl implements KGameMessage {
//public static void main(String[] args) {
//	
////	System.out.println(0xFF);
////	System.out.println(0xF0);
////	System.out.println(0x0F);
//	
//	byte b1 = 0;
//	byte b2 = 15;
//	byte b = (byte) (((b1<<4)&0xFF)|((b2<<0)&0xFF));
//	System.out.println(Integer.toBinaryString(b1));
//	System.out.println(Integer.toBinaryString(b2));
//	System.out.println(Integer.toBinaryString(b));
//	//System.out.println(b);
//	byte b11 = (byte) ((b>>>4)&0x0F);
//	byte b22 = (byte) ((b>>>0)&0x0F);
//	System.out.println(b11);
//	System.out.println(b22);
//}

	private final static KGameLogger logger = KGameLogger
			.getLogger(KGameMessageImpl.class);
	ChannelBuffer backingChannelBuffer;
	private boolean inorout;
	private boolean isDuplicate;
//	private boolean _dataChange = false;

	// /////////2013-5-6增加发送后不能写的保护>>>>>>>>>>>>>>>>>>>>>>>
	private final AtomicBoolean cannotbewritten = new AtomicBoolean();

	private boolean iscannotbewritten() {
		if (cannotbewritten.get()) {
			logger.warn("message({}) can't be writed after send.",
					this.toString());
			return true;
		}
		return false;
	}

	void closewriteop() {
		cannotbewritten.set(true);
	}

	// /////////2013-5-6增加发送后不能写的保护<<<<<<<<<<<<<<<<<<<<<<<

	private KGameMessageImpl() {
	}

	/**
	 * 构造函数，通过此函数构造的对象表示是服务器发给客户端的‘发送消息’<br>
	 * 
	 * <b>OUT MSG</b>
	 * 
	 * @param msgType 
	 * @param clientType
	 * @param msgID
	 */
	KGameMessageImpl(byte msgType, byte clientType, int msgID) {
		// this.msgType = msgType;
		// this.clientType = clientType;
		// this.msgID = msgID;
		backingChannelBuffer = ChannelBuffers.dynamicBuffer();
		backingChannelBuffer.writeByte(msgType);
		backingChannelBuffer.writeByte(clientType);
		backingChannelBuffer.writeInt(msgID);
		backingChannelBuffer.writeInt(0);// 暂时不知道payload的长度，最后再set
		backingChannelBuffer.writeInt(0); // 校验码位置，暂时未知
		inorout = false;
	}

	/**
	 * 构造函数，通过本函数构造的对象表示从客户端发过来的消息，属于平台内部完成的调用<br>
	 * 
	 * <b>IN MSG</b>
	 * 
	 * @param srcBuf
	 */
	KGameMessageImpl(byte msgType, byte clientType, int msgID,
			int payloadLength, int checksum, ChannelBuffer srcBuf) {
		backingChannelBuffer = ChannelBuffers.dynamicBuffer(LENGTH_OF_HEADER
				+ payloadLength);
		backingChannelBuffer.writeByte(msgType);
		backingChannelBuffer.writeByte(clientType);
		backingChannelBuffer.writeInt(msgID);
		backingChannelBuffer.writeInt(payloadLength);
		backingChannelBuffer.writeInt(checksum);
		if (payloadLength > 0) {
			backingChannelBuffer.writeBytes(srcBuf, payloadLength);
		}
		backingChannelBuffer.skipBytes(LENGTH_OF_HEADER);// 要跳过消息头
		inorout = true;
	}
	
	private boolean checkIfNotAllowToChangeData(int index) {
		return isDuplicate && index > INDEX_CLIENTTYPE;
	}
	
	boolean isDuplicate() {
		return isDuplicate;
	}
	
//	boolean isDataChangeAfterDuplicate() {
//		return _dataChange;
//	}

	public KGameMessage duplicate() {
		return duplicate(true);
	}

	public KGameMessage duplicate(boolean resetReaderIndex) {
		KGameMessageImpl duplication = new KGameMessageImpl();
		duplication.inorout = this.inorout;
		duplication.isDuplicate = true;
//		duplication._dataChange = false;
		// System.out.println("0============================= "
		// + this.backingChannelBuffer.readerIndex() + ","
		// + this.backingChannelBuffer.writerIndex() + ";"
		// + this.backingChannelBuffer.readableBytes());
		duplication.backingChannelBuffer = this.backingChannelBuffer
				.duplicate();
		// duplication.backingChannelBuffer.writerIndex(duplication.backingChannelBuffer.readableBytes()-LENGTH_OF_HEADER);
		if (inorout && resetReaderIndex) {
			duplication.backingChannelBuffer.readerIndex(INDEX_START_PAYLOAD);
		}
		// System.out.println("1============================= "
		// + duplication.backingChannelBuffer.readerIndex() + ","
		// + duplication.backingChannelBuffer.writerIndex() + ";"
		// + duplication.backingChannelBuffer.readableBytes());
		return duplication;
	}

	@Override
	public KGameMessage copy() {

		KGameMessageImpl copy = new KGameMessageImpl();
		copy.inorout = this.inorout;

		// System.out.println("0-------------------- "
		// + this.backingChannelBuffer.readerIndex() + ","
		// + this.backingChannelBuffer.writerIndex() + ";"
		// + this.backingChannelBuffer.readableBytes() + "::" + this);

		//
		// copy.backingChannelBuffer = this.backingChannelBuffer.copy(0,
		// this.backingChannelBuffer.readableBytes());
		copy.backingChannelBuffer = ChannelBuffers
				.buffer(this.backingChannelBuffer.capacity());
		copy.backingChannelBuffer.writeByte(this.getMsgType());
		copy.backingChannelBuffer.writeByte(this.getClientType());
		copy.backingChannelBuffer.writeInt(this.getMsgID());
		copy.backingChannelBuffer.writeInt(this.getPayloadLength());
		copy.backingChannelBuffer.writeInt(this.getChecksum());
		copy.backingChannelBuffer.writeBytes(this.backingChannelBuffer,
				INDEX_START_PAYLOAD, this.getPayloadLength());

		// System.out.println("1-------------------- "
		// + copy.backingChannelBuffer.readerIndex() + ","
		// + copy.backingChannelBuffer.writerIndex() + ";"
		// + copy.backingChannelBuffer.readableBytes() + "::" + copy);

		// if (inorout) {
		// copy.backingChannelBuffer.setIndex(INDEX_PAYLOAD,
		// this.backingChannelBuffer.readableBytes()+LENGTH_OF_HEADER);
		// }

		if (inorout) {
			copy.skipBytes(LENGTH_OF_HEADER);
		}

		// System.out.println("2-------------------- "
		// + copy.backingChannelBuffer.readerIndex() + ","
		// + copy.backingChannelBuffer.writerIndex() + ";"
		// + copy.backingChannelBuffer.readableBytes() + "::" + copy);

		return copy;
	}
	
	public int getChecksum() {
		return backingChannelBuffer.getInt(INDEX_START_CHECKSUM);
	}

	public byte getEncryption(){
		return (byte) ((backingChannelBuffer.getByte(INDEX_MSGTYPE)>>>4)&0x0F);
	}
	
	public byte getUndefined(){
		return (byte) ((backingChannelBuffer.getByte(INDEX_CLIENTTYPE)>>>4)&0x0F);
	}
	
	@Override
	public void setEncryption(byte enc) {
		byte b = backingChannelBuffer.getByte(INDEX_MSGTYPE);
		byte b1 = (byte) (((enc<<4)&0xFF)+(b&0xFF));
		backingChannelBuffer.setByte(INDEX_MSGTYPE, b1);
	}

	@Override
	public void setUndefined(byte ud) {
		byte b = backingChannelBuffer.getByte(INDEX_CLIENTTYPE);
		byte b1 = (byte) (((ud<<4)&0xFF)+(b&0xFF));
		backingChannelBuffer.setByte(INDEX_CLIENTTYPE, b1);
	}

	public byte getMsgType() {
		// return msgType;
		//return backingChannelBuffer.getByte(INDEX_MSGTYPE);
		return (byte) ((backingChannelBuffer.getByte(INDEX_MSGTYPE)>>>0)&0x0F);
	}

	public byte getClientType() {
		// return clientType;
		//return backingChannelBuffer.getByte(INDEX_CLIENTTYPE);
		return (byte) ((backingChannelBuffer.getByte(INDEX_CLIENTTYPE)>>>0)&0x0F);
	}

	public int getMsgID() {
		// return msgID;
		return backingChannelBuffer.getInt(INDEX_MSGID);
	}

	public int getPayloadLength() {
		if (inorout) {
			// IN MSG
			return backingChannelBuffer.getInt(INDEX_PAYLOADLENGTH);
		} else {
			// OUT MSG
			return backingChannelBuffer.writerIndex() - LENGTH_OF_HEADER;
		}
	}

	// /////////////////////////////////////////////////////////////////////////////
	@Override
	public byte readByte() {
		return backingChannelBuffer.readByte();
	}

	@Override
	public short readUnsignedByte() {
		return backingChannelBuffer.readUnsignedByte();
	}

	@Override
	public short readShort() {
		return backingChannelBuffer.readShort();
	}

	@Override
	public int readUnsignedShort() {
		return backingChannelBuffer.readUnsignedShort();
	}

	@Override
	public int readInt() {
		return backingChannelBuffer.readInt();
	}

	@Override
	public long readUnsignedInt() {
		return backingChannelBuffer.readUnsignedInt();
	}

	@Override
	public long readLong() {
		return backingChannelBuffer.readLong();
	}

	@Override
	public char readChar() {
		return backingChannelBuffer.readChar();
	}

	@Override
	public float readFloat() {
		return backingChannelBuffer.readFloat();
	}

	@Override
	public double readDouble() {
		return backingChannelBuffer.readDouble();
	}

	@Override
	public void readBytes(byte[] dst) {
		backingChannelBuffer.readBytes(dst);
	}

	@Override
	public void readBytes(byte[] dst, int dstIndex, int length) {
		backingChannelBuffer.readBytes(dst, dstIndex, length);
	}

	@Override
	public void readBytes(OutputStream out, int length) throws IOException {
		backingChannelBuffer.readBytes(out, length);
	}

	@Override
	public void writeByte(int value) {
		if (iscannotbewritten()) {
			return;
		} else if (isDuplicate) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.writeByte(value);
	}

	@Override
	public void writeShort(int value) {
		if (iscannotbewritten()) {
			return;
		} else if (isDuplicate) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.writeShort(value);
	}

	@Override
	public void writeInt(int value) {
		if (iscannotbewritten()) {
			return;
		} else if (isDuplicate) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.writeInt(value);
	}

	@Override
	public void writeLong(long value) {
		if (iscannotbewritten()) {
			return;
		} else if (isDuplicate) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.writeLong(value);
	}

	@Override
	public void writeChar(int value) {
		if (iscannotbewritten()) {
			return;
		} else if (isDuplicate) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.writeChar(value);
	}

	@Override
	public void writeFloat(float value) {
		if (iscannotbewritten()) {
			return;
		} else if (isDuplicate) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.writeFloat(value);
	}

	@Override
	public void writeDouble(double value) {
		if (iscannotbewritten()) {
			return;
		} else if (isDuplicate) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.writeDouble(value);
	}

	@Override
	public void writeBytes(byte[] src) {
		if (iscannotbewritten()) {
			return;
		} else if (isDuplicate) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.writeBytes(src);
	}

	@Override
	public void writeBytes(byte[] src, int srcIndex, int length) {
		if (iscannotbewritten()) {
			return;
		} else if (isDuplicate) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.writeBytes(src, srcIndex, length);
	}

	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public String readUtf8String() {
		int len = backingChannelBuffer.readInt();
		if (len <= 0) {
			return "";
		}
		byte[] bs = new byte[len];
		backingChannelBuffer.readBytes(bs);
		String utf8string = new String(bs, CharsetUtil.UTF_8);
		return utf8string;
	}

	@Override
	public void writeUtf8String(String utf8string) {
		if (iscannotbewritten()) {
			return;
		}
		if (utf8string != null) {
			byte[] bs = utf8string.getBytes(CharsetUtil.UTF_8);
			if (bs != null) {
				backingChannelBuffer.writeInt(bs.length);
				backingChannelBuffer.writeBytes(bs);
				bs = null;
				return;
			}
		}
		backingChannelBuffer.writeInt(0);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("KGameMessage[");
		sb.append(getMsgType()).append(",").append(getClientType()).append(",")
				.append(getMsgID()).append(",").append(getPayloadLength())
				.append("]");
		return sb.toString();
	}

	public byte getByte(int index) {
		return backingChannelBuffer.getByte(index);
	}

	public short getUnsignedByte(int index) {
		return backingChannelBuffer.getUnsignedByte(index);
	}

	public short getShort(int index) {
		return backingChannelBuffer.getShort(index);
	}

	public int getUnsignedShort(int index) {
		return backingChannelBuffer.getUnsignedShort(index);
	}

	public int getInt(int index) {
		return backingChannelBuffer.getInt(index);
	}

	public long getUnsignedInt(int index) {
		return backingChannelBuffer.getUnsignedInt(index);
	}

	public long getLong(int index) {
		return backingChannelBuffer.getLong(index);
	}

	public char getChar(int index) {
		return backingChannelBuffer.getChar(index);
	}

	public float getFloat(int index) {
		return backingChannelBuffer.getFloat(index);
	}

	public double getDouble(int index) {
		return backingChannelBuffer.getDouble(index);
	}

	public void getBytes(int index, byte[] dst) {
		backingChannelBuffer.getBytes(index, dst);
	}

	public void getBytes(int index, byte[] dst, int dstIndex, int length) {
		backingChannelBuffer.getBytes(index, dst, dstIndex, length);
	}

	public void getBytes(int index, OutputStream out, int length)
			throws IOException {
		backingChannelBuffer.getBytes(index, out, length);
	}

	public void setByte(int index, int value) {
		if (checkIfNotAllowToChangeData(index)) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.setByte(index, value);
	}

	public void setShort(int index, int value) {
		if (checkIfNotAllowToChangeData(index)) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.setShort(index, value);
	}

	public void setInt(int index, int value) {
		if (checkIfNotAllowToChangeData(index)) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.setInt(index, value);
	}

	public void setLong(int index, long value) {
		if (checkIfNotAllowToChangeData(index)) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.setLong(index, value);
	}

	public void setChar(int index, int value) {
		if (checkIfNotAllowToChangeData(index)) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.setChar(index, value);
	}

	public void setFloat(int index, float value) {
		if (checkIfNotAllowToChangeData(index)) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.setFloat(index, value);
	}

	public void setDouble(int index, double value) {
		if (checkIfNotAllowToChangeData(index)) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.setDouble(index, value);
	}

	public void setBytes(int index, byte[] src) {
		if (checkIfNotAllowToChangeData(index)) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.setBytes(index, src);
	}

	public void setBytes(int index, byte[] src, int srcIndex, int length) {
		if (checkIfNotAllowToChangeData(index)) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.setBytes(index, src, srcIndex, length);
	}

	public int setBytes(int index, InputStream in, int length)
			throws IOException {
		if (checkIfNotAllowToChangeData(index)) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		int result = backingChannelBuffer.setBytes(index, in, length);
		return result;
	}

	public void skipBytes(int length) {
		backingChannelBuffer.skipBytes(length);
	}

	// ///////////Boolean/////////////////////////////////////

	@Override
	public boolean readBoolean() {
		return backingChannelBuffer.readByte() == FALSE ? false : true;
	}

	@Override
	public void writeBoolean(boolean value) {
		if (iscannotbewritten()) {
			return;
		} else if (isDuplicate) {
			throw new UnsupportedOperationException("duplicate msg is not allowed to change data");
		}
		backingChannelBuffer.writeByte(value ? TRUE : FALSE);
	}

	@Override
	public boolean getBoolean(int index) {
		return backingChannelBuffer.getByte(index) == FALSE ? false : true;
	}

	// ////////////////////////get & set////////////////////////////

	@Override
	public int writerIndex() {
		return backingChannelBuffer.writerIndex();
	}

	@Override
	public int readerIndex() {
		return backingChannelBuffer.readerIndex();
	}

	@Override
	public int readableBytes() {
		return backingChannelBuffer.readableBytes();
	}
}
