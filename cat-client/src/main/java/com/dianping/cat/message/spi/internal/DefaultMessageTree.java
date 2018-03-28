package com.dianping.cat.message.spi.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.io.Serializable;
import java.nio.charset.Charset;

import com.dianping.cat.message.Message;
import com.dianping.cat.message.spi.MessageTree;
import com.dianping.cat.message.spi.codec.PlainTextMessageCodec;

public class DefaultMessageTree implements MessageTree,Serializable {

	private ByteBuf m_buf;

	private String m_domain;

	private String m_hostName;

	private String m_ipAddress;

	private Message m_message;

	private String m_messageId;

	private String m_parentMessageId;

	private String m_rootMessageId;

	private String m_sessionToken;

	private String m_threadGroupName;

	private String m_threadId;

	private String m_threadName;

	private boolean m_sample = true;

	public DefaultMessageTree(){

	}
	public MessageTree copy() {
		MessageTree tree = new DefaultMessageTree();

		tree.setDomain(m_domain);
		tree.setHostName(m_hostName);
		tree.setIpAddress(m_ipAddress);
		tree.setMessageId(m_messageId);
		tree.setParentMessageId(m_parentMessageId);
		tree.setRootMessageId(m_rootMessageId);
		tree.setSessionToken(m_sessionToken);
		tree.setThreadGroupName(m_threadGroupName);
		tree.setThreadId(m_threadId);
		tree.setThreadName(m_threadName);
		tree.setMessage(m_message);
		tree.setSample(m_sample);

		return tree;
	}

	public ByteBuf getBuffer() {
		return m_buf;
	}

	public String getDomain() {
		return m_domain;
	}

	public String getHostName() {
		return m_hostName;
	}

	public String getIpAddress() {
		return m_ipAddress;
	}

	public Message getMessage() {
		return m_message;
	}

	
	public String getMessageId() {
		return m_messageId;
	}

	
	public String getParentMessageId() {
		return m_parentMessageId;
	}

	
	public String getRootMessageId() {
		return m_rootMessageId;
	}

	
	public String getSessionToken() {
		return m_sessionToken;
	}

	
	public String getThreadGroupName() {
		return m_threadGroupName;
	}

	
	public String getThreadId() {
		return m_threadId;
	}

	
	public String getThreadName() {
		return m_threadName;
	}

	
	public boolean isSample() {
		return m_sample;
	}

	public void setBuffer(ByteBuf buf) {
		m_buf = buf;
	}

	
	public void setDomain(String domain) {
		m_domain = domain;
	}

	
	public void setHostName(String hostName) {
		m_hostName = hostName;
	}

	
	public void setIpAddress(String ipAddress) {
		m_ipAddress = ipAddress;
	}

	
	public void setMessage(Message message) {
		m_message = message;
	}

	
	public void setMessageId(String messageId) {
		if (messageId != null && messageId.length() > 0) {
			m_messageId = messageId;
		}
	}

	
	public void setParentMessageId(String parentMessageId) {
		if (parentMessageId != null && parentMessageId.length() > 0) {
			m_parentMessageId = parentMessageId;
		}
	}

	
	public void setRootMessageId(String rootMessageId) {
		if (rootMessageId != null && rootMessageId.length() > 0) {
			m_rootMessageId = rootMessageId;
		}
	}

	
	public void setSample(boolean sample) {
		m_sample = sample;
	}

	
	public void setSessionToken(String sessionToken) {
		m_sessionToken = sessionToken;
	}

	
	public void setThreadGroupName(String threadGroupName) {
		m_threadGroupName = threadGroupName;
	}

	
	public void setThreadId(String threadId) {
		m_threadId = threadId;
	}

	
	public void setThreadName(String threadName) {
		m_threadName = threadName;
	}

	
	public String toString() {
		PlainTextMessageCodec codec = new PlainTextMessageCodec();
		ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();

		codec.encode(this, buf);
		buf.readInt(); // get rid of length
		codec.reset();
		return buf.toString(Charset.forName("utf-8"));
	}




}
