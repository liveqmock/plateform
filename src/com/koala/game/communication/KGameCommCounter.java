package com.koala.game.communication;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.koala.game.util.DateUtil;
/**
 * 通信统计器，统计消息来回总量、连接数量、异常等
 * @author AHONG
 *
 */
public class KGameCommCounter {
	
	private String name;
	
	public KGameCommCounter(String name) {
		this.name = name;
	}

	private AtomicLong      writeRequestedCount = new AtomicLong();
	private AtomicLong       writeCompleteCount = new AtomicLong();
	private AtomicLong       writtenAmountCount = new AtomicLong();
	private AtomicLong     messageReceivedCount = new AtomicLong();
	private AtomicLong    channelConnectedCount = new AtomicLong();
	private AtomicLong channelDisconnectedCount = new AtomicLong();
	private AtomicLong         channelOpenCount = new AtomicLong();
	private AtomicLong       channelClosedCount = new AtomicLong();
	private AtomicLong     exceptionCaughtCount = new AtomicLong();
	private AtomicLong          handshakedCount = new AtomicLong();
	private AtomicLong             loginedCount = new AtomicLong();
	
	public void writeRequested       (){     writeRequestedCount.incrementAndGet();}
	public void writeComplete        (long writtenAmount){      writeCompleteCount.incrementAndGet();writtenAmountCount.addAndGet(writtenAmount);}
	public void messageReceived      (){    messageReceivedCount.incrementAndGet();}
	public void channelConnected     (){   channelConnectedCount.incrementAndGet();}
	public void channelDisconnected  (){channelDisconnectedCount.incrementAndGet();}
	public void channelOpen          (){        channelOpenCount.incrementAndGet();}
	public void channelClosed        (){      channelClosedCount.incrementAndGet();}
	public void exceptionCaught      (){    exceptionCaughtCount.incrementAndGet();}
	public void handshaked           (){         handshakedCount.incrementAndGet();}
	public void logined              (){            loginedCount.incrementAndGet();}
	
	public long      writeRequestedCount(){return      writeRequestedCount.get();}
	public long       writeCompleteCount(){return       writeCompleteCount.get();}
	public long       writtenAmountCount(){return       writtenAmountCount.get();}
	public long     messageReceivedCount(){return     messageReceivedCount.get();}
	public long    channelConnectedCount(){return    channelConnectedCount.get();}
	public long channelDisconnectedCount(){return channelDisconnectedCount.get();}
	public long         channelOpenCount(){return         channelOpenCount.get();}
	public long       channelClosedCount(){return       channelClosedCount.get();}
	public long     exceptionCaughtCount(){return     exceptionCaughtCount.get();}
	public long          handshakedCount(){return          handshakedCount.get();}
	public long             loginedCount(){return             loginedCount.get();}
	
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder(KGameCommCounter.class.getSimpleName()).append("-").append(name).append("  ");
//		
//		sb.append("open,");
//		sb.append("conn,");
//		sb.append("disconn,");
//		sb.append("closed,");
//		sb.append("msgRev,");
//		sb.append("wReq,");
//		sb.append("wComp,");
//		sb.append("excep,");
//		sb.append("hsed,");
//		sb.append("logined = ");
//		
//		sb.append(channelOpenCount()).append(",");
//		sb.append(channelConnectedCount()).append(",");
//		sb.append(channelDisconnectedCount()).append(",");
//		sb.append(channelClosedCount()).append(",");
//		sb.append(messageReceivedCount()).append(",");
//		sb.append(writeRequestedCount()).append(",");
//		sb.append(writtenAmountCount()).append("(");
//		sb.append(writeCompleteCount()).append("),");
//		sb.append(exceptionCaughtCount()).append(",");
//		sb.append(handshakedCount()).append(",");
//		sb.append(loginedCount());
//		
//		return sb.toString();
//	}
	
	@Override
	public String toString() {
		return "KGameCommCounter [name=" + name + ", writeRequestedCount="
				+ writeRequestedCount + ", writeCompleteCount="
				+ writeCompleteCount + ", writtenAmountCount="
				+ writtenAmountCount + ", messageReceivedCount="
				+ messageReceivedCount + ", channelConnectedCount="
				+ channelConnectedCount + ", channelDisconnectedCount="
				+ channelDisconnectedCount + ", channelOpenCount="
				+ channelOpenCount + ", channelClosedCount="
				+ channelClosedCount + ", exceptionCaughtCount="
				+ exceptionCaughtCount + ", handshakedCount=" + handshakedCount
				+ ", loginedCount=" + loginedCount + "]";
	}

	///////////////////////////////////////////////////////
	//测试统计
	class WrittenCountAndAmount {
		final AtomicLong count = new AtomicLong();
		final AtomicLong amount = new AtomicLong();
		public long addAndGet(long written){
			count.incrementAndGet();
			return amount.addAndGet(written);
		}
	}
	private static final ConcurrentHashMap<Integer, WrittenCountAndAmount> mid_mapping_writtenAmount=new ConcurrentHashMap<Integer, WrittenCountAndAmount>();
	public void writtenAmountPerMID(int mid,long writtenAmount){
		WrittenCountAndAmount wca = mid_mapping_writtenAmount.get(mid);
		if (wca == null) {
			wca = new WrittenCountAndAmount();
			mid_mapping_writtenAmount.put(mid, wca);
			wca.addAndGet(writtenAmount);
		}else{
			wca.addAndGet(writtenAmount);
		}
	}

	public void printMapping(String fileprefix,boolean clean0) {
		try {
			File dir = new File("monitor");
			if(!dir.exists()){
				dir.mkdir();
			}
			File file = new File("monitor" + File.separator + fileprefix + "_"
					+ DateUtil.getCurrentTimeDefault() + ".csv");
			if (!file.exists()) {
				file.createNewFile();
			}
			PrintWriter pw = new PrintWriter(file);
			try {
				pw.println("MID,WriteCount,WrittenAmount,avg");
				for (Integer mid : mid_mapping_writtenAmount.keySet()) {
					WrittenCountAndAmount wca = mid_mapping_writtenAmount
							.get(mid);
					if (wca != null) {
						pw.println(mid + "," + wca.count.get() + ","
								+ wca.amount.get() + "," + wca.amount.get()
								/ Math.max(wca.count.get(), 1));
					}
				}
			} finally {
				pw.close();
			}
			//清0
			if (clean0) {
				mid_mapping_writtenAmount.clear();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	///////////////////////////////////////////////////////
	
}
