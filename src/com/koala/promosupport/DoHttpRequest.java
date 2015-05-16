package com.koala.promosupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.koala.game.logging.KGameLogger;

public class DoHttpRequest {

	private static final KGameLogger logger = KGameLogger
			.getLogger(DoHttpRequest.class);

	public static void main(String[] args) {
		// Map<String,String> params = new HashMap<String,String>();
		// params.put("lpsust",
		// "ZAgAAAAAAAGE9MTAwMDM1NTA4MDMmYj0yJmM9NCZkPTExJmU9RTZGM0EzMTY5RjAwQTM2QzE4MzNERDM4QzhCQkU0QzkxJmg9MTM3MjkxMDg2NDI3NSZpPTQzMjAwJm89MDAwMDAwMDAwMDAwMDAwJnA9aW1laSZxPTExMTExMSZ1c2VybmFtZT0xMzgxMDUzNTg4N6z979s5fL06DibrT5d7D6s=");
		// params.put("realm", "appstore.lps.lenovo.com");
		// String r0 =
		// HttpUtils.sendGet("http://passport.lenovo.com/interserver/authen/1.2/getaccountid",
		// params);
		// System.out.println(r0);

		DoHttpRequest h = new DoHttpRequest();
		String r = h
				.request(
						"http://passport.lenovo.com/interserver/authen/1.2/getaccountid",
						"lpsust=ZAgAAAAAAAGE9MTAwMDM1NT&realm=appstore.lps.lenovo.com",
						"GET");
		System.out.println(r);
	}

	public DoHttpRequest() {
	}
	
	/**
	 * 
	 * @param url
	 * @param params
	 * @param method
	 * @return
	 */
	public String request(String url, String params, String method) {
		return request(url, params, method, null);
	}

	/**
	 * 
	 * @param url
	 * @param params
	 * @param method
	 * @return
	 */
	public String request(String url, String params, String method,Map<String,String> headers) {
		try {
			if (!url.toLowerCase().startsWith("http")) {
				url = "http://" + url;
			}

			if ("GET".equalsIgnoreCase(method)) {
				if (url.contains("?")) {
					if (url.endsWith("?")||url.endsWith("=")) {
						url = (new StringBuilder(url).append(params))
								.toString();
					} else {
						url = (new StringBuilder(url).append("&")
								.append(params)).toString();
					}
				} else {
					url = (new StringBuilder(url).append("?").append(params))
							.toString();
				}
			}
			URL u = new URL(url);
			HttpURLConnection conn = null;

			boolean bSSL = url.startsWith("https:");
			if (bSSL) {
				// 创建SSLContext对象，并使用我们指定的信任管理器初始化
				TrustManager[] tm = { new MyX509TrustManager() };
				SSLContext sslContext = SSLContext
						.getInstance("SSL", "SunJSSE");
				sslContext.init(null, tm, new java.security.SecureRandom());
				// 从上述SSLContext对象中得到SSLSocketFactory对象
				SSLSocketFactory ssf = sslContext.getSocketFactory();
				conn = (HttpsURLConnection) u.openConnection();
				((HttpsURLConnection) conn).setSSLSocketFactory(ssf);
			} else {
				conn = (HttpURLConnection) u.openConnection();
			}

			conn.setRequestMethod(method);
			conn.setReadTimeout(30000);// 30s
			conn.setDoInput(true);
			conn.setDoOutput(true);

			//20131106增加部分特殊渠道需要增加Header的需求
			if (headers != null && headers.size() > 0) {
				for (String hk : headers.keySet()) {
					String hv = headers.get(hk);
					if (hv != null && hv.length() > 0) {
						conn.setRequestProperty(hk, hv);
					}
				}
			}
			
			logger.info("HTTP REQUEST>> {}", url);

			// WRITE
			if ("POST".equalsIgnoreCase(method)) {
				OutputStream out = conn.getOutputStream();
				out.write(params.getBytes("UTF-8"));
				out.flush();// ?
				out.close();
			}

			int code = conn.getResponseCode();
//			logger.info("ResponseCode : {}", code);

			// READ
			if (code == HttpURLConnection.HTTP_OK) {
				// int contentLength = conn.getContentLength();
				String en = conn.getContentEncoding();
				String encoding = (en != null && en.length() > 0) ? en
						: "utf-8";
//				logger.info("Content-Type,ContentEncoding,Content-Length {},{},{}",
//						conn.getContentType(), en, conn.getContentLength());
				// logger.info("Content-Length : {}", conn.getContentLength());
				// InputStream in = conn.getInputStream();
				BufferedReader in = new BufferedReader(new InputStreamReader(
						conn.getInputStream(), encoding));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					sb.append(line.trim());
				}
				// contentLength = contentLength < 0 ? 2048 : contentLength;
				// byte[] buf = new byte[contentLength];
				// int r = in.read(buf);
				// logger.info("Readed : {}", r);
				// if (r > 0) {
				// response = new String(buf, 0, r, "UTF-8");
				// }
				in.close();

				logger.info("HTTP RESPONSE>> {}", sb.toString());
				return sb.toString();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	class MyX509TrustManager implements X509TrustManager {
		/*
		 * The default X509TrustManager returned by SunX509. We'll delegate
		 * decisions to it, and fall back to the logic in this class if the
		 * default X509TrustManager doesn't trust it.
		 */
		// X509TrustManager sunJSSEX509TrustManager;

		MyX509TrustManager() throws Exception {
			// // create a "default" JSSE X509TrustManager.
			// KeyStore ks = KeyStore.getInstance("JKS");
			// ks.load(new FileInputStream("trustedCerts"),
			// "passphrase".toCharArray());
			// TrustManagerFactory tmf = TrustManagerFactory.getInstance(
			// "SunX509", "SunJSSE");
			// tmf.init(ks);
			// TrustManager tms[] = tmf.getTrustManagers();
			// /*
			// * Iterate over the returned trustmanagers, look for an instance
			// of
			// * X509TrustManager. If found, use that as our "default" trust
			// * manager.
			// */
			// for (int i = 0; i < tms.length; i++) {
			// if (tms[i] instanceof X509TrustManager) {
			// sunJSSEX509TrustManager = (X509TrustManager) tms[i];
			// return;
			// }
			// }
			// /*
			// * Find some other way to initialize, or else we have to fail the
			// * constructor.
			// */
			// throw new Exception("Couldn't initialize");
		}

		/*
		 * Delegate to the default trust manager.
		 */
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			// try {
			// sunJSSEX509TrustManager.checkClientTrusted(chain, authType);
			// } catch (CertificateException excep) {
			// // do any special handling here, or rethrow exception.
			// }
		}

		/*
		 * Delegate to the default trust manager.
		 */
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			// try {
			// sunJSSEX509TrustManager.checkServerTrusted(chain, authType);
			// } catch (CertificateException excep) {
			// /*
			// * Possibly pop up a dialog box asking whether to trust the cert
			// * chain.
			// */
			// }
		}

		/*
		 * Merely pass this through.
		 */
		public X509Certificate[] getAcceptedIssuers() {
			// return sunJSSEX509TrustManager.getAcceptedIssuers();
			return null;
		}
	}
}
