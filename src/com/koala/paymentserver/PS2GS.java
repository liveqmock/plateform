package com.koala.paymentserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import com.koala.game.KGameMessage;
import com.koala.game.communication.KGameCommunication;
import com.koala.game.communication.KGameMessageDecoder;
import com.koala.game.communication.KGameMessageEncoder;
import com.koala.game.exception.KGameServerException;
import com.koala.game.logging.KGameLogger;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimerTask;
import com.koala.game.util.DateUtil;
import com.koala.paymentserver.PayOrder.OrderStatus;
import com.koala.promosupport.PromoChannel;
import com.koala.promosupport.PromoSupport;
import com.koala.thirdpart.json.JSONObject;

/**
 * PaymentServer跟GameServer直接的通信。 PS作为一个客户端跟GS通信，传递支付指令和信息，让GS做发货操作
 * 
 * @author AHONG
 * 
 */
public class PS2GS extends SimpleChannelUpstreamHandler implements
		KGameTimerTask, PaymentProtocol {

	private static final KGameLogger logger = KGameLogger
			.getLogger(PS2GS.class);
	private int gsid = -1;
	//////////////////////////////////
	private String ip;
	private int port;
	// String passport;
	// String password;
	//////////////////////////////////

	private Channel channel;
	private final AtomicBoolean connecting = new AtomicBoolean();
	private final static int CONN_NULL = 0x0;
	private final static int CONN_NEW = 0x1;
	private final static int CONN_FINE = 0x2;
	//private final AtomicBoolean handshaked = new AtomicBoolean();

	private final Lock lockprocessorder = new ReentrantLock();
	// 缓存的订单对象，主要用于状态设置及“判断是否重复SDK服务器callback通知”。
	// 要有时间限制，采用FIFO的形式，超过20分钟的踢出。
	final ConcurrentHashMap<PayOrderID, PayOrder> processedpayordercache = new ConcurrentHashMap<PayOrderID, PayOrder>();
	// 待处理（发送到GS处理）订单队列
	final ConcurrentLinkedQueue<PayOrder> waitingprocesspayorderqueue = new ConcurrentLinkedQueue<PayOrder>();

	private final SendOrderWorker workerSendOrder = new SendOrderWorker();
	private final Thread workerThread;
	private final AtomicBoolean isloadtempfile = new AtomicBoolean();
	private final AtomicBoolean stop = new AtomicBoolean();

	public PS2GS(String ip, int port) {
		this.ip = ip;
		this.port = port;
		workerThread = Executors.defaultThreadFactory().newThread(
				workerSendOrder);
	}

	public int getGsid() {
		return gsid;
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	// ////////////////////////////////////////////////////////////////////
	// Message Handle
	// ////////////////////////////////////////////////////////////////////

	private void handshake() {
		KGameMessage msg = newMessage(MID_HANDSHAKE);
		msg.writeUtf8String("pay");
		// 当客户端要进行充值时会向GS获取一些参数（各渠道不同）发去给SDK服务器。
		// 所以在PS跟GS握手的时候就把这些参数先发给GS缓存起来（注：GS是没有渠道相关的内容的）
		Set<Integer> idset = PromoSupport.getInstance().getSupportPromoIdSet();
		int writeindex = msg.writerIndex();
		msg.writeInt(idset.size());
		int counter =0;
		for (Integer promoid : idset) {
			PromoChannel ch = PromoSupport.getInstance().getPromoChannel(promoid);
			if (ch != null && ch.canPay()) {
				msg.writeInt(promoid);
				Map<String, String> pps = ch.getParamsToClientBeforePay();
				logger.debug("--------------------------------------------{}({})", promoid, pps.size());
				msg.writeInt(pps.size());
				for (String pk : pps.keySet()) {
					String pv = pps.get(pk);
					msg.writeUtf8String(pk);
					msg.writeUtf8String(pv);
					logger.debug("promochannel({}) paybefore's params: {} = {}", promoid, pk, pv);
				}
				counter++;
			}
		}
		msg.setInt(writeindex, counter);
		logger.debug("handshake with gs:{},promosize:{}",gsid,counter);

		channel.write(msg);
	}

	private void ping() {
		KGameMessage msg = newMessage(MID_PING);
		msg.writeLong(System.currentTimeMillis());
		channel.write(msg);
	}

	/**
	 * 重点：充值时通知给GS 【本方法会检测是否重复的订单】
	 * 
	 * <pre>
	 * 【注意】：
	 *  由于存在多次发送通知的情况，因此“游戏服务器”必须能够正确处理重复的通知。
	 *  当接收到通知时，需要检查系统内对应业务数据的状态，以判断该通知是否已经处理过。
	 *  在对业务数据进行状态检查或处理之前，需要采取数据加锁或时间戳判断等方式进行并
	 *  发控制。
	 *  由于支付网关的通知机制原因，偶尔会发生通知支付失败后又通知支付成功的现象。
	 *  基于这个情况，“游戏服务器”在处理充值结果通知时，对同一个订单，如果先接收到
	 *  支付失败，再接收到支付成功的通知，应以成功的支付结果为准，替换原接收到的失败
	 *  的支付结果。一旦通知支付成功，不会再发生通知支付失败的情形。
	 * </pre>
	 * 
	 * @return true表示接受处理；false表示不接受，例如是重复通知，这时回调应该返回给SDK服务器“成功”的标记
	 * @throws Exception
	 */
	public boolean processPayOrder(PayOrder order) throws Exception {
		if (lockprocessorder.tryLock()) {
			try {
				// 检测是否重复的订单
				PayOrder existOrder = processedpayordercache.get(order
						.getPayOrderID());
				if (existOrder != null) {
					// 已经存在
					if (existOrder.getOrderStatus() == OrderStatus.DONE_SUCCESS) {
						logger.error("【订单已经处理，重复通知】{}", existOrder);
						return false;// 已经成功的订单
					}
				}
				// 放入等待队列
				if (waitingprocesspayorderqueue.contains(order)) {
					logger.error("【订单在处理队列中，重复通知】{}", existOrder);
				} else {
					waitingprocesspayorderqueue.add(order);
				}
			} finally {
				lockprocessorder.unlock();
			}
		}
		// KGameMessage msg = newMessage(MID_PS2GS_PAY);
		// msg.writeUtf8String(order.toJSON());
		// send(msg);
		return true;
	}

	private final void sendorder2gs(PayOrder order) throws Exception {
		KGameMessage msg = newMessage(MID_PS2GS_PAY);
		msg.writeUtf8String(order.toJSON());
		if ((ensureConnection() & CONN_FINE) != 0) {
			channel.write(msg);
		} else {// 如果网络不通先放回Q
			waitingprocesspayorderqueue.offer(order);
		}
	}

	synchronized void start() {
		if (PaymentServer.getIntance().shutdownflag.get()) {
			throw new IllegalStateException("cannot be started once stopped");
		}
		if (!workerThread.isAlive()) {
			workerThread.start();
		}
		logger.warn("PS2GS({}) 's worker thread started. {}",workerThread.isAlive());
	}

	/**
	 * @return unprocessedOrder
	 */
	synchronized Set<PayOrder> stop() {
		if (Thread.currentThread() == workerThread) {
			throw new IllegalStateException(PS2GS.class.getSimpleName()
					+ ".stop() cannot be called by same thread.");
		}
		if (stop.compareAndSet(false, true)) {
			logger.error("writeCachedPayOrdersToTempFile...gsid:{}", getGsid());
			int line = writeCachedPayOrdersToTempFile();
			logger.error("writeCachedPayOrdersToTempFile Done! {}", line);

			boolean interrupted = false;
			while (workerThread.isAlive()) {
				workerThread.interrupt();
				try {
					workerThread.join(100);
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}

			if (interrupted) {
				Thread.currentThread().interrupt();
			}

			Set<PayOrder> unprocessedOrder = new HashSet<PayOrder>();
			unprocessedOrder.addAll(waitingprocesspayorderqueue);
			return unprocessedOrder;
		}
		return Collections.emptySet();
	}

	private int writeCachedPayOrdersToTempFile() {
		int lineN = 0;
		try {
			File tmpfile = new File(
					PaymentServer.getIntance().tempFileRecordCachedPayOrders
							+ gsid);
			if (tmpfile.exists()) {
				tmpfile.delete();
			}
			tmpfile.createNewFile();
				
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(
					tmpfile)));
			for (PayOrderID pid : processedpayordercache.keySet()) {
				PayOrder order = processedpayordercache.get(pid);
				pw.println(order.toJSON());
				lineN++;
			}
			//pw.flush();
			pw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lineN;
	}

	private int loadCachedPayOrdersFromTempFile() {
		int lineN = 0;
		try {
			File tmpfile = new File(
					PaymentServer.getIntance().tempFileRecordCachedPayOrders
							+ gsid);
			if (tmpfile.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(tmpfile));
				String line;
				while ((line = br.readLine()) != null) {
					PayOrder order = new PayOrder(line);
					processedpayordercache.put(order.getPayOrderID(), order);
					newTimeTaskRemoveCachedPayOrder(order);
					lineN++;
				}
				br.close();
//				//读完之后不删除，直接改名
//				tmpfile.renameTo(new File(tmpfile.getPath()+"."+DateUtil.formatDefault(new Date())));
			}else{
				logger.warn("tmpfile not exists..{}",tmpfile.getPath());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lineN;
	}
	
	private void newTimeTaskRemoveCachedPayOrder(PayOrder payOrder) {
		if (payOrder != null) {
			try {
				TimeTaskRemoveCachedPayOrder tt = new TimeTaskRemoveCachedPayOrder(
						payOrder.getPayOrderID());
				long delay = (DateUtil.parse(payOrder.getPaytime(),
						PayOrder.FORMAT_PAYTIME).getTime() / 1000)
						+ PaymentServer.getIntance().payOrderCacheDurationSeconds;
				PaymentServer.getIntance().timer.newTimeSignal(tt, delay,
						TimeUnit.SECONDS);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}

	private final class SendOrderWorker implements Runnable {
		@Override
		public void run() {
			boolean interrupted = false;
			try {
				while ((!PaymentServer.getIntance().shutdownflag.get())
						&& (!stop.get())) {
					PayOrder order = waitingprocesspayorderqueue.poll();
					if (order != null) {
						switch (order.getOrderStatus()) {
						case WAITING2PROCESS:
							try {
								order.setOrderStatus(OrderStatus.PROCESSING);
								sendorder2gs(order);
							} catch (Exception e) {
								logger.error("QPoll【订单JSON异常】{}", order);
							}
							break;
						case DONE_FAILED:
						case DONE_SUCCESS:
						case PROCESSING:
						default:
							logger.error("QPoll【订单已处理】{}", order);
							break;
						}
					}
					// sleep
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
						interrupted = true;
					}
				}// ~while
			} finally {
				if (interrupted)
					Thread.currentThread().interrupt();
			}
		}//~run
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		if (!(e.getMessage() instanceof KGameMessage)) {
			// 非法消息，logging & disconnect
			logger.warn("Illegal Message Type! Must be KGameMessage. {}",
					e.getMessage());
			return;
		}

		Channel channel = e.getChannel();
		KGameMessage kmsg = (KGameMessage) e.getMessage();

		if (kmsg.getMsgType() != KGameMessage.MTYPE_PLATFORM) {
			logger.warn("Illegal Message Type for PaymentGS {}",
					kmsg.getMsgType());
			return;
		}
		if (kmsg.getClientType() != KGameMessage.CTYPE_PAYMENT) {
			logger.warn("Illegal Client Type for PaymentGS {}",
					kmsg.getMsgType());
			return;
		}

		int msgID = kmsg.getMsgID();
		switch (msgID) {

		case MID_HANDSHAKE:
			String serverinfo = kmsg.readUtf8String();
			JSONObject json = new JSONObject(serverinfo);
			this.gsid = json.getInt("gsid");

			logger.info("HANDSHAKE({}),{}", serverinfo, this);

			// 握手返回表示连接成功，之后要不断ping
			PaymentServer.getIntance().timer.newTimeSignal(this,
					PaymentServer.getIntance().pingIntervalSeconds,
					TimeUnit.SECONDS);
			
			// 加载临时文件中的PayOrder
			if (isloadtempfile.compareAndSet(false, true)) {
				int line = loadCachedPayOrdersFromTempFile();
				logger.info("gs({}) loadCachedPayOrdersFromTempFile, N={}",
						getGsid(), line);
			}
			//handshaked.set(true);
			break;

		case MID_PING:
			long pingt = kmsg.readLong();
			// logger.debug("PING (roundtime:{}ms),{}",
			// (System.currentTimeMillis() - pingt),this);
			break;

		case MID_PS2GS_PAY:
			int result = kmsg.readInt();
			String payorderstring = kmsg.readUtf8String();
			PayOrder payOrderFromGS = new PayOrder(payorderstring);
			logger.info("【GS返回订单处理结果】 (result:{}) payOrder={}", result,
					payOrderFromGS);
			PayOrderID poid = payOrderFromGS.getPayOrderID();
			// 设置订单状态
			if (lockprocessorder.tryLock()) {
				try {
					PayOrder cpayOrder = processedpayordercache.get(poid);
					if (cpayOrder != null) {
						// 缓存中存在
						cpayOrder
								.setOrderStatus(result == 0 ? OrderStatus.DONE_SUCCESS
										: OrderStatus.DONE_FAILED);
					} else {
						// 缓存中不存在
						payOrderFromGS
								.setOrderStatus(result == 0 ? OrderStatus.DONE_SUCCESS
										: OrderStatus.DONE_FAILED);
						processedpayordercache.put(poid, payOrderFromGS);
						newTimeTaskRemoveCachedPayOrder(payOrderFromGS);
					}

				} finally {
					lockprocessorder.unlock();
				}
			}
			break;
		}
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		logger.info("channelConnected. {} -> {}", e.getChannel()
				.getLocalAddress(), e.getChannel().getRemoteAddress());
//		// 连接上GS后就握手
//		handshake();
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		logger.info("channelClosed. {} -> {}",
				e.getChannel().getLocalAddress(), e.getChannel()
						.getRemoteAddress());
		//断开就未握手
		//handshaked.set(false);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		logger.warn("exceptionCaught.{} -> {},{}", e.getChannel()
				.getLocalAddress(), e.getChannel().getRemoteAddress(), e);
		// //连接异常
		// if(e.getCause() instanceof ConnectException){
		// System.out.println("ooo");
		// }
	}

	// ///////////////////////////////////////////////////////////////////
	// Timer Task
	// ///////////////////////////////////////////////////////////////////

	@Override
	public String getName() {
		return PaymentServer.class.getSimpleName();
	}

	@Override
	public Object onTimeSignal(KGameTimeSignal timeSignal)
			throws KGameServerException {
		if (!stop.get()) {
			try {
				if ((ensureConnection() | CONN_FINE) != 0) {// 保证有连接（没有则重连）
					// if (handshaked.get()) {//保证是握手后才发PING
					ping();
					// }
				}
			} finally {
				timeSignal.getTimer().newTimeSignal(this,
						PaymentServer.getIntance().pingIntervalSeconds,
						TimeUnit.SECONDS);
			}
		}
		return null;
	}

	@Override
	public void done(KGameTimeSignal timeSignal) {
	}

	@Override
	public void rejected(RejectedExecutionException e) {
		logger.error("RejectedExecutionException {}", e);
	}

	// ///////////////////////////////////////////////////////////////////////////

	private static KGameMessage newMessage(int msgID) {
		return KGameCommunication.newMessage(KGameMessage.MTYPE_PLATFORM,
				KGameMessage.CTYPE_PAYMENT, msgID);
	}
	
	/**断开连接*/
	synchronized void disconnect() {
		if (channel != null) {
			channel.close();
		}
	}

	/**检测连接是否正常，不正常重连*/
	synchronized int ensureConnection() {
		int re = CONN_NULL;
		if (connecting.compareAndSet(false, true)) {
			try {
				// 如果没有连接就建立
				if (channel == null || (!channel.isConnected())) {
					ClientBootstrap bootstrap = new ClientBootstrap(
							new NioClientSocketChannelFactory(
									Executors.newSingleThreadExecutor(),
									Executors.newSingleThreadExecutor()));
					bootstrap
							.setPipelineFactory(new PaymentGSPipelineFactory());
					ChannelFuture connectFuture = bootstrap
							.connect(new InetSocketAddress(ip, port));
					channel = connectFuture.awaitUninterruptibly().getChannel();
					logger.info("Connect>>> {} -> {}",
							channel.getLocalAddress(),
							channel.getRemoteAddress());
					
					re |= CONN_NEW;
					
					//马上做握手
					handshake();

				}
			} finally {
				connecting.set(false);
			}
		}
		if (channel != null && channel.isConnected()) {
			re |= CONN_FINE;
		} else {
			re = CONN_NULL;
		}
		return re;
	}

	@Override
	public String toString() {
		return "PS2GS [gsid=" + gsid + ", ip=" + ip + ", port=" + port
				+ ", channel=" + channel + "]";
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////inner classes/////////////////////////////////////////
	
	private final class PaymentGSPipelineFactory implements
			ChannelPipelineFactory {
		@Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeline = Channels.pipeline();
			// 自定义编码解码器
			pipeline.addLast("kgame_decoder", new KGameMessageDecoder());
			pipeline.addLast("kgame_encoder", new KGameMessageEncoder());
			pipeline.addLast("handler", PS2GS.this);
			return pipeline;
		}
	}
	
	private final class TimeTaskRemoveCachedPayOrder implements KGameTimerTask{

		private PayOrderID pid;
		
		TimeTaskRemoveCachedPayOrder(PayOrderID pid){
			this.pid = pid;
		}
		
		@Override
		public String getName() {
			return TimeTaskRemoveCachedPayOrder.class.getSimpleName();
		}

		@Override
		public Object onTimeSignal(KGameTimeSignal timeSignal)
				throws KGameServerException {
			PayOrder po = processedpayordercache.remove(pid);
			if (po != null) {
				logger.warn("【onTime Remove PayOrder】{}", po);
			}
			return null;
		}

		@Override
		public void done(KGameTimeSignal timeSignal) {
		}

		@Override
		public void rejected(RejectedExecutionException e) {
			logger.warn("RejectedExecutionException remove PayOrder {}",pid);
		}
		
	}
}
