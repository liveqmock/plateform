/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//TODO 【KGameMessage】clone、release、set之类的方法在具体使用过程再完善
/**
 * KOALA游戏约定的通信消息结构，含消息头和内容，采用二进制传输方式...<br>
 * <br>
 * <b> !!!注意!!!<br>
 * (1)当中get/set方法都不改变消息内部的指针，在没有完全100%确定index的时候切勿随便使用set方法。<br>
 * (2)而read/write方法都会改变消息内部的指针。<br>
 * (3)内容长度(payloadLength)在发送时不需要自己修改，平台底层在发送前会更新这个值，故使用者无需理会。<br>
 * (4)常量命名的约定：‘MID_’开头代表‘msgID’、‘MTYPE_’开头代表‘msgType’、‘CTYPE_’开头代表‘client type’
 * 
 * </b>
 * 
 * @author AHONG
 */
public interface KGameMessage {

	public final static byte FALSE = 0;
	public final static byte TRUE = 1;

	public final static byte MTYPE_PLATFORM = 0;
	public final static byte MTYPE_GAMELOGIC = 1;

	public final static byte CTYPE_ANDROID = 0;
	public final static byte CTYPE_IPHONE = 1;
	public final static byte CTYPE_IPAD = 2;
	public final static byte CTYPE_PC = 3;
	public final static byte CTYPE_GS = 4;// 对FE来说GS也是一种客户端
	public final static byte CTYPE_PAYMENT = 5;//支付服务器<->GS
	
	public final static byte ENCRYPTION_NONE = 0;
	public final static byte ENCRYPTION_ZIP = 1;
	public final static byte ENCRYPTION_BASE64 = 2;

	public final static int LENGTH_OF_HEADER = 10 + 4; // 2015-01-22 加了个校验码

	// ///////////////////////////////////Header/////////////////////////////////////////////
	/*
	 * 消息头： 长度=10 byte private byte msgType; private byte clientType; private
	 * int msgID; //private int payloadLength;
	 */
	public final static int INDEX_MSGTYPE = 0;// 0
	public final static int INDEX_CLIENTTYPE = 1;// 1
	public final static int INDEX_MSGID = 2;// 2|3|4|5
	public final static int INDEX_PAYLOADLENGTH = 6;// 6|7|8|9
	public final static int INDEX_START_CHECKSUM = 10; // 10|11|12|13
	public final static int INDEX_START_PAYLOAD = 14; 

	/**获取消息内容的加密方式（如base64、zip等）*/
	byte getEncryption();
	
	/**设置消息内容的加密方式，！！注意：本值只能为小于16的正整数！！*/
	void setEncryption(byte enc);
	
	/**获取自定义的字段值*/
	byte getUndefined();
	
	/**设置自定义字段值，！！注意：本值只能为小于16的正整数！！*/
	void setUndefined(byte ud);
	
	/** 获取消息类型，可重复调用 */
	byte getMsgType();

	/** 获取客户端类型，可重复调用 */
	byte getClientType();

	/** 获取消息ID，可重复调用 */
	int getMsgID();

	/** 获取消息内容的长度，可重复调用 */
	int getPayloadLength();

	/**
	 * 对当前{@link KGameMessage}做一个副本。！！{@link #duplicate()}方法跟{@link #copy()}
	 * 起到的效果是一样，但其实内部实现不同： {@link #duplicate()}方法生成出来的新的{@link KGameMessage}
	 * 实例跟原实例的数据内容其实是共享的，只是各自维护一套indexs。 而{@link #copy()}是对原{@link KGameMessage}
	 * 对象的完全的复制。所以一般情况下都使用{@link #duplicate()}方法，更省内存。
	 * 
	 * @return 新的{@link KGameMessage}副本
	 */
	KGameMessage duplicate();

	/**
	 * 对当前{@link KGameMessage}做一个副本。！！{@link #duplicate()}方法跟{@link #copy()}
	 * 起到的效果是一样，但其实内部实现不同： {@link #duplicate()}方法生成出来的新的{@link KGameMessage}
	 * 实例跟原实例的数据内容其实是共享的，只是各自维护一套indexs。 而{@link #copy()}是对原{@link KGameMessage}
	 * 对象的完全的复制。所以一般情况下都使用{@link #duplicate()}方法，更省内存。
	 * 
	 * @param resetReaderIndex
	 *            对新产生的{@link KGameMessage}副本的读取index是否要重设，如果原
	 *            {@link KGameMessage}
	 *            实例已经读取过部分数据，那么如果本值为false的话在副本中将不能直接读取到前面已经被读取过的数据。(无参数版方法
	 *            {@link #duplicate()}此参数为ture)
	 * @return 新的{@link KGameMessage}副本
	 */
	KGameMessage duplicate(boolean resetReaderIndex);

	/**
	 * 对当前{@link KGameMessage}实例做完全的克隆。！！{@link #duplicate()}方法跟{@link #copy()}
	 * 起到的效果是一样，但其实内部实现不同： {@link #duplicate()}方法生成出来的新的{@link KGameMessage}
	 * 实例跟原实例的数据内容其实是共享的，只是各自维护一套indexs。 而{@link #copy()}是对原{@link KGameMessage}
	 * 对象的完全的复制。所以一般情况下都使用{@link #duplicate()}方法，更省内存。
	 * 
	 * @return 新的{@link KGameMessage}克隆
	 */
	KGameMessage copy();

	/**
	 * Increases the current {@code readerIndex} by the specified {@code length}
	 * in this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code length} is greater than {@code this.readableBytes}
	 */
	void skipBytes(int length);

	// ///////////////////////////////////Read/////////////////////////////////////////////
	/**
	 * Gets a byte at the current {@code readerIndex} and increases the
	 * {@code readerIndex} by {@code 1} in this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.readableBytes} is less than {@code 1}
	 */
	byte readByte();

	/**
	 * Gets an unsigned byte at the current {@code readerIndex} and increases
	 * the {@code readerIndex} by {@code 1} in this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.readableBytes} is less than {@code 1}
	 */
	short readUnsignedByte();

	/**
	 * Gets a 16-bit short integer at the current {@code readerIndex} and
	 * increases the {@code readerIndex} by {@code 2} in this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.readableBytes} is less than {@code 2}
	 */
	short readShort();

	/**
	 * Gets an unsigned 16-bit short integer at the current {@code readerIndex}
	 * and increases the {@code readerIndex} by {@code 2} in this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.readableBytes} is less than {@code 2}
	 */
	int readUnsignedShort();

	boolean readBoolean();

	/**
	 * Gets a 32-bit integer at the current {@code readerIndex} and increases
	 * the {@code readerIndex} by {@code 4} in this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.readableBytes} is less than {@code 4}
	 */
	int readInt();

	/**
	 * Gets an unsigned 32-bit integer at the current {@code readerIndex} and
	 * increases the {@code readerIndex} by {@code 4} in this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.readableBytes} is less than {@code 4}
	 */
	long readUnsignedInt();

	/**
	 * Gets a 64-bit integer at the current {@code readerIndex} and increases
	 * the {@code readerIndex} by {@code 8} in this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.readableBytes} is less than {@code 8}
	 */
	long readLong();

	/**
	 * Gets a 2-byte UTF-16 character at the current {@code readerIndex} and
	 * increases the {@code readerIndex} by {@code 2} in this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.readableBytes} is less than {@code 2}
	 */
	char readChar();

	/**
	 * Gets a 32-bit floating point number at the current {@code readerIndex}
	 * and increases the {@code readerIndex} by {@code 4} in this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.readableBytes} is less than {@code 4}
	 */
	float readFloat();

	/**
	 * Gets a 64-bit floating point number at the current {@code readerIndex}
	 * and increases the {@code readerIndex} by {@code 8} in this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.readableBytes} is less than {@code 8}
	 */
	double readDouble();

	/**
	 * Transfers this buffer's data to the specified destination starting at the
	 * current {@code readerIndex} and increases the {@code readerIndex} by the
	 * number of the transferred bytes (= {@code dst.length}).
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code dst.length} is greater than
	 *             {@code this.readableBytes}
	 */
	void readBytes(byte[] dst);

	/**
	 * Transfers this buffer's data to the specified destination starting at the
	 * current {@code readerIndex} and increases the {@code readerIndex} by the
	 * number of the transferred bytes (= {@code length}).
	 * 
	 * @param dstIndex
	 *            the first index of the destination
	 * @param length
	 *            the number of bytes to transfer
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code dstIndex} is less than {@code 0}, if
	 *             {@code length} is greater than {@code this.readableBytes}, or
	 *             if {@code dstIndex + length} is greater than
	 *             {@code dst.length}
	 */
	void readBytes(byte[] dst, int dstIndex, int length);

	/**
	 * Transfers this buffer's data to the specified stream starting at the
	 * current {@code readerIndex}.
	 * 
	 * @param length
	 *            the number of bytes to transfer
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code length} is greater than {@code this.readableBytes}
	 * @throws IOException
	 *             if the specified stream threw an exception during I/O
	 */
	void readBytes(OutputStream out, int length) throws IOException;

	// ///////////////////////////////////Write/////////////////////////////////////////////

	/**
	 * Sets the specified byte at the current {@code writerIndex} and increases
	 * the {@code writerIndex} by {@code 1} in this buffer. The 24 high-order
	 * bits of the specified value are ignored.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.writableBytes} is less than {@code 1}
	 */
	void writeByte(int value);

//	void writeUnsignedByte(int value);
//	void writeUnsignedShort(int value);
//	void writeUnsignedInt(long value);
	
	/**
	 * Sets the specified 16-bit short integer at the current
	 * {@code writerIndex} and increases the {@code writerIndex} by {@code 2} in
	 * this buffer. The 16 high-order bits of the specified value are ignored.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.writableBytes} is less than {@code 2}
	 */
	void writeShort(int value);

	void writeBoolean(boolean value);

	/**
	 * Sets the specified 32-bit integer at the current {@code writerIndex} and
	 * increases the {@code writerIndex} by {@code 4} in this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.writableBytes} is less than {@code 4}
	 */
	void writeInt(int value);

	/**
	 * Sets the specified 64-bit long integer at the current {@code writerIndex}
	 * and increases the {@code writerIndex} by {@code 8} in this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.writableBytes} is less than {@code 8}
	 */
	void writeLong(long value);

	/**
	 * Sets the specified 2-byte UTF-16 character at the current
	 * {@code writerIndex} and increases the {@code writerIndex} by {@code 2} in
	 * this buffer. The 16 high-order bits of the specified value are ignored.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.writableBytes} is less than {@code 2}
	 */
	void writeChar(int value);

	/**
	 * Sets the specified 32-bit floating point number at the current
	 * {@code writerIndex} and increases the {@code writerIndex} by {@code 4} in
	 * this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.writableBytes} is less than {@code 4}
	 */
	void writeFloat(float value);

	/**
	 * Sets the specified 64-bit floating point number at the current
	 * {@code writerIndex} and increases the {@code writerIndex} by {@code 8} in
	 * this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code this.writableBytes} is less than {@code 8}
	 */
	void writeDouble(double value);

	/**
	 * Transfers the specified source array's data to this buffer starting at
	 * the current {@code writerIndex} and increases the {@code writerIndex} by
	 * the number of the transferred bytes (= {@code src.length}).
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if {@code src.length} is greater than
	 *             {@code this.writableBytes}
	 */
	void writeBytes(byte[] src);

	/**
	 * Transfers the specified source array's data to this buffer starting at
	 * the current {@code writerIndex} and increases the {@code writerIndex} by
	 * the number of the transferred bytes (= {@code length}).
	 * 
	 * @param srcIndex
	 *            the first index of the source
	 * @param length
	 *            the number of bytes to transfer
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code srcIndex} is less than {@code 0}, if
	 *             {@code srcIndex + length} is greater than {@code src.length},
	 *             or if {@code length} is greater than
	 *             {@code this.writableBytes}
	 */
	void writeBytes(byte[] src, int srcIndex, int length);

	// //////////////////////////////////////////////////////////////////////

	/**
	 * KGame自定义的一种读取字符串封装方法:<br>
	 * {@code int len=readInt();}<br>
	 * {@code byte[] bs=new byte[len];}<br>
	 * {@code readBytes(bs);}<br>
	 * 
	 * @return UTF-8编码的字符串
	 */
	String readUtf8String();

	/**
	 * KGame自定义的一种写入字符串封装方法:<br>
	 * {@code byte[] bs=utf8string.getBytes("UTF-8");}<br>
	 * {@code writeInt(len);}<br>
	 * {@code writeBytes(bs);}<br>
	 */
	void writeUtf8String(String utf8string);

	// ////////////////////////get & set////////////////////////////

	/**
	 * Gets a byte at the specified absolute {@code index} in this buffer. This
	 * method does not modify {@code readerIndex} or {@code writerIndex} of this
	 * buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 1} is greater than {@code this.capacity}
	 */
	byte getByte(int index);

	/**
	 * Gets an unsigned byte at the specified absolute {@code index} in this
	 * buffer. This method does not modify {@code readerIndex} or
	 * {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 1} is greater than {@code this.capacity}
	 */
	short getUnsignedByte(int index);

	/**
	 * Gets a 16-bit short integer at the specified absolute {@code index} in
	 * this buffer. This method does not modify {@code readerIndex} or
	 * {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 2} is greater than {@code this.capacity}
	 */
	short getShort(int index);

	/**
	 * Gets an unsigned 16-bit short integer at the specified absolute
	 * {@code index} in this buffer. This method does not modify
	 * {@code readerIndex} or {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 2} is greater than {@code this.capacity}
	 */
	int getUnsignedShort(int index);

	boolean getBoolean(int index);

	/**
	 * Gets a 32-bit integer at the specified absolute {@code index} in this
	 * buffer. This method does not modify {@code readerIndex} or
	 * {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 4} is greater than {@code this.capacity}
	 */
	int getInt(int index);

	/**
	 * Gets an unsigned 32-bit integer at the specified absolute {@code index}
	 * in this buffer. This method does not modify {@code readerIndex} or
	 * {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 4} is greater than {@code this.capacity}
	 */
	long getUnsignedInt(int index);

	/**
	 * Gets a 64-bit long integer at the specified absolute {@code index} in
	 * this buffer. This method does not modify {@code readerIndex} or
	 * {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 8} is greater than {@code this.capacity}
	 */
	long getLong(int index);

	/**
	 * Gets a 2-byte UTF-16 character at the specified absolute {@code index} in
	 * this buffer. This method does not modify {@code readerIndex} or
	 * {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 2} is greater than {@code this.capacity}
	 */
	char getChar(int index);

	/**
	 * Gets a 32-bit floating point number at the specified absolute
	 * {@code index} in this buffer. This method does not modify
	 * {@code readerIndex} or {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 4} is greater than {@code this.capacity}
	 */
	float getFloat(int index);

	/**
	 * Gets a 64-bit floating point number at the specified absolute
	 * {@code index} in this buffer. This method does not modify
	 * {@code readerIndex} or {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 8} is greater than {@code this.capacity}
	 */
	double getDouble(int index);

	/**
	 * Transfers this buffer's data to the specified destination starting at the
	 * specified absolute {@code index}. This method does not modify
	 * {@code readerIndex} or {@code writerIndex} of this buffer
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or if
	 *             {@code index + dst.length} is greater than
	 *             {@code this.capacity}
	 */
	void getBytes(int index, byte[] dst);

	/**
	 * Transfers this buffer's data to the specified destination starting at the
	 * specified absolute {@code index}. This method does not modify
	 * {@code readerIndex} or {@code writerIndex} of this buffer.
	 * 
	 * @param dstIndex
	 *            the first index of the destination
	 * @param length
	 *            the number of bytes to transfer
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0}, if the
	 *             specified {@code dstIndex} is less than {@code 0}, if
	 *             {@code index + length} is greater than {@code this.capacity},
	 *             or if {@code dstIndex + length} is greater than
	 *             {@code dst.length}
	 */
	void getBytes(int index, byte[] dst, int dstIndex, int length);

	/**
	 * Transfers this buffer's data to the specified stream starting at the
	 * specified absolute {@code index}. This method does not modify
	 * {@code readerIndex} or {@code writerIndex} of this buffer.
	 * 
	 * @param length
	 *            the number of bytes to transfer
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or if
	 *             {@code index + length} is greater than {@code this.capacity}
	 * @throws IOException
	 *             if the specified stream threw an exception during I/O
	 */
	void getBytes(int index, OutputStream out, int length) throws IOException;

	/**
	 * Sets the specified byte at the specified absolute {@code index} in this
	 * buffer. The 24 high-order bits of the specified value are ignored. This
	 * method does not modify {@code readerIndex} or {@code writerIndex} of this
	 * buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 1} is greater than {@code this.capacity}
	 */
	void setByte(int index, int value);

	/**
	 * Sets the specified 16-bit short integer at the specified absolute
	 * {@code index} in this buffer. The 16 high-order bits of the specified
	 * value are ignored. This method does not modify {@code readerIndex} or
	 * {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 2} is greater than {@code this.capacity}
	 */
	void setShort(int index, int value);

	/**
	 * Sets the specified 32-bit integer at the specified absolute {@code index}
	 * in this buffer. This method does not modify {@code readerIndex} or
	 * {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 4} is greater than {@code this.capacity}
	 */
	void setInt(int index, int value);

	/**
	 * Sets the specified 64-bit long integer at the specified absolute
	 * {@code index} in this buffer. This method does not modify
	 * {@code readerIndex} or {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 8} is greater than {@code this.capacity}
	 */
	void setLong(int index, long value);

	/**
	 * Sets the specified 2-byte UTF-16 character at the specified absolute
	 * {@code index} in this buffer. The 16 high-order bits of the specified
	 * value are ignored. This method does not modify {@code readerIndex} or
	 * {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 2} is greater than {@code this.capacity}
	 */
	void setChar(int index, int value);

	/**
	 * Sets the specified 32-bit floating-point number at the specified absolute
	 * {@code index} in this buffer. This method does not modify
	 * {@code readerIndex} or {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 4} is greater than {@code this.capacity}
	 */
	void setFloat(int index, float value);

	/**
	 * Sets the specified 64-bit floating-point number at the specified absolute
	 * {@code index} in this buffer. This method does not modify
	 * {@code readerIndex} or {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or
	 *             {@code index + 8} is greater than {@code this.capacity}
	 */
	void setDouble(int index, double value);

	/**
	 * Transfers the specified source array's data to this buffer starting at
	 * the specified absolute {@code index}. This method does not modify
	 * {@code readerIndex} or {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or if
	 *             {@code index + src.length} is greater than
	 *             {@code this.capacity}
	 */
	void setBytes(int index, byte[] src);

	/**
	 * Transfers the specified source array's data to this buffer starting at
	 * the specified absolute {@code index}. This method does not modify
	 * {@code readerIndex} or {@code writerIndex} of this buffer.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0}, if the
	 *             specified {@code srcIndex} is less than {@code 0}, if
	 *             {@code index + length} is greater than {@code this.capacity},
	 *             or if {@code srcIndex + length} is greater than
	 *             {@code src.length}
	 */
	void setBytes(int index, byte[] src, int srcIndex, int length);

	/**
	 * Transfers the content of the specified source stream to this buffer
	 * starting at the specified absolute {@code index}. This method does not
	 * modify {@code readerIndex} or {@code writerIndex} of this buffer.
	 * 
	 * @param length
	 *            the number of bytes to transfer
	 * 
	 * @return the actual number of bytes read in from the specified channel.
	 *         {@code -1} if the specified channel is closed.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is less than {@code 0} or if
	 *             {@code index + length} is greater than {@code this.capacity}
	 * @throws IOException
	 *             if the specified stream threw an exception during I/O
	 */
	int setBytes(int index, InputStream in, int length) throws IOException;

	/**
	 * Returns the {@code writerIndex} of this buffer.
	 */
	int writerIndex();

	/**
	 * Returns the {@code readerIndex} of this buffer.
	 */
	int readerIndex();

	/**
	 * Returns the number of readable bytes which is equal to
	 * {@code (this.writerIndex() - this.readerIndex())}.
	 */
	int readableBytes();
}
