package com.koala.promosupport.kola;

import java.util.Map;

import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayOrder;

public class KolaPayCallback implements IPayCallback{

	private KolaChannel ch;
	
	
	public KolaPayCallback(KolaChannel ch) {
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String parse(String data) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PayOrder getGeneratedPayOrder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String responseOfRepeatCallback() {
		return "success";
	}
}
