package com.koala.game.tips;

/**
 * 响应码
 * 
 * @author AHONG
 * 
 */
public class RespCode {

	public int v;
	public String k;

	public void set(int v, String k) {
		this.v = v;
		this.k = k;
	}

	@Override
	public String toString() {
		return "RespCode[" + v + "," + k + "]";
	}
}
