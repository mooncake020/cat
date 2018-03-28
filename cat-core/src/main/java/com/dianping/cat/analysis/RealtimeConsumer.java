package com.dianping.cat.analysis;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import com.alibaba.fastjson.JSON;
import com.dianping.cat.message.spi.internal.DefaultMessageTree;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;
import io.searchbox.indices.CreateIndex;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.unidal.helper.Threads;
import org.unidal.lookup.ContainerHolder;
import org.unidal.lookup.annotation.Inject;

import com.dianping.cat.Cat;
import com.dianping.cat.CatConstants;
import com.dianping.cat.config.server.BlackListManager;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.MessageProducer;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.spi.MessageTree;
import com.dianping.cat.statistic.ServerStatisticManager;

public class RealtimeConsumer extends ContainerHolder implements MessageConsumer, Initializable, LogEnabled {

	@Inject
	private MessageAnalyzerManager m_analyzerManager;

	@Inject
	private ServerStatisticManager m_serverStateManager;

	@Inject
	private BlackListManager m_blackListManager;

	private PeriodManager m_periodManager;

	private long m_black = -1;

	private Logger m_logger;

	public static final long MINUTE = 60 * 1000L;

	public static final long HOUR = 60 * MINUTE;

	@Override
	public void consume(MessageTree tree) {
		String domain = tree.getDomain();
		String ip = tree.getIpAddress();

		if (!m_blackListManager.isBlack(domain, ip)) {
			long timestamp = tree.getMessage().getTimestamp();
			Period period = m_periodManager.findPeriod(timestamp);

			if (period != null) {
				MessageTree messageTree =tree.copy();
//				MessageTree messageTree = new DefaultMessageTree();
//				messageTree.setDomain(tree.getDomain());
//				messageTree.setHostName(tree.getHostName());
//				messageTree.setIpAddress(tree.getIpAddress());
//				messageTree.setMessage(tree.getMessage());
//				messageTree.setMessageId(tree.getMessageId());
//				messageTree.setParentMessageId(tree.getParentMessageId());
//				messageTree.setRootMessageId(tree.getRootMessageId());
//				messageTree.setSessionToken(tree.getSessionToken());
//				messageTree.setThreadGroupName(tree.getThreadGroupName());
//				messageTree.setThreadName(tree.getThreadName());
				if (! "cat".equals(tree.getDomain()))
				new ElasticSearchHelperJedisJackson().push2es(tree);
				//System.err.println(tree.toString());

				period.distribute(tree);
			} else {
				m_serverStateManager.addNetworkTimeError(1);
			}
		} else {
			m_black++;

			if (m_black % CatConstants.SUCCESS_COUNT == 0) {
				Cat.logEvent("Discard", domain);
			}
		}
	}







	public void doCheckpoint() {
		m_logger.info("starting do checkpoint.");
		MessageProducer cat = Cat.getProducer();
		Transaction t = cat.newTransaction("Checkpoint", getClass().getSimpleName());

		try {
			long currentStartTime = getCurrentStartTime();
			Period period = m_periodManager.findPeriod(currentStartTime);

			for (MessageAnalyzer analyzer : period.getAnalzyers()) {
				try {
					analyzer.doCheckpoint(false);
				} catch (Exception e) {
					Cat.logError(e);
				}
			}

			try {
				// wait dump analyzer store completed
				Thread.sleep(10 * 1000);
			} catch (InterruptedException e) {
				// ignore
			}
			t.setStatus(Message.SUCCESS);
		} catch (RuntimeException e) {
			cat.logError(e);
			t.setStatus(e);
		} finally {
			t.complete();
		}
		m_logger.info("end do checkpoint.");
	}

	@Override
	public void enableLogging(Logger logger) {
		m_logger = logger;
	}

	public List<MessageAnalyzer> getCurrentAnalyzer(String name) {
		long currentStartTime = getCurrentStartTime();
		Period period = m_periodManager.findPeriod(currentStartTime);

		if (period != null) {
			return period.getAnalyzer(name);
		} else {
			return null;
		}
	}

	private long getCurrentStartTime() {
		long now = System.currentTimeMillis();
		long time = now - now % HOUR;

		return time;
	}

	public List<MessageAnalyzer> getLastAnalyzer(String name) {
		long lastStartTime = getCurrentStartTime() - HOUR;
		Period period = m_periodManager.findPeriod(lastStartTime);

		return period == null ? null : period.getAnalyzer(name);
	}

	@Override
	public void initialize() throws InitializationException {
		m_periodManager = new PeriodManager(HOUR, m_analyzerManager, m_serverStateManager, m_logger);
		m_periodManager.init();

		Threads.forGroup("cat").start(m_periodManager);
	}

}

class DefaultMessageTreeVO{
	private String detail;
	private MessageTree messageTree;
	private String timestamp;
	private String rootMessageId;

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getRootMessageId() {
		return rootMessageId;
	}

	public void setRootMessageId(String rootMessageId) {
		this.rootMessageId = rootMessageId;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public MessageTree getMessageTree() {
		return messageTree;
	}

	public void setMessageTree(MessageTree messageTree) {
		this.messageTree = messageTree;
	}
}