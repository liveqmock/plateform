package com.koala.game.communication;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.koala.game.util.StringUtil;
import com.koala.promosupport.MD5;
import com.koala.thirdpart.json.JSONArray;
import com.koala.thirdpart.json.JSONObject;

public class KGameHttpRequestSender {

	private static final char _GET_ADDRESS_SEPARATOR = '?';
	private static final char _GET_PARA_SEPARATOR = '&';
	
	private static final String _paraFormat = "{}={}";
	
	private static final int _TIME_OUT = (int)TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);
	
	private ExecutorService _service;
	
	public KGameHttpRequestSender() {
		_service = Executors.newFixedThreadPool(8);
	}
	
	public void shutdown() {
		_service.shutdownNow();
	}
	
	/**
	 * 
	 * @param urlAddress
	 * @param paraMap
	 * @param charSetName
	 * @throws Exception
	 */
	public Future<KGameHttpRequestResult> sendGetRequest(String urlAddress, Map<String, Object> paraMap, List<String> needEncodeKeys, String charSetName) throws Exception {
		KGameHttpInvoker invoker = new KGameHttpInvoker(urlAddress, paraMap, needEncodeKeys, charSetName, KGameHttpInvoker.METHOD_GET);
		return _service.submit(invoker);
	}
	
	/**
	 * 
	 * @param urlAddress
	 * @param paraMap
	 * @throws Exception
	 */
	public Future<KGameHttpRequestResult> sendPostRequest(String urlAddress, Map<String, Object> paraMap, List<String> needEncodeKeys, String charSetName) throws Exception {
		KGameHttpInvoker invoker = new KGameHttpInvoker(urlAddress, paraMap, needEncodeKeys, charSetName, KGameHttpInvoker.METHOD_POST);
		return _service.submit(invoker);
	}
	
	/**
	 * 
	 * @param urlAddress
	 * @param paraMap
	 * @throws Exception
	 */
	public Future<KGameHttpRequestResult> sendPostRequestUseJSON(String urlAddress, Map<String, Object> paraMap, List<String> needEncodeKeys, String charSetName) throws Exception {
		KGameHttpInvoker invoker = new KGameHttpInvoker(urlAddress, paraMap, needEncodeKeys, charSetName, KGameHttpInvoker.METHOD_POST);
		invoker.setUseJSON(true);
		return _service.submit(invoker);
	}
	
	private static class KGameHttpInvoker implements Callable<KGameHttpRequestResult> {

		static final byte METHOD_GET = 1;
		static final byte METHOD_POST = 2;
		
		private String _urlAddress;
		private Map<String, Object> _paraMap;
		private List<String> _needEncodeKeys;
		private String _charsetName;
		private byte _methodType;
		private boolean _useJSON;
		
		public KGameHttpInvoker(String pUrlAddress, Map<String, Object> pParaMap, List<String> pNeedEncodeKeys, String pCharSetName, byte pMethodType) {
			this._urlAddress = pUrlAddress;
			this._paraMap = new HashMap<String, Object>(pParaMap);
			this._charsetName = pCharSetName;
			this._methodType = pMethodType;
			if (pNeedEncodeKeys != null) {
				this._needEncodeKeys = new ArrayList<String>(pNeedEncodeKeys);
			} else {
				this._needEncodeKeys = Collections.emptyList();
			}
		}
		
		@Override
		public KGameHttpRequestResult call() throws Exception {
			switch(this._methodType) {
			case METHOD_GET:
				return sendGetRequest();
			case METHOD_POST:
				return sendPostRequest();
			}
			return null;
		}
		
		public void setUseJSON(boolean pUseJSON) {
			this._useJSON = pUseJSON;
		}
		
		private KGameHttpRequestResult sendGetRequest() throws Exception {
			StringBuilder urlBuilder = new StringBuilder(_urlAddress);
			if (urlBuilder.charAt(_urlAddress.length() - 1) != _GET_ADDRESS_SEPARATOR) {
				urlBuilder.append(_GET_ADDRESS_SEPARATOR);
			}
			Map.Entry<String, Object> entry;
			Object value;
			for (Iterator<Map.Entry<String, Object>> itr = _paraMap.entrySet().iterator(); itr.hasNext();) {
				entry = itr.next();
				value = entry.getValue();
				if (_needEncodeKeys.contains(entry.getKey())) {
					value = URLEncoder.encode(value.toString(), _charsetName);
				}
				urlBuilder.append(entry.getKey()).append("=").append(value);
				if (itr.hasNext()) {
					urlBuilder.append(_GET_PARA_SEPARATOR);
				}
			}

			URL getUrl = new URL(urlBuilder.toString());
			HttpURLConnection conn = (HttpURLConnection) getUrl.openConnection();
			conn.setDoInput(true);
			conn.connect();
			KGameHttpRequestResult result = processResponse(conn);
			conn.disconnect();
			return result;
		}
		
		private KGameHttpRequestResult sendPostRequest() throws Exception {
			URL getUrl = new URL(_urlAddress);
			HttpURLConnection conn = (HttpURLConnection)getUrl.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setInstanceFollowRedirects(true);
			conn.setConnectTimeout(_TIME_OUT);
			conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
			DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
			if (_useJSON) {
				JSONObject obj = new JSONObject();
				Map.Entry<String, Object> entry;
				Object value;
				for (Iterator<Map.Entry<String, Object>> itr = _paraMap.entrySet().iterator(); itr.hasNext();) {
					entry = itr.next();
					value = entry.getValue();
					if (_needEncodeKeys.contains(entry.getKey())) {
						value = URLEncoder.encode(value.toString(), _charsetName);
					}
					obj.put(entry.getKey(), value);
				}
				dos.writeBytes(obj.toString());
			} else {
				Map.Entry<String, Object> entry;
				Object value;
				StringBuilder strBld = new StringBuilder();
				for (Iterator<Map.Entry<String, Object>> itr = _paraMap.entrySet().iterator(); itr.hasNext();) {
					entry = itr.next();
					value = entry.getValue();
					if (_needEncodeKeys.contains(entry.getKey())) {
						value = URLEncoder.encode(value.toString(), _charsetName);
					}
//					dos.writeBytes(StringUtil.format(_paraFormat, entry.getKey(), value));
					strBld.append(StringUtil.format(_paraFormat, entry.getKey(), value));
					if(itr.hasNext()) {
						strBld.append("&");
					}
				}
				dos.writeBytes(strBld.toString());
			}
			dos.flush();
			dos.close();
			KGameHttpRequestResult result = processResponse(conn);
			conn.disconnect();
			return result;
		}
		
		private KGameHttpRequestResult processResponse(HttpURLConnection conn) throws Exception {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
				int respCode = conn.getResponseCode();
				StringBuilder content = new StringBuilder();
				String temp;
				while ((temp = br.readLine()) != null) {
					content.append(temp);
				}
				return new KGameHttpRequestResult(respCode, content.toString());
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
		}
		
	}
	
	public static class KGameHttpRequestResult {
		public final int respCode;
		public final String content;

		KGameHttpRequestResult(int pRespCode, String pContent) {
			this.respCode = pRespCode;
			this.content = pContent;
		}
	}
	
	public static void main(String[] args) throws Exception {
//		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//		KGameHttpRequestSender sender = new KGameHttpRequestSender();
//		Map<String, Object> paraMap = new HashMap<String, Object>();
//		//key，giftType,userId ,giftCode 其中 一个不同 就行了
//		paraMap.put("userId", (System.currentTimeMillis()+"").substring(5));
//		paraMap.put("giftType", "F");
//		paraMap.put("giftCode", "00544");
//		//
//		paraMap.put("key", "0d6f9fc43f4952879357dc06a3f1c5d5");
//		paraMap.put("uuid", "f8:a4:5f8c:0d:d9");
//		paraMap.put("userAccount", "kolaapp");
//		paraMap.put("serverName", "[1服]英雄降临");
//		paraMap.put("role", "矮需要勇气");
//		paraMap.put("createUserTime", "20141218100000");
//		paraMap.put("useGiftTime", formatter.format(new Date()));
//		//
//		List<String> needEncodeKeys = new ArrayList<String>();
//		needEncodeKeys.add("serverName");
//		needEncodeKeys.add("role");
//		Future<KGameHttpRequestResult> result = sender.sendPostRequestUseJSON("http://data.xxwan.com/gift", paraMap, needEncodeKeys, "UTF-8");
//		KGameHttpRequestResult respResult = result.get();
//		System.out.println(StringUtil.format("====content:{}====", respResult.content));
//		sender.shutdown();
		
		String activationCode = args[0];
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String json;
		String code;
		try {
			JSONObject josnO = new JSONObject();
			josnO.put("passport", "my_good_game");
			josnO.put("timestamp", sdf.format(new Date(System.currentTimeMillis())));
			josnO.put("tid", 1118);
			
			JSONArray jsonA = new JSONArray();
			jsonA.put(activationCode);
			
			josnO.put("data", jsonA);
			//
			json = josnO.toString();
			code = MD5.MD5Encode( json + ";" + "4@#aX9Has*72");
		} catch(Exception e){
			e.printStackTrace();
			return;
		}
		KGameHttpRequestSender sender = new KGameHttpRequestSender();
		try {
			Map<String, Object> paraMap = new HashMap<String, Object>();
			paraMap.put("code", code);
			paraMap.put("json", json);
			List<String> list = Arrays.asList("json");
			Future<KGameHttpRequestResult> result = sender.sendPostRequest("http://task.g.yy.com/task/daily/complete.do", paraMap, list, "UTF-8");
			System.out.println(StringUtil.format("====content:{}====", result.get().content));
		} catch (Exception e) {
			e.printStackTrace();
		}
		sender.shutdown();
	}
}
