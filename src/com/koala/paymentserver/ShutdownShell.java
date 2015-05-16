package com.koala.paymentserver;

import com.koala.promosupport.DoHttpRequest;

public final class ShutdownShell {

	public static void main(String[] args) {
		String url = args[0];
		String code = args[1];
		DoHttpRequest http = new DoHttpRequest();
		String response = http.request(url, "code=" + code, "GET");
		System.err.println(response);
	}
}
