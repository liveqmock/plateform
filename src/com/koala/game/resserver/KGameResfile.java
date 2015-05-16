package com.koala.game.resserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * 代表一个资源文件
 * @author AHONG
 * 
 */
class KGameResfile {

	int resid;// 资源ID
	String uri;// 资源文件位置
	String absolutePath;
	int revision;// 版本号

	File file;
	//byte[] cachedData;// 加载后缓存起来的数据

	@SuppressWarnings("unused")
	private KGameResfile() {
	}

	public KGameResfile(int resid) {
		this.resid = resid;
	}

	public KGameResfile(int resid, String uri, int revision) {
		this.resid = resid;
		this.uri = uri;
		this.revision = revision;
	}

	public void load() throws Exception {
		file = new File(uri);
		if (file == null || (!file.exists())) {
			throw new FileNotFoundException("File not found : " + uri);
		}
//		if ((cachedData = readFileToByteArray(file)) != null) {
//		}
	}

	public long lastModified() {
		return file != null ? file.lastModified() : -1;
	}

	public long fileSize() {
		return file != null ? file.length() : 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(KGameResfile.class.getSimpleName());
		sb.append("[");
		sb.append(resid).append(" ");// 资源ID
		sb.append(uri).append(" ");// 资源文件位置
		sb.append(absolutePath).append(" ");
		sb.append(revision).append(" ");// 版本号
		sb.append(file!=null?file.length():-1);// size
		sb.append("]");
		return sb.toString();
	}

	private static final int EOF = -1;

	public static byte[] readFileToByteArray(File file) throws IOException {
		int size = (int) file.length();
		byte[] data = new byte[size];
		InputStream input = new FileInputStream(file);
		try {
			int offset = 0;
			int readed;

			while (offset < size
					&& (readed = input.read(data, offset, size - offset)) != EOF) {
				offset += readed;
			}

			if (offset != size) {
				throw new IOException("Unexpected readed size. current: "
						+ offset + ", excepted: " + size);
			}
		} finally {
			input.close();
		}
		return data;
	}
}
