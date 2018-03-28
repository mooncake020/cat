package com.dianping.cat.message.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.dianping.cat.message.Event;
import com.dianping.cat.message.ForkedTransaction;
import com.dianping.cat.message.Heartbeat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Metric;
import com.dianping.cat.message.TaggedTransaction;
import com.dianping.cat.message.Trace;
import com.dianping.cat.message.Transaction;

public enum NullMessage implements Transaction, Event, Metric, Trace, Heartbeat, ForkedTransaction, TaggedTransaction {
	TRANSACTION,

	EVENT,

	METRIC,

	TRACE,

	HEARTBEAT;


	public Transaction addChild(Message message) {
		return this;
	}
	public void setChildren(List<Message> children) {

	}

	public void addData(String keyValuePairs) {
	}

	public void addData(String key, Object value) {
	}

	public void bind(String tag, String childMessageId, String title) {
	}

	public void complete() {
	}

	public void fork() {
	}

	public List<Message> getChildren() {
		return Collections.emptyList();
	}

	public Object getData() {
		return null;
	}

	public long getDurationInMicros() {
		return 0;
	}

	public long getDurationInMillis() {
		return 0;
	}

	public String getForkedMessageId() {
		throw new UnsupportedOperationException();
	}

	public String getName() {
		throw new UnsupportedOperationException();
	}

	public String getParentMessageId() {
		return null;
	}

	public String getRootMessageId() {
		return null;
	}

	public String getStatus() {
		throw new UnsupportedOperationException();
	}

	public String getTag() {
		throw new UnsupportedOperationException();
	}

	public long getTimestamp() {
		throw new UnsupportedOperationException();
	}

	public String getType() {
		throw new UnsupportedOperationException();
	}

	public boolean hasChildren() {
		return false;
	}

	public boolean isCompleted() {
		return true;
	}

	public boolean isStandalone() {
		return true;
	}

	public boolean isSuccess() {
		return true;
	}

	public void setStatus(String status) {
	}

	public void setStatus(Throwable e) {
	}

	public void start() {
	}
}
