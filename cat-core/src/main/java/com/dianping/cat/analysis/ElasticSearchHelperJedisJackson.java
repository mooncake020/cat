package com.dianping.cat.analysis;

import com.alibaba.fastjson.JSON;
import com.dianping.cat.analysis.DefaultMessageTreeVO;
import com.dianping.cat.analysis.JedisHelper;
import com.dianping.cat.configuration.client.entity.ClientConfig;
import com.dianping.cat.message.spi.MessageTree;
import com.dianping.cat.message.spi.internal.DefaultMessageTree;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import redis.clients.jedis.Jedis;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author jiangxiucai@gmail.com
 * @DATE 2017/7/21 11:43
 */
public class ElasticSearchHelperJedisJackson implements LogEnabled {
    private static final String es_prop_file = "config/es.properties";
    private static final String es_prop_name_url = "elastic.url";
    private static final String es_user_name = "elastic.user-name";
    private static final String es_user_password = "elastic.password";
    private Logger m_logger;
    private static final ArrayBlockingQueue<JestClient> pool = new ArrayBlockingQueue<JestClient>(30);

    private ClientConfig m_config;

    static {
        initPools();
    }
    public static JestClient getJestClient(){
        JestClient take = null;
        try {
            take = pool.take();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return take;
    }

    public static void returnJestClient(JestClient jestClient){
        try {
            pool.put(jestClient);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void initPools(){
        Long start = System.currentTimeMillis();
        for (int i=0;i<30;i++){
            System.err.println("create client NO." + i );
            JestClient jestClient = initJestClient();
            try {
                pool.put(jestClient);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.err.println("create client NO." + i + "done" );
        }
        Long end = System.currentTimeMillis();
        Long cost = end - start;
        System.err.println("初始化es连接池消耗：\t" + cost + "毫秒");
    }


    public void enableLogging(Logger logger) {
        m_logger = logger;
    }

    private String[] splitReg(String input){
        String reg = "([\\w]+)\\|\\|(.*)";
        Matcher matcher = Pattern.compile(reg).matcher(input);
        if (matcher.find()){
            String group = matcher.group();
            String group0 = matcher.group(1);
            String group1 = matcher.group(2);/*
            System.err.println(group);
            System.err.println(group0);
            System.err.println(group1);
            System.err.println(group1.equals(""));*/
            return new String[]{group0,group1};
        }
        return null;
    }
    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public void push2es(MessageTree tree) {
        String type = tree.getMessage().getType();
        String name = tree.getMessage().getName();
        Properties properties = loadESProperties();
        Boolean flag = true;
        if ("compose".equals(properties.getProperty("elastic.ignore-selector"))){
            String compList = properties.getProperty("elastic.ignored-type-name-compose-list");
            if (StringUtils.isNotEmpty(compList)){

                String[] arr = compList.split(",");
                for (int i = 0; i < arr.length; i++) {
                    String s = arr[i];
                    if (StringUtils.isNotEmpty(s)){
                        String[] split = splitReg(s);
                        if (split[0].equals(type) && split[1].equals(name)){
                            flag = false;
                            break;
                        }

                    }
                }
            }
        }

        if (flag){
//            DefaultMessageTreeVO vo = getDefaultMessageTreeVO(tree);
//
//            String treeStr = JSON.toJSONString(vo);
//            System.err.println("===========================tree consume====================================="  );
//            System.err.println(treeStr);
//            System.err.println(tree.getMessage());
//            System.err.println(tree.getRootMessageId());

            Long start = System.currentTimeMillis();
            try {
                //client.execute(new CreateIndex.Builder("test_idx").build());
//
//                Index index = new Index.Builder(vo).index(tree.getDomain()).type("type_1").build();
//                client.execute(index);
                distribute(tree);

            } catch (Exception e) {
                e.printStackTrace();
            }

            Long end = System.currentTimeMillis();

            Long cost = end - start;
            System.err.println("消费消息消耗：\t" + cost + "毫秒");
        }
    }

    private void distribute(MessageTree tree){

        DefaultMessageTreeVO vo = getDefaultMessageTreeVO(tree);

        String treeStr = JSON.toJSONString(vo);
        //DefaultMessageTreeVO vo1 = JSON.parseObject(treeStr, DefaultMessageTreeVO.class);
        //System.err.println(vo1.getDetail());
        System.err.println("===========================tree consume====================================="  );
        System.err.println(treeStr);
        System.err.println(tree.getMessage());
        String rootMessageId = tree.getRootMessageId();
        System.err.println(rootMessageId);
        JestClient client = getJestClient();
        Long start = System.currentTimeMillis();
        Jedis jedis = JedisHelper.getInstance().getResource();
        try {
            //client.execute(new CreateIndex.Builder("test_idx").build());
            ObjectMapper mapper = new ObjectMapper();

            if (isEmpty(tree.getParentMessageId()) && isEmpty(rootMessageId)){
                String messageId = tree.getMessageId();
                String messageIdKey = "messageId" + messageId;
                String messageCacheKey = "messageCache" + messageId;//此处他自己的id就应该是service层的rootId
                Boolean exists = jedis.exists(messageIdKey);
                if (!exists){
                    //此处记录这条msg的id，并记录这条消息所在的domain，方便远程调用获取请求发起方所在的domain。
                    jedis.set(messageIdKey,tree.getDomain());
                    jedis.expire(messageIdKey,10);
                    //TODO 去查看是否存在没有发送到请求发起方的远程调用消息
                    Set<String> objects = jedis.zrange(messageCacheKey, 0, -1);
                    //Collection<Object> objects = RedissonHelperPooled.zsetRange(messageCacheKey);
                    for (String var: objects
                            ) {
                        DefaultMessageTree defaultMessageTree = mapper.readValue(var,DefaultMessageTree.class);
                        DefaultMessageTreeVO defaultMessageTreeVO = getDefaultMessageTreeVO(defaultMessageTree);
                        Index indexCurr = new Index.Builder(defaultMessageTreeVO).index(tree.getDomain()).id(defaultMessageTreeVO.getMessageTree().getMessageId()).type("type_1").build();//把缓存的消息写到调用方索引中去
                        client.execute(indexCurr);
                        jedis.zrem(messageCacheKey,var);
                        jedis.expire(messageCacheKey,10);
                        //TODO 此处应该不需要重新设置超时时间，所包含的对象都会被移除，新加入时会重新设置超时时间 jedis.expire()

                    }

                }
                Index index = new Index.Builder(vo).index(tree.getDomain()).id(vo.getMessageTree().getMessageId()).type("type_1").build();//把本条消息写到索引中去
                client.execute(index);
            }else {
                String messageIdKey = "messageId" + rootMessageId;
                String messageCacheKey = "messageCache" + rootMessageId;
                Boolean exists = jedis.exists(messageIdKey);
                if (exists){
                    String s = jedis.get(messageIdKey);
                    //如果发起方的消息先被传输至cat，不需要缓存这条消息，直接推送至cat调用发起方的索引，并且推送至它本身的索引
                    Index index = new Index.Builder(vo).index(tree.getDomain()).id(vo.getMessageTree().getMessageId()).type("type_1").build();
                    Index indexCaller = new Index.Builder(vo).index(s).id(vo.getMessageTree().getMessageId()).type("type_1").build();
                    client.execute(index);
                    client.execute(indexCaller);


                }else {
                    //缓存此条调用记录，待调用方的日志到达后拉取此记录
                    String s = mapper.writeValueAsString(tree.copy());
                    jedis.zadd(messageCacheKey,0d, s);
                    jedis.expire(messageCacheKey,10);
                    //推送此记录到service自己的索引
                    Index index = new Index.Builder(vo).index(tree.getDomain()).id(vo.getMessageTree().getMessageId()).type("type_1").build();
                    client.execute(index);
                }


            }
//TODO FIXME 此处应该不需要推送了
//            Index index = new Index.Builder(vo).index(tree.getDomain()).type("type_1").build();
//            client.execute(index);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (null != jedis)
                jedis.close();
        }
        Long end = System.currentTimeMillis();
        Long cost = end - start;

        System.err.println("推送至es消耗：\t" + cost + "毫秒");
        returnJestClient(client);
    }

    private boolean isEmpty(String str){
        return "null".equals(str) || StringUtils.isEmpty(str);
    }

    private DefaultMessageTreeVO getDefaultMessageTreeVO(MessageTree tree) {
        DefaultMessageTreeVO vo = new DefaultMessageTreeVO();
        vo.setMessageTree(tree.copy());
        vo.setDetail(tree.toString());
        vo.setRootMessageId(isEmpty(tree.getRootMessageId()) ? tree.getMessageId() : tree.getRootMessageId());
        vo.setTimestamp(format.format(new Date(tree.getMessage().getTimestamp())));
        return vo;
    }



    private static JestClient initJestClient() {
        JestClientFactory factory = new JestClientFactory();
        Properties properties = loadESProperties();
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder((String) properties.get(es_prop_name_url))
                .multiThreaded(true)
                .defaultCredentials((String) properties.get(es_user_name), (String) properties.get(es_user_password))

                //Per default this implementation will create no more than 2 concurrent connections per given route
                //.defaultMaxTotalConnectionPerRoute(<YOUR_DESIRED_LEVEL_OF_CONCURRENCY_PER_ROUTE>)
                // and no more 20 connections in total
                //.maxTotalConnection(<YOUR_DESIRED_LEVEL_OF_CONCURRENCY_TOTAL>)
                .build());
        return factory.getObject();
    }

    private static Properties loadESProperties() {
        Properties pps = new Properties();

        String appName = null;
        InputStream in = null;
        try {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(es_prop_file);


            if (in == null) {
                in = ElasticSearchHelperJedisJackson.class.getClassLoader().getResourceAsStream(es_prop_file);
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
