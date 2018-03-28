package com.dianping.cat.analysis;

import com.alibaba.fastjson.JSON;
import com.dianping.cat.Cat;
import com.dianping.cat.configuration.client.entity.ClientConfig;
import com.dianping.cat.message.internal.MessageId;
import com.dianping.cat.message.spi.MessageTree;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.InputStream;
import java.util.Properties;

/**
 * @Author jiangxiucai@gmail.com
 * @DATE 2017/7/21 11:43
 */
public class ElasticSearchHelper implements LogEnabled {
    private static final String es_prop_file = "config/es.properties";
    private static final String es_prop_name_url = "elastic.url";

    private Logger m_logger;

    private ClientConfig m_config;


    public void enableLogging(Logger logger) {
        m_logger = logger;
    }

    public void push2es(MessageTree tree, MessageTree messageTree) {
        DefaultMessageTreeVO vo = new DefaultMessageTreeVO();
        vo.setMessageTree(messageTree);
        vo.setDetail(tree.toString());
        String treeStr = JSON.toJSONString(vo);
        System.err.println("===========================tree consume====================================="  );
        System.err.println(treeStr);
				System.err.println(tree.getMessage());
				System.err.println(tree.getRootMessageId());
        //int index = MessageId.parse(messageId).getIndex();
        JestClient client = getJestClient();
        try {
            //client.execute(new CreateIndex.Builder("test_idx").build());
            Index index = new Index.Builder(vo).index(tree.getDomain()).type("type_1").id(tree.getMessageId()).build();
            client.execute(index);
        } catch (Exception e) {
            e.printStackTrace();
        }
        client.shutdownClient();
    }
    private JestClient getJestClient() {
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder((String) loadESProperties().get(es_prop_name_url))
                .multiThreaded(true)
                //Per default this implementation will create no more than 2 concurrent connections per given route
                //.defaultMaxTotalConnectionPerRoute(<YOUR_DESIRED_LEVEL_OF_CONCURRENCY_PER_ROUTE>)
                // and no more 20 connections in total
                //.maxTotalConnection(<YOUR_DESIRED_LEVEL_OF_CONCURRENCY_TOTAL>)
                .build());
        return factory.getObject();
    }

    private Properties loadESProperties() {
        Properties pps = new Properties();

        String appName = null;
        InputStream in = null;
        try {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(es_prop_file);


            if (in == null) {
                m_logger.info(String.format("Can't find file es.properties,try another way:"));
                in = ElasticSearchHelper.class.getClassLoader().getResourceAsStream(es_prop_file);
            }

            pps.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
        return pps;
    }
}
