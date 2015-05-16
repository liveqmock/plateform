package com.koala.game.frontend;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.koala.game.KGameMessage;
import com.koala.game.exception.KGameServerException;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimerTask;
import com.koala.game.util.KGameExcelFile;
import com.koala.game.util.KGameExcelTable;
import com.koala.game.util.KGameExcelTable.KGameExcelRow;
import com.koala.game.util.KGameExcelWriter;

public class KGameNoticeManager {

	private static final String _NOTICE_PATH = "./res/config/notices.xls";
	private static final String _SHEET_NAME = "登录公告";
	private static final int _HEADER_INDEX = 3;
	
	private static final Map<Integer, KGameNotice> _noticeMap = new HashMap<Integer, KGameNotice>();
	private static final List<String> _titles = new ArrayList<String>();
	private static final AtomicInteger _ID_GENERATOR = new AtomicInteger();
	private static final AtomicBoolean _MODIFY = new AtomicBoolean();
	
	private static void writeSingleNotice(KGameMessage msg, KGameNotice notice) {
		msg.writeByte(notice.getNoticeType());
		msg.writeByte(notice.getStatus());
		msg.writeUtf8String(notice.getTitle());
		msg.writeUtf8String(notice.getContent());
	}
	
	static void saveNotice() {
		KGameExcelWriter writer = new KGameExcelWriter(_NOTICE_PATH);
		writer.setTitleIndex(_SHEET_NAME, _HEADER_INDEX - 1);
		writer.addColTitleForSheet(_SHEET_NAME, _titles);
		List<Object> list;
		KGameNotice notice;
		for(Iterator<Map.Entry<Integer, KGameNotice>> itr = _noticeMap.entrySet().iterator(); itr.hasNext();) {
			notice = itr.next().getValue();
			list = new ArrayList<Object>();
			list.add(notice.id);
			list.add(notice.getNoticeType());
			list.add(notice.getStatus());
			list.add(notice.getTitle());
			list.add(notice.getContent());
			list.add(KGameNotice._SDF.format(new Date(notice.getStartTime())));
			list.add(KGameNotice._SDF.format(new Date(notice.getEndTime())));
			writer.putDataToSheet(_SHEET_NAME, list);
		}
		try {
			writer.output();
		} catch (Exception e) {
			e.printStackTrace();
			KGameNoticeManager._MODIFY.compareAndSet(false, true);
		}
	}
	
	public static void load() throws Exception {
		File file = new File(_NOTICE_PATH);
		if (file.exists()) {
			KGameExcelFile excelFile = new KGameExcelFile(_NOTICE_PATH);
			KGameExcelTable table = excelFile.getTable(_SHEET_NAME, _HEADER_INDEX);
			KGameExcelRow[] allRows = table.getAllDataRows();
			_titles.addAll(table.getHeaderNames());
			KGameExcelRow row;
			KGameNotice notice;
			int maxId = 0;
			for (int i = 0; i < allRows.length; i++) {
				row = allRows[i];
				notice = new KGameNotice(row);
				_noticeMap.put(notice.id, notice);
				if(maxId < notice.id) {
					maxId = notice.id;
				}
			}
			_ID_GENERATOR.set(maxId);
		}
		KGameFrontend.getInstance().getTimer().newTimeSignal(new KGameNoticeWatcher(), 1, TimeUnit.MINUTES);
	}
	
	public static void writeNoticesToMsgForClient(KGameMessage msg) {
		int writerIndex = msg.writerIndex();
		int count = 0;
		KGameNotice notice;
		long currentTime = System.currentTimeMillis();
		msg.writeByte(_noticeMap.size());
		for (Iterator<Map.Entry<Integer, KGameNotice>> itr = _noticeMap.entrySet().iterator(); itr.hasNext();) {
			notice = itr.next().getValue();
			if (notice.getStartTime() > currentTime || notice.getEndTime() < currentTime) {
				continue;
			}
			writeSingleNotice(msg, notice);
			count++;
		}
		msg.setByte(writerIndex, count);
	}
	
	public static void writeNoticeToMsgForGM(KGameMessage msg) {
		msg.writeByte(_noticeMap.size());
		KGameNotice notice;
		for (Iterator<Map.Entry<Integer, KGameNotice>> itr = _noticeMap.entrySet().iterator(); itr.hasNext();) {
			notice = itr.next().getValue();
			msg.writeInt(notice.id);
			writeSingleNotice(msg, notice);
			msg.writeLong(notice._startTime);
			msg.writeLong(notice._endTime);
		}
	}
	
	public static boolean updateNoticeList(KGameMessage msg) {
		int noticeId;
		int size = msg.readByte();
		if (size > 0) {
			KGameNotice notice;
			for (int i = 0; i < size; i++) {
				noticeId = msg.readInt();
				byte noticeType = msg.readByte();
				byte status = msg.readByte();
				String title = msg.readUtf8String();
				String content = msg.readUtf8String();
				long startTime = msg.readLong();
				long endTime = msg.readLong();
				if (noticeId > 0) {
					notice = _noticeMap.get(noticeId);
					if (notice != null) {
						notice.update(noticeType, status, title, content, startTime, endTime);
					} else {
						return false;
					}
				} else {
					notice = new KGameNotice(noticeType, status, title, content, startTime, endTime);
					_noticeMap.put(notice.id, notice);
				}
			}
			_MODIFY.compareAndSet(false, true);
		}
		int delSize = msg.readByte();
		if (delSize > 0) {
			for (int i = 0; i < delSize; i++) {
				noticeId = msg.readInt();
				_noticeMap.remove(noticeId);
			}

			_MODIFY.compareAndSet(false, true);
		}
		return true;
	}
	
	public static class KGameNotice {
		private static final SimpleDateFormat _SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		public final int id;
		private String _title;
		private byte _noticeType;
		private byte _status;
		private String _content;
		private long _startTime;
		private long _endTime;
		
		public KGameNotice(byte pNoticeType, byte pStatus, String pTitle, String pContent, long pStartTime, long pEndTime) {
			this.id = _ID_GENERATOR.incrementAndGet();
			this.update(pNoticeType, pStatus, pTitle, pContent, pStartTime, pEndTime);
		}
		
		public KGameNotice(KGameExcelRow row) throws Exception {
			this.id = row.getInt("id");
			this._noticeType = row.getByte("noticeType");
			this._status = row.getByte("status");
			this._title = row.getData("title");
			this._content = row.getData("content");
			this._startTime = _SDF.parse(row.getData("startTimeStr")).getTime();
			this._endTime = _SDF.parse(row.getData("endTimeStr")).getTime();
		}
		
		void update(byte pNoticeType, byte pStatus, String pTitle, String pContent, long pStartTime, long pEndTime) {
			this._title = pTitle;
			this._content = pContent;
			this._noticeType = pNoticeType;
			this._status = pStatus;
			this._startTime = pStartTime;
			this._endTime = pEndTime;
		}

		public String getTitle() {
			return _title;
		}

		public byte getNoticeType() {
			return _noticeType;
		}

		public byte getStatus() {
			return _status;
		}

		public String getContent() {
			return _content;
		}

		public long getStartTime() {
			return _startTime;
		}

		public long getEndTime() {
			return _endTime;
		}
		
	}
	
	private static class KGameNoticeWatcher implements KGameTimerTask {

		@Override
		public String getName() {
			return "KGameNoticeWatcher";
		}

		@Override
		public Object onTimeSignal(KGameTimeSignal timeSignal) throws KGameServerException {
			if(KGameNoticeManager._MODIFY.compareAndSet(true, false)) {
				KGameNoticeManager.saveNotice();
			}
			timeSignal.getTimer().newTimeSignal(this, 1, TimeUnit.MINUTES);
			return "SUCCESS";
		}

		@Override
		public void done(KGameTimeSignal timeSignal) {
			
		}

		@Override
		public void rejected(RejectedExecutionException e) {
			
		}
		
	}
}
