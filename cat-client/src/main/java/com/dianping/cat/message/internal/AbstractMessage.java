package com.dianping.cat.message.internal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.nio.charset.Charset;

import com.dianping.cat.message.Message;
import com.dianping.cat.message.spi.codec.PlainTextMessageCodec;

import static org.aspectj.lang.reflect.DeclareAnnotation.Kind.Type;


public abstract class AbstractMessage implements Message {
	@JsonProperty("m_type")
	private String m_type;
	@JsonProperty("m_name")
	private String m_name;
	@JsonProperty("m_status")
	private String m_status = "unset";

	@JsonProperty("m_timestampInMillis")
	private long m_timestampInMillis;
	@JsonProperty("data")
	private CharSequence m_data;

	private boolean m_completed;

	public AbstractMessage(String type, String name) {
		m_type = String.valueOf(type);
		m_name = String.valueOf(name);
		m_timestampInMillis = MilliSecondTimer.currentTimeMillis();
	}
	public AbstractMessage(){

	}

	
	public void addData(String keyValuePairs) {
		if (m_data == null) {
			m_data = keyValuePairs;
		} else if (m_data instanceof StringBuilder) {
			((StringBuilder) m_data).append('&').append(keyValuePairs);
		} else {
			StringBuilder sb = new StringBuilder(m_data.length() + keyValuePairs.length() + 16);

			sb.append(m_data).append('&');
			sb.append(keyValuePairs);
			m_data = sb;
		}
	}

	
	public void addData(String key, Object value) {
		if (m_data instanceof StringBuilder) {
			((StringBuilder) m_data).append('&').append(key).append('=').append(value);
		} else {
			String str = String.valueOf(value);
			int old = m_data == null ? 0 : m_data.length();
			StringBuilder sb = new StringBuilder(old + key.length() + str.length() + 16);

			if (m_data != null) {
				sb.append(m_data).append('&');
			}

			sb.append(key).append('=').append(str);
			m_data = sb;
		}
	}

	
	public CharSequence getData() {
		if (m_data == null) {
			return "";
		} else {
			return m_data;
		}
	}

	
	public String getName() {
		return m_name;
	}

	
	public String getStatus() {
		return m_status;
	}

	
	public long getTimestamp() {
		return m_timestampInMillis;
	}

	
	public String getType() {
		return m_type;
	}

	
	public boolean isCompleted() {
		return m_completed;
	}

	

	@JsonProperty("success")
	@JsonIgnore
	public boolean isSuccess() {
		return Message.SUCCESS.equals(m_status);
	}
	public void setSuccess(boolean success){}

	public void setCompleted(boolean completed) {
		m_completed = completed;
	}

	public void setName(String name) {
		m_name = name;
	}

	
	public void setStatus(String status) {
		m_status = status;
	}

	
	public void setStatus(Throwable e) {
		m_status = e.getClass().getName();
	}

	public void setTimestamp(long timestamp) {
		m_timestampInMillis = timestamp;
	}

	public void setType(String type) {
		m_type = type;
	}

	
	public String toString() {
		PlainTextMessageCodec codec = new PlainTextMessageCodec();
		ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();

		codec.encodeMessage(this, buf);
		codec.reset();
		return buf.toString(Charset.forName("utf-8"));
	}
}
