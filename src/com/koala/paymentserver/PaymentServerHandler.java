package com.koala.paymentserver;

import static org.jboss.netty.handler.codec.http.HttpHeaders.getHost;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.handler.codec.http.multipart.Attribute;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import org.jboss.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.jboss.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import org.jboss.netty.util.CharsetUtil;

import com.koala.game.logging.KGameLogger;
import com.koala.promosupport.PromoChannel;
import com.koala.promosupport.PromoSupport;

public class PaymentServerHandler extends SimpleChannelHandler {

	public static final KGameLogger logger = KGameLogger
			.getLogger(PaymentServerHandler.class);

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		HttpRequest request = (HttpRequest) e.getMessage();
		System.out.println("-----------------------------------" + request);
		Channel channel = ctx.getChannel();

		// _printHttpInfo(request);
		
		String responsecontent;
		
		// 关闭服务器的指令 /sd?y=md5("yesjustdoit")
		if ((responsecontent = PaymentServer.getIntance().shutdowncmdcheck(
				request.getUri())) != null) {
			doResponse(channel, responsecontent);
			PaymentServer.getIntance().shutdown();
			return;
		}
		

		// try {

		int promoID = getPromoIDbyUriPath(request);
		logger.debug("promoID={}", promoID);
		if (promoID == -1) {
			// 不合法的请求
			logger.error("Illega PromoID {}", promoID);
			doResponse(channel, "Illega PromoID " + promoID);
			return;
		}

		// 找到对应的渠道
		PromoChannel pchannel = PromoSupport.getInstance().getPromoChannel(
				promoID);
		if (pchannel == null) {
			// 不合法的请求
			logger.error("Not Supported PromoChannel. {}", promoID);
			doResponse(channel, "Not Supported PromoChannel. "+ promoID);
			return;
		}

		// //////////////////////////////////////////////////////////
		// 根据不同渠道生成一个IPayCallback对象
		IPayCallback callback = pchannel.newPayCallback();

//		logger.info("request.uri------ {}", request.getUri());
		
		// /////////////////////////////////////////////////////////
		// 根据不同的HTTP方法读取数据，并将数据进行分析处理
		switch (pchannel.getPayCallbackMethod()) {
		case GET_STRING:
		case POST:
			responsecontent = callback.parse(readContentAsString(request));
			break;
		case GET:
		case GET_POST:
		default:
			responsecontent = callback.parse(readContentAsParams(request));
			break;
		}

		// /////////////////////////////////////////////////////////
		// 处理生成的订单（有可能订单为null）
		PayOrder payOrder = callback.getGeneratedPayOrder();
		if (payOrder != null) {
			// 通知GS发货
			PS2GS p2g = PaymentServer.getIntance().getPS2GS(
					payOrder.getExt().getGsID());
			if (p2g != null) {
				// XXX 这里有两种将订单通知GS发货的方式：(目前是用第二种，在PS2GS中做队列)
				// 第一种是入Q然后有线程不断的看有没有订单需要发给GS；
				// 第二种是直接在本线程把订单发给GS；
				boolean accepted = p2g.processPayOrder(payOrder);
				if(!accepted){
					responsecontent = callback.responseOfRepeatCallback();
				}
			} else {
				logger.warn("【{}充值】回调找不到PS2GS(gsid:{}),{}",
						pchannel.getPromoID(), payOrder.getExt().getGsID(),
						callback);
			}
		} else {
			// 如果订单为null，记录callback的数据以便手工跟踪，到时为玩家补充
			logger.warn("【{}充值】生成订单为null. {}", pchannel.getPromoID(), callback);
		}
		
		if(responsecontent==null){
			responsecontent="";
		}

		// } finally {
		// 对SDK的通知作出HTTP响应
		doResponse(channel, responsecontent);
		// }
	}

	/**
	 * 部分渠道是通过参数形式的，基本是GET，但同时需要兼容POST（如360渠道建议要支持GET/POST两种方法）
	 */
	private Map<String, String> readContentAsParams(HttpRequest request)
			throws Exception {
		Map<String, String> params = new HashMap<String, String>();
		// HttpRequest request = (HttpRequest) e.getMessage();

		// GET>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		if (HttpMethod.GET.equals(request.getMethod())) {
			logger.info("【GET】{}", request.getUri());
			QueryStringDecoder decoderQuery = new QueryStringDecoder(
					request.getUri(), true);
			Map<String, List<String>> uriAttributes = decoderQuery
					.getParameters();
			for (String key : uriAttributes.keySet()) {
				for (String valuen : uriAttributes.get(key)) {
					params.put(key, valuen);
					logger.debug("[Param] {} = {}", key, valuen);
				}
			}
		}
		// POST>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		else if (HttpMethod.POST.equals(request.getMethod())
				|| HttpMethod.PUT.equals(request.getMethod())) {
			if (request.isChunked()) {
				// chunk方式
				logger.error("Not Support Chunk Version.");
			} else {
				// 非chunk方式
				HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(
						request);
				List<InterfaceHttpData> datas = postDecoder.getBodyHttpDatas();
				if (datas == null || datas.size() <= 0) {
					// String postContent = request.getContent().toString(
					// Charset.forName("UTF-8"));
					// logger.info("【POST】{}", postContent);
				} else {
					for (InterfaceHttpData data : datas) {
						if (data.getHttpDataType() == HttpDataType.Attribute) {
							Attribute attribute = (Attribute) data;
							logger.debug("[Attribute] {} = {}",
									attribute.getName(), attribute.getValue());
							params.put(attribute.getName(),
									attribute.getValue());
						}
					}
				}
				//*******附加的参数，哪怕是POST都有可能在请求地址中有参数的********/
				String query = null;
				try {
					URI uri = new URI(request.getUri());
					query = uri.getRawQuery();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				if(query!=null&&query.length()>0){
					QueryStringDecoder decoderQuery = new QueryStringDecoder(query,false);
					Map<String, List<String>> uriAttributes = decoderQuery
							.getParameters();
					for (String key : uriAttributes.keySet()) {
						for (String valuen : uriAttributes.get(key)) {
							params.put(key, valuen);
							logger.debug("[Query.Param] {} = {}", key, valuen);
						}
					}
				}
			}
		}
		return params;
	}

	/**
	 * 部分渠道是通过POST一段JSON数据的方式
	 */
	private String readContentAsString(HttpRequest request) {
		// GET>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		if (HttpMethod.GET.equals(request.getMethod())) {
			URI uri=null;
			try {
				uri = new URI(request.getUri());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			return uri==null?"":uri.getQuery();
		}
		// POST>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		else if (HttpMethod.POST.equals(request.getMethod())
				|| HttpMethod.PUT.equals(request.getMethod())) {
			String postContent = request.getContent().toString(
					Charset.forName("UTF-8"));
			
			//*******附加的参数，哪怕是POST都有可能在请求地址中有参数的********/
			String query = null;
			try {
				URI uri = new URI(request.getUri());
				query = uri.getRawQuery();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			if(query!=null&&query.length()>0){
				postContent = (new StringBuilder().append(postContent).append("&").append(query)).toString();
			}
			
			logger.info("【POST】{}", postContent);
			return postContent;
		}
		return "";
	}

	private void doResponse(Channel channel, String responsecontent) {
		ChannelBuffer buf = ChannelBuffers.copiedBuffer(responsecontent,
				CharsetUtil.UTF_8);
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
				HttpResponseStatus.OK);
		response.setContent(buf);
		response.setHeader(HttpHeaders.Names.CONTENT_TYPE,
				"text/plain; charset=UTF-8");
		response.setHeader(HttpHeaders.Names.CONTENT_LENGTH,
				String.valueOf(buf.readableBytes()));
		channel.write(response).addListener(ChannelFutureListener.CLOSE);
	}

	// HttpMessage currentMessage;
	// int maxContentLength = 3000;
	//
	// public boolean excuteChunk(ChannelHandlerContext ctx, MessageEvent e)
	// throws TooLongFrameException {
	// // HttpMessage currentMessage = e.getMessage();
	//
	// if (e.getMessage() instanceof HttpMessage) {
	// HttpMessage m = (HttpMessage) e.getMessage();
	// if (m.isChunked()) {
	// // A chunked message - remove 'Transfer-Encoding' header,
	// // initialize the cumulative buffer, and wait for incoming
	// // chunks.
	// List<String> encodings = m
	// .getHeaders(HttpHeaders.Names.TRANSFER_ENCODING);
	// encodings.remove(HttpHeaders.Values.CHUNKED);
	// if (encodings.isEmpty()) {
	// m.removeHeader(HttpHeaders.Names.TRANSFER_ENCODING);
	// }
	// m.setContent(ChannelBuffers.dynamicBuffer(e.getChannel()
	// .getConfig().getBufferFactory()));
	// this.currentMessage = m;
	// } else {
	// // Not a chunked message - pass through.
	// this.currentMessage = null;
	// }
	// return false;
	// } else if (e.getMessage() instanceof HttpChunk) {
	// // Sanity check
	// if (currentMessage == null) {
	// throw new IllegalStateException("received "
	// + HttpChunk.class.getSimpleName() + " without "
	// + HttpMessage.class.getSimpleName());
	// }
	//
	// // Merge the received chunk into the content of the current message.
	// HttpChunk chunk = (HttpChunk) e.getMessage();
	// ChannelBuffer content = currentMessage.getContent();
	//
	// if (content.readableBytes() > maxContentLength
	// - chunk.getContent().readableBytes()) {
	// throw new TooLongFrameException("HTTP content length exceeded "
	// + maxContentLength + " bytes.");
	// }
	//
	// content.writeBytes(chunk.getContent());
	// if (chunk.isLast()) {
	// this.currentMessage = null;
	// currentMessage.setHeader(HttpHeaders.Names.CONTENT_LENGTH,
	// String.valueOf(content.readableBytes()));
	// return true;
	// // Channels.fireMessageReceived(ctx, currentMessage,
	// // e.getRemoteAddress());
	// }
	// }
	// return true;
	// }

	// ///////////////////////////////////////////////////////////////////////////////
	private Map<String, String> getRequestParams(HttpRequest request) {
		Map<String, String> ps = new HashMap<String, String>();
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(
				request.getUri());
		logger.info("rawPath={}", queryStringDecoder.getPath());
		Map<String, List<String>> params = queryStringDecoder.getParameters();
		if (!params.isEmpty()) {
			for (Entry<String, List<String>> p : params.entrySet()) {
				String key = p.getKey();
				List<String> vals = p.getValue();
				for (String val : vals) {
					logger.info("request params ({}={})", key, val);
					ps.put(key, val);
				}
			}
		}
		return ps;
	}
	
//public static void main(String[] args) throws Exception {
//	String ur = "/1201?ext=p1201g223r51&a=199";
//	URI u = new URI(ur);
//	String p = u.getRawPath();
//	System.out.println(u.getQuery());
//	System.out.println(u.getRawQuery());
//	System.out.println(u.getPath());
//	System.out.println(p);
//	System.out.println(Integer.parseInt(p.substring(p.lastIndexOf("/") + 1)));
//}
	
	private int getPromoIDbyUriPath(HttpRequest request) {
		try {
			URI u = new URI(request.getUri());
			String p = u.getRawPath();
			int pid = Integer.parseInt(p.substring(p.lastIndexOf("/") + 1));
			return pid;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (Exception e) {
		}
		return -1;
	}
	
	private void _printHttpInfo(HttpRequest request) {
		StringBuilder buf = new StringBuilder();
		buf.append("VERSION: " + request.getProtocolVersion() + "\r\n");
		buf.append("HOSTNAME: " + getHost(request, "unknown") + "\r\n");
		buf.append("REQUEST_URI: " + request.getUri() + "\r\n\r\n");
		for (Map.Entry<String, String> h : request.getHeaders()) {
			buf.append("HEADER: " + h.getKey() + " = " + h.getValue() + "\r\n");
		}
		buf.append("\r\n");
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(
				request.getUri());
		Map<String, List<String>> params = queryStringDecoder.getParameters();
		if (!params.isEmpty()) {
			for (Entry<String, List<String>> p : params.entrySet()) {
				String key = p.getKey();
				List<String> vals = p.getValue();
				for (String val : vals) {
					buf.append("PARAM: " + key + " = " + val + "\r\n");
				}
			}
			buf.append("\r\n");
		}
		System.out.println(buf.toString());
	}
	
	// ///////////////////////////////////////////////////////////////////////////////

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		e.getCause().printStackTrace();
	}

//	public static void main(String[] args) {
//		String u = "/1008?orderId=P1008R10T20130726124632520N24&skyId=365007926&resultCode=0&payNum=31307261249140108799&cardType=13&realAmount=1000&payTime=20130726124911&failure=0&ext1=p1008g1r10&signMsg=19196E16446671346C08B8B1B9979DD2";
//		try {
//			URI uri = new URI(u);
//			System.out.println(uri.getPath());
//			System.out.println(uri.getRawPath());
//			System.out.println(uri.getQuery());
//			System.out.println(uri.getRawQuery());
//		} catch (URISyntaxException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
}
