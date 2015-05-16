package com.koala.game.resserver;

import static org.jboss.netty.handler.codec.http.HttpHeaders.getHost;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.MimetypesFileTypeMap;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFutureProgressListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;

import com.koala.game.util.KGameExcelFile;
import com.koala.game.util.KGameExcelTable;
import com.koala.game.util.KGameExcelTable.KGameExcelRow;

/**
 * 资源管理， 关键是管理服务器和客户端各自的资源以及版本，更新等问题
 * 
 * @author AHONG
 */
final class KGameHttpResfileServerHandler extends SimpleChannelHandler
		implements KGameHttpProtocol {

	private KGameResfileList reslist;
	// <resid,KGameResfile>
	private static final ConcurrentHashMap<Integer, KGameResfile> resfiles = new ConcurrentHashMap<Integer, KGameResfile>();

	public void loadAllResource(String resRootDirectory) throws Exception {

		KGameExcelFile excel = new KGameExcelFile("res/gres/reslist.xls");
		String[] names = excel.getAllSheetNames();
		int reslistrevision = Integer.MIN_VALUE;
		for (String string : names) {
			int i = Integer.parseInt(string);
			if (i > reslistrevision) {
				reslistrevision = i;
			}
		}
		reslist = new KGameResfileList(reslistrevision);
		KGameExcelTable table = excel.getTable("" + reslistrevision, 1);
		KGameExcelRow[] rows = table.getAllDataRows();
		for (KGameExcelRow r : rows) {
			Integer resid = r.getInt("resid");
			String uri = r.getData("uri");
			uri = uri.replace('/', File.separatorChar);
			Integer revision = r.getInt("revision");

			reslist.list.put(resid, revision);

			KGameResfile resfile = new KGameResfile(resid, uri, revision);
			resfile.absolutePath = System.getProperty("user.dir")
					+ File.separator + uri;
			resfile.load();// 加载到缓存
			resfiles.put(resid, resfile);

			System.out.println(resfile.absolutePath);
			System.out.println(resid + " " + uri + " " + revision + " "
					+ resfile.fileSize());
		}
		System.out.println(reslist.toString());
	}

	public List<KGameResfile> checkUpdatableReslist(
			KGameResfileList clientReslist) {
		List<KGameResfile> list = new ArrayList<KGameResfile>();
		for (Integer resid : reslist.list.keySet()) {
			Integer revision = reslist.list.get(resid);
			Integer crevision = clientReslist.list.get(resid);
			if (crevision != null) {
				if (crevision < revision) {
					list.add(resfiles.get(resid));
				}
			} else {
				list.add(resfiles.get(resid));
			}
		}
		return list;
	}

	public KGameResfile getRes(Integer resid) {
		return resfiles.get(resid);
	}

	public List<KGameResfile> getRes(Collection<Integer> resids) {
		List<KGameResfile> list = new ArrayList<KGameResfile>();
		for (Integer resid : resids) {
			list.add(resfiles.get(resid));
		}
		return list;
	}

	// //////////////////////////////////////////////////////////////////////////////////

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		HttpRequest request = (HttpRequest) e.getMessage();
		System.out.println("--------------------------------" + request);
		Channel channel = ctx.getChannel();
		if (request.getMethod() != GET) {
			sendError(ctx, METHOD_NOT_ALLOWED);
			return;
		}

		boolean isDirectDownload;
		int resid = -1;
		final File file;
		int _offset;
		long _fileLength;
		RandomAccessFile raf = null;

		// TODO【临时，只判断zip资源包】判断是否直接下载文件
		final String path = sanitizeUri(request.getUri());
		if (path != null && path.endsWith(".zip")) {
			file = new File(path);
			if (file.isHidden() || !file.exists()) {
				sendError(ctx, NOT_FOUND);
				return;
			}
			if (!file.isFile()) {
				sendError(ctx, FORBIDDEN);
				return;
			}

			try {
				raf = new RandomAccessFile(file, "r");
			} catch (FileNotFoundException fnfe) {
				sendError(ctx, NOT_FOUND);
				fnfe.printStackTrace();
				return;
			}
			_offset = 0;
			_fileLength = raf.length();
			isDirectDownload = true;
		} else {

			// ///////////////////////////////////////////////////////////
			// PRINT HEADERS AND PARAMS
			// _printHttpInfo(request);
			// ////////////////////////////////////////////////////////////////

			int revision;
			int offset;
			int len;
			try {
				Map<String, String> params = getRequestParams(request);
				if (params.size() <= 0) {
					return;
				}
				resid = Integer.parseInt(params.get(URL_PARAM_RESID));
				revision = Integer.parseInt(params.get(URL_PARAM_REVISION));
				offset = Integer.parseInt(params.get(URL_PARAM_OFFSET));
				len = Integer.parseInt(params.get(URL_PARAM_LENGTH));
			} catch (Exception e1) {
				sendError(ctx, BAD_REQUEST);
				e1.printStackTrace();
				return;
			}

			final KGameResfile rfile = resfiles.get(resid);
			System.out.println(rfile);

			file = rfile.file;

			try {
				raf = new RandomAccessFile(file, "r");
			} catch (FileNotFoundException fnfe) {
				sendError(ctx, NOT_FOUND);
				fnfe.printStackTrace();
				return;
			}
			long fileLength = raf.length();

			// 修正文件读取起点
			if (offset < 0 || offset >= fileLength) {
				offset = 0;
			}

			// 修正文件读取长度
			if (len < 0 || (len + offset) > fileLength) {
				len = (int) (fileLength - offset);
			}

			// CHECK PARAMS
			if ((offset < 0 || offset > fileLength - 1)
					|| (len < 0 || len > fileLength)
					|| (revision > rfile.revision)) {
				sendError(ctx, BAD_REQUEST);
				System.err.println("PARAMS ERROR");
				return;
			}

			_offset = offset;
			_fileLength = len;

			isDirectDownload = false;
		}

		// RESPONSE
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
		setContentLength(response, _fileLength);
		setContentTypeHeader(response, file);
		if (!isDirectDownload) {
			response.setHeader(HEADER_RESID, resid);// 自定义Header-资源ID
		}

		// System.out.println(response);

		// Write the initial line and the header.
		channel.write(response);

		// Write the content.
		ChannelFuture writeFuture;
		// No encryption - use zero-copy.
		final FileRegion region = new DefaultFileRegion(raf.getChannel(),
				_offset, _fileLength);
		writeFuture = channel.write(region);
		writeFuture.addListener(new ChannelFutureProgressListener() {
			public void operationComplete(ChannelFuture future) {
				// KGameHttpResfileServer.logger.info("===operationComplete "+future);
				region.releaseExternalResources();
			}

			public void operationProgressed(ChannelFuture future, long amount,
					long current, long total) {
				System.out.printf("%s: %d / %d (+%d)%n",
						file.getAbsolutePath(), current, total, amount);
			}
		});

		// Decide whether to close the connection or not.
		if (!isKeepAlive(request)) {
			// Close the connection when the whole content is written out.
			writeFuture.addListener(ChannelFutureListener.CLOSE);
		}

		System.out.println("-----------------------------Request End.\r\n");
	}

	// private boolean isDirectDownloadZipRespack(HttpRequest request){
	//
	// return false;
	// }

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {

		e.getCause().printStackTrace();

		// super.exceptionCaught(ctx, e);
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		KGameHttpResfileServer.logger.info("writeRequested");
		super.writeRequested(ctx, e);
	}

	@Override
	public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e)
			throws Exception {
		KGameHttpResfileServer.logger.info("writeComplete");
		super.writeComplete(ctx, e);
	}

	private static Map<String, String> getRequestParams(HttpRequest request) {
		Map<String, String> ps = new HashMap<String, String>();
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(
				request.getUri());
		Map<String, List<String>> params = queryStringDecoder.getParameters();
		if (!params.isEmpty()) {
			for (Entry<String, List<String>> p : params.entrySet()) {
				String key = p.getKey();
				List<String> vals = p.getValue();
				for (String val : vals) {
					// System.err.println(key + " : " + val);
					ps.put(key, val);
				}
			}
		}
		return ps;
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

	private static void sendError(ChannelHandlerContext ctx,
			HttpResponseStatus status) {
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
		response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
		response.setContent(ChannelBuffers.copiedBuffer(
				"Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));

		// Close the connection as soon as the error message is sent.
		ctx.getChannel().write(response)
				.addListener(ChannelFutureListener.CLOSE);
	}

	private static void setContentTypeHeader(HttpResponse response, File file) {
		MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
		response.setHeader(HttpHeaders.Names.CONTENT_TYPE,
				mimeTypesMap.getContentType(file.getPath()));
	}

	private static String sanitizeUri(String uri) {
		// Decode the path.
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			try {
				uri = URLDecoder.decode(uri, "ISO-8859-1");
			} catch (UnsupportedEncodingException e1) {
				throw new Error();
			}
		}

		// Convert file separators.
		uri = uri.replace('/', File.separatorChar);

		// Simplistic dumb security check.
		// You will have to do something serious in the production environment.
		if (uri.contains(File.separator + ".")
				|| uri.contains("." + File.separator) || uri.startsWith(".")
				|| uri.endsWith(".")) {
			return null;
		}

		// Convert to absolute path.
		return System.getProperty("user.dir") + File.separator + uri;
	}
}
