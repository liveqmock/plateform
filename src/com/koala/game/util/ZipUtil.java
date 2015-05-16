package com.koala.game.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

	/**
	 * Answer a byte array compressed in the Zip format from bytes.
	 * 
	 * @param bytes
	 *            a byte array
	 * @param aName
	 *            a String the represents a file name
	 * @return byte[] compressed bytes
	 * @throws IOException
	 */
	public static byte[] zipBytes(byte[] bytes) throws IOException {
		ByteArrayOutputStream tempOStream = null;
		BufferedOutputStream tempBOStream = null;
		ZipOutputStream tempZStream = null;
		ZipEntry tempEntry = null;
		byte[] tempBytes = null;

		tempOStream = new ByteArrayOutputStream(bytes.length);
		tempBOStream = new BufferedOutputStream(tempOStream);
		tempZStream = new ZipOutputStream(tempBOStream);
		tempEntry = new ZipEntry(String.valueOf(bytes.length));
		tempEntry.setMethod(ZipEntry.DEFLATED);
		tempEntry.setSize((long) bytes.length);

		tempZStream.putNextEntry(tempEntry);
		tempZStream.write(bytes, 0, bytes.length);
		tempZStream.flush();
		tempBOStream.flush();
		tempOStream.flush();
		tempZStream.close();
		tempBytes = tempOStream.toByteArray();
		tempOStream.close();
		tempBOStream.close();
		return tempBytes;
	}

	/**
	 * Answer a byte array that has been decompressed from the Zip format.
	 * 
	 * @param bytes
	 *            a byte array of compressed bytes
	 * @param uncompressedlength
	 *            原始长度，如果不知道可以填-1
	 * @return byte[] uncompressed bytes
	 * @throws IOException
	 */
	public static byte[] unzipBytes(byte[] bytes, int uncompressedlength)
			throws IOException {
		ByteArrayInputStream tempIStream = null;
		BufferedInputStream tempBIStream = null;
		ZipInputStream tempZIStream = null;
		ZipEntry tempEntry = null;
		long tempDecompressedSize = -1;
		byte[] tempUncompressedBuf = null;
		ByteArrayOutputStream os = uncompressedlength > 0 ? new ByteArrayOutputStream(
				uncompressedlength) : new ByteArrayOutputStream();

		tempIStream = new ByteArrayInputStream(bytes, 0, bytes.length);
		tempBIStream = new BufferedInputStream(tempIStream);
		tempZIStream = new ZipInputStream(tempBIStream);
		tempEntry = tempZIStream.getNextEntry();

		if (tempEntry != null) {
			tempDecompressedSize = tempEntry.getCompressedSize();
			if (tempDecompressedSize < 0) {
				tempDecompressedSize = Long.parseLong(tempEntry.getName());
			}

			int size = (int) tempDecompressedSize;
			tempUncompressedBuf = new byte[size];
			int num = 0, count = 0;
			while (true) {
				count = tempZIStream.read(tempUncompressedBuf, 0, size - num);
				num += count;
				os.write(tempUncompressedBuf, 0, count);
				os.flush();
				if (num >= size)
					break;
			}
		}
		tempZIStream.close();
		return os.toByteArray();
	}

	//=================================================================================
	
	/**
	 * 
	 * 利用zip算法压缩
	 * 
	 * @param src
	 * @return
	 */
	public static byte[] compress(byte[] src) {
		byte[] array;
		Deflater compresser = new Deflater();
		compresser.reset();
		compresser.setInput(src);
		compresser.finish();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			byte[] buf = new byte[1024];
			int readLength = 0;
			while ((readLength = compresser.deflate(buf)) > 0) {
				out.write(buf, 0, readLength);
			}
			array = out.toByteArray();
		} catch (Exception e) {
			array = src;
			System.err.println("压缩出现异常！"+ e);
		} finally {
			try {
				out.close();
			} catch (Exception e) {

			}
		}
		// compresser.end();
		return array;
	}
	
	/**
	 * 
	 * 使用zip算法对src进行解压缩
	 * 
	 * @param src
	 * @return
	 */
	public static byte[] decompress(byte[] src) {
		byte[] output;
		Inflater decompresser = new Inflater();
		decompresser.reset();
		decompresser.setInput(src);

		ByteArrayOutputStream out = new ByteArrayOutputStream(src.length);
		try {
			byte[] buf = new byte[1024];
			int readLength = 0;
			while (!decompresser.finished()) {
				readLength = decompresser.inflate(buf);
				out.write(buf, 0, readLength);
			}
			output = out.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			output = src;
			System.err.println("解压缩出现异常！");
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				// We are safe here! ByteArrayOutStream never throw this
				// exception
			}
		}

		decompresser.end();
		return output;
	}
}
