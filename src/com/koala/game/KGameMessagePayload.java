package com.koala.game;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 本接口可以把一个按约定格式写入的byte[]转换成可读取的对象
 * 
 * @author AHONG
 * 
 */
public interface KGameMessagePayload {
	
	public final static byte FALSE = 0;
	public final static byte TRUE = 1;

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

	/**
	 * KGame自定义的一种读取字符串封装方法:<br>
	 * {@code int len=readInt();}<br>
	 * {@code byte[] bs=new byte[len];}<br>
	 * {@code readBytes(bs);}<br>
	 * 
	 * @return UTF-8编码的字符串
	 */
	String readUtf8String();

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
	 * Returns the {@code readerIndex} of this buffer.
	 */
	int readerIndex();

	/**
	 * Returns the number of readable bytes which is equal to
	 * {@code (this.writerIndex() - this.readerIndex())}.
	 */
	int readableBytes();
}
