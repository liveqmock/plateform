package com.koala.verserver;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jdom.Document;
import org.jdom.Element;

import com.koala.game.logging.KGameLogger;
import com.koala.game.util.XmlUtil;

/**
 * <pre>
 * 	Android推送协议：
 * 
 * 	1、客户端定期向服务器请求
 * 	http://ver.kl321.com:30000/push?p=0&pid=1001&product=txb&roleid=123123&sign=aejf32423jdsijfafaj
 * 
 * 	p:平台号，0：Android，1：ios
 * 	pid:渠道号
 * 	product:产品名称，例如：txb
 * 	roleid:角色ID
 * 	sign:MD5加密字符串（和更新一样的加密策略：&key=kola）
 * </pre>
 * 
 * 对应配置文件 config/fe/Push.xml
 * 
 * @author AHONG
 * 
 */
public class KGamePush {

	class PushResult {
		int promoid;
		boolean open;
		// ///////////////////////
		String title;
		String content;
		String interval;
		String newpushurl;

		@Override
		public String toString() {
			return "PushResult [promoid=" + promoid + ", open=" + open
					+ ", title=" + title + ", content=" + content
					+ ", interval=" + interval + ", newpushurl=" + newpushurl
					+ "]";
		}
	}

	private final static KGameLogger logger = KGameLogger
			.getLogger(KGamePush.class);
	private final static File xmlFile = new File("res/config/fe/Push.xml");
	private final Map<Integer, KGamePush.PushResult> pushResults = new HashMap<Integer, KGamePush.PushResult>();
	private final static Integer DEFAULT_PID = 0;

	public KGamePush() {
		loadPushXml();
	}

	private void loadPushXml() {
		Document doc = XmlUtil.openXml(xmlFile);
		Element root = doc.getRootElement();

		List<Element> ePushResult = root.getChildren("PushResult");
		for (Element element : ePushResult) {
			PushResult pr = new PushResult();

			pr.title = element.getChildTextTrim("title");
			pr.content = element.getChildTextTrim("tips");
			pr.interval = element.getChildTextTrim("Interval");
			pr.newpushurl = element.getChildTextTrim("newURL");

			// /////////////////////////////////
			pr.open = Boolean.parseBoolean(element.getAttributeValue("open"));
			String[] pids = element.getAttributeValue("promoid").split(",");
			for (String pid : pids) {
				if (pid != null && pid.length() > 0) {
					if ("default".equalsIgnoreCase(pid)) {
						pr.promoid = DEFAULT_PID;
					} else {
						pr.promoid = Integer.parseInt(pid);
					}
					pushResults.put(pr.promoid, pr);
					logger.debug("LOAD PushResult : {}", pr);
				}
			}
		}
	}

	boolean messageReceived(ChannelHandlerContext ctx, HttpRequest request,
			HttpResponse response) throws Exception {

		URI uri = new URI(request.getUri());
		String path = uri.getRawPath();
		logger.debug("REQUEST PATH = ", path);

		if (!path.contains(KGameVerControlServerHandler.REQUEST_PATH_PUSH)) {
			return false;
		}

		Document doc = new Document();
		Element root = new Element("PushResult");
		doc.setRootElement(root);
		try {
			Map<String, String> params = KGameVerControlServerHandler
					.readContentAsParams(request);
			// http://ver.kl321.com:30000/push?p=0&pid=1001&product=txb&name=xxx&sign=aejf32423jdsijfafaj
			String platform = params
					.get(KGameVerControlServerHandler.PKEY_PLATFORM);
			String promoid = params
					.get(KGameVerControlServerHandler.PKEY_PROMOID);
			String product = params.get("product");
			String roleid = params.get("roleid");
			String sign = params.get(KGameVerControlServerHandler.PKEY_SIGN);

			StringBuilder buf = new StringBuilder();
			buf.append(KGameVerControlServerHandler.PKEY_PLATFORM).append("=")
					.append(platform).append("&")
					.append(KGameVerControlServerHandler.PKEY_PROMOID)
					.append("=").append(promoid).append("&").append("product")
					.append("=").append(product).append("&").append("roleid")
					.append("=").append(roleid).append("&key=")
					.append(KGameVerControlServerHandler.SIGN_KEY);
			// if (!MD5.MD5Encode(buf.toString()).equals(sign)) {
			// sendError(ctx, request, response, doc, PL_VERCHECK_SIGN_ERROR,
			// KGameTips.get("PL_VERCHECK_PARAM_ERROR"));
			// return;
			// }
			PushResult push = pushResults.get(promoid);
			if(push==null){
				push = pushResults.get(DEFAULT_PID);
			}
			if (push != null) {
				writepushresulttoresponsecontent(push, root);
				KGameVerControlServerHandler.sendHttpResponse(ctx, request,
						response, doc);
			} else {
				sendemptypush(ctx, request, response, doc);
			}

		} catch (Exception e) {
			sendemptypush(ctx, request, response, doc);
		}
		return true;
	}

	private static void sendemptypush(ChannelHandlerContext ctx,
			HttpRequest request, HttpResponse response, Document doc)
			throws Exception {
		doc.getRootElement().addContent(new Element("title").addContent(""));
		doc.getRootElement().addContent(new Element("tips").addContent(""));
		KGameVerControlServerHandler.sendHttpResponse(ctx, request, response,
				doc);
	}

	private static void writepushresulttoresponsecontent(PushResult push,
			Element root) {
		root.addContent(new Element("title").addContent(push.title));
		root.addContent(new Element("tips").addContent(push.content));
		root.addContent(new Element("Interval").addContent(push.interval));
		root.addContent(new Element("newURL").addContent(push.newpushurl));
	}

	 public static void main(String[] args) {
	 String[] pids = ",default".split(",");
	 System.out.println(pids.length);
	 for (String pid : pids) {
	 System.out.println(pid);
	 }
	 }
}
