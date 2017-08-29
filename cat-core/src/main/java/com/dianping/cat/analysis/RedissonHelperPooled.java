package com.dianping.cat.analysis;

import org.codehaus.plexus.logging.Logger;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.KryoCodec;
import org.redisson.codec.SerializationCodec;
import org.redisson.config.Config;

import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @Author jiangxiucai@gmail.com
 * @DATE 2017/8/24 17:09
 */
public class RedissonHelperPooled {

    private static final String es_prop_file = "config/es.properties";
    private static final String es_prop_name_url = "elastic.url";
    private static final String es_user_name = "elastic.user-name";
    private static final String es_user_password = "elastic.password";

    private static final ArrayBlockingQueue<RedissonClient> pool = new ArrayBlockingQueue<RedissonClient>(30);
    private Logger m_logger;
    //private RedissonClient redissonClient;
//    static {
//        Config config = new Config();
////        config.setUseLinuxNativeEpoll(true);
//        /*
//        Properties properties = loadESProperties();
//        config.useClusterServers()
//                //可以用"rediss://"来启用SSL连接
//                .addNodeAddress(properties.getProperty("redis-server"));*/
//        config.useSingleServer()
//                //可以用"rediss://"来启用SSL连接
//                .setAddress("redis://172.17.80.10:6379");
//
//        //config.setCodec(new AvroJacksonCodec()); 官方文档没有关于如何指定编码方式的例子 ，Jackson JSON 编码 默认编码
//
//        redissonClient = Redisson.create(config);
//    }
    static {
        initPools();
    }


    private RedissonHelperPooled(){

    }
    public static RedissonClient createRedissonClient(){
        RedissonClient take = null;
        try {
            take = pool.take();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return take;
    }
    public static void returnRedissonClient(RedissonClient redissonClient){
        try {
            pool.put(redissonClient);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
//        Config config = new Config();
//        //config.setUseLinuxNativeEpoll(true);
//        config.useSingleServer()
//                //可以用"rediss://"来启用SSL连接
//                .setAddress("redis://172.17.80.10:6379");
//        //config.setCodec(new AvroJacksonCodec()); 官方文档没有关于如何指定编码方式的例子 ，Jackson JSON 编码 默认编码
//
//        RedissonClient client = Redisson.create(config);
//        client.getScoredSortedSet("name").add(1d,"abc");
        System.err.println(pool.size());
        zsetAdd("test","def");
        Collection<Object> name = zsetRange("test");
        for (Object o :
                name) {
            System.err.println((String )o);

        }

        for (int i=0;i<100;i++){
            System.err.println(pool.size());
            zsetAdd("test","abc"+i);
        }
        System.err.println(pool.size());
        zsetAdd("test","abc");

        System.err.println(pool.size());
        zsetAdd("test","ggg");

        System.err.println(pool.size());
        Collection<Object> name2 = zsetRange("test");
        for (Object o :
                name2) {
            System.err.println((String )o);

        }

        shutdownConnections();



    }


    public static Config createConfig() {
//        String redisAddress = System.getProperty("redisAddress");
//        if (redisAddress == null) {
//            redisAddress = "127.0.0.1:6379";
//        }
        Config config = new Config();
        config.setCodec(new KryoCodec());
//        config.setCodec(new MsgPackJacksonCodec());
//        config.useSentinelServers().setMasterName("mymaster").addSentinelAddress("127.0.0.1:26379", "127.0.0.1:26389");
//        config.useClusterServers().addNodeAddress("127.0.0.1:7004", "127.0.0.1:7001", "127.0.0.1:7000");
        config.useSingleServer()
                .setAddress("redis://172.17.80.10:6379");
//        .setPassword("mypass1");
//        config.useMasterSlaveConnection()
//        .setMasterAddress("127.0.0.1:6379")
//        .addSlaveAddress("127.0.0.1:6399")
//        .addSlaveAddress("127.0.0.1:6389");
        return config;
    }
    public  static <T> void zsetAdd(String name,T value){
        RedissonClient redissonClient = createRedissonClient();
        RScoredSortedSet<T> scoredSortedSet = redissonClient.getScoredSortedSet(name);
        scoredSortedSet.add(1d,value);
        scoredSortedSet.expire(10, TimeUnit.SECONDS);
        returnRedissonClient(redissonClient);
    }
    public static Collection<Object> zsetRange(String name){
        RedissonClient redissonClient = createRedissonClient();
        Collection<Object> objects = redissonClient.getScoredSortedSet(name).valueRange(0, -1);
        returnRedissonClient(redissonClient);
        return objects;
    }
    public static <T> void zsetRemove(String name,T o){

        RedissonClient redissonClient = createRedissonClient();
        RScoredSortedSet<T> scoredSortedSet = redissonClient.getScoredSortedSet(name);
        scoredSortedSet.remove(o);
        scoredSortedSet.expire(10, TimeUnit.SECONDS);
        returnRedissonClient(redissonClient);
    }

    public static <T> void set(String key,T value){
        RedissonClient redissonClient = createRedissonClient();
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value);
        bucket.expire(10, TimeUnit.SECONDS);
        returnRedissonClient(redissonClient);
    }
    public static Object get(String key){
        RedissonClient redissonClient = createRedissonClient();
        RBucket<Object> bucket = redissonClient.getBucket(key);
        Object o = bucket.get();
        returnRedissonClient(redissonClient);
        return o;
    }

    private static Properties loadESProperties() {
        Properties pps = new Properties();

        String appName = null;
        InputStream in = null;
        try {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(es_prop_file);


            if (in == null) {
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

    public static RedissonClient initRedissonClient(){
        Config config = createConfig();
        return Redisson.create(config);
    }
    public static void initPools(){
        Long start = System.currentTimeMillis();
        for (int i=0;i<30;i++){
            System.err.println("create client NO." + i );
            RedissonClient redissonClient = initRedissonClient();
            try {
                pool.put(redissonClient);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.err.println("create client NO." + i + "done" );
        }
        Long end = System.currentTimeMillis();
        Long cost = end - start;
        System.err.println("初始化redis连接池消耗：\t" + cost + "毫秒");
    }

    public static void shutdownConnections(){

        Long start = System.currentTimeMillis();
        while (pool.size() > 0 ){
            System.err.println("shutdown client ..." + pool.size() + "remain" );
            RedissonClient take = null;
            try {
                take = pool.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            take.shutdown();

            System.err.println("shutdown client done" );
        }

        Long end = System.currentTimeMillis();
        Long cost = end - start;
        System.err.println("关闭redis连接池消耗：\t" + cost + "毫秒");

    }
}
