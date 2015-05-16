package com.koala.game.communication;

import java.io.IOException;
import java.io.OutputStream;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.util.CharsetUtil;

import com.koala.game.KGameMessagePayload;

public class KGameMessagePayloadImpl implements KGameMessagePayload {

	private ChannelBuffer backingChannelBuffer;

	public KGameMessagePayloadImpl(byte[] payloadBytes, int offset, int length) {
		backingChannelBuffer = ChannelBuffers.wrappedBuffer(payloadBytes,
				offset, length);
	}

	public void skipBytes(int length) {
		backingChannelBuffer.skipBytes(length);
	}

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
	public boolean readBoolean() {
		return backingChannelBuffer.readByte() == FALSE ? false : true;
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

	@Override
	public boolean getBoolean(int index) {
		return backingChannelBuffer.getByte(index) == FALSE ? false : true;
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
