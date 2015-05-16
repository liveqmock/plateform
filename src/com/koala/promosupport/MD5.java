package com.koala.promosupport;

import java.net.URLDecoder;
import java.security.MessageDigest;

public class MD5 {

	private final static String[] hexDigits = { "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

	public static String byteArrayToHexString(byte[] b) {
		StringBuffer resultSb = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			resultSb.append(byteToHexString(b[i]));
		}
		return resultSb.toString();
	}

	private static String byteToHexString(byte b) {
		int n = b;
		if (n < 0) {
			n = 256 + n;
		}
		int d1 = n / 16;
		int d2 = n % 16;
		return hexDigits[d1] + hexDigits[d2];
	}

	public static String MD5Encode(String origin) {
		String resultString = null;
		try {
			resultString = new String(origin);
			MessageDigest md = MessageDigest.getInstance("MD5");
			resultString = byteArrayToHexString(md.digest(resultString
					.getBytes("ISO-8859-1")));
		} catch (Exception ex) {
		}
		return resultString;
	}
	
	public static String MD5Encode(String origin,String chs) {
		String resultString = null;
		try {
			resultString = new String(origin);
			MessageDigest md = MessageDigest.getInstance("MD5");
			resultString = byteArrayToHexString(md.digest(resultString
					.getBytes(chs)));
		} catch (Exception ex) {
		}
		return resultString;
	}

	public static void main(String[] args) throws Exception {
		// String
		// data="mid=237&gid=2&sid=1&uif=2.249163&cardno=3pvG8fU3/TUwqQMo5Vqmcg==&cardpwd=3pvG8fU3/TXra8UXGkYiR/6CQ6d6/raA&utp=0&uip=220.231.155.194&eif=Y5060162242&amount=10&timestamp=20110110180711&verstring=Vx&@113*";
		//String data = "orderId=P1008R10T20130726124632520N24&skyId=365007926&resultCode=0&payNum=31307261249140108799&cardType=13&realAmount=1000&payTime=20130726124911&failure=0&ext1=p1008g1r10&key=jmyolioyk$@#97l9";
		//System.out.println(MD5Encode(data));
		//19196e16446671346c08b8b1b9979dd2
		//19196E16446671346C08B8B1B9979DD2
		System.out.println(URLDecoder.decode("%7B%22status%22%3A1%2C%22info%22%3A%22sign%E4%B8%8D%E8%83%BD%E4%B8%BA%E7%A9%BA%22%2C%22userinfo%22%3A%7B%7D%7D", "UTF-8"));
	}
}