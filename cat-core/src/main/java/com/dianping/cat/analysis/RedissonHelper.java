package com.dianping.cat.analysis;

import org.codehaus.plexus.logging.Logger;
import org.redisson.RedisClientResult;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.redisson.codec.AvroJacksonCodec;
import org.redisson.config.Config;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @Author jiangxiucai@gmail.com
 * @DATE 2017/8/24 17:09
 */
public class RedissonHelper {

    private static final String es_prop_file = "config/es.properties";
    private static final String es_prop_name_url = "elastic.url";
    private static final String es_user_name = "elastic.user-name";
    private static final String es_user_password = "elastic.password";
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


    private RedissonHelper(){

    }
    public static RedissonClient createRedissonClient(){
        Config config = createConfig();
        return Redisson.create(config);
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

        zsetAdd("test","def");
        Collection<Object> name = zsetRange("test");
        for (Object o :
                name) {
            System.err.println((String )o);

        }
        zsetAdd("test","abc");
        zsetAdd("test","ggg");
        Collection<Object> name2 = zsetRange("test");
        for (Object o :
                name2) {
            System.err.println((String )o);

        }



    }


    public static Config createConfig() {
//        String redisAddress = System.getProperty("redisAddress");
//        if (redisAddress == null) {
//            redisAddress = "127.0.0.1:6379";
//        }
        Config config = new Config();
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
        redissonClient.shutdown();
    }
    public static Collection<Object> zsetRange(String name){
        RedissonClient redissonClient = createRedissonClient();
        Collection<Object> objects = redissonClient.getScoredSortedSet(name).valueRange(0, -1);
        redissonClient.shutdown();
        return objects;
    }
    public static <T> void zsetRemove(String name,T o){

        RedissonClient redissonClient = createRedissonClient();
        RScoredSortedSet<T> scoredSortedSet = redissonClient.getScoredSortedSet(name);
        scoredSortedSet.remove(o);
        scoredSortedSet.expire(10, TimeUnit.SECONDS);
        redissonClient.shutdown();
    }

    public static <T> void set(String key,T value){
        RedissonClient redissonClient = createRedissonClient();
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value);
        bucket.expire(10, TimeUnit.SECONDS);
        redissonClient.shutdown();
    }
    public static Object get(String key){
        RedissonClient redissonClient = createRedissonClient();
        RBucket<Object> bucket = redissonClient.getBucket(key);
        Object o = bucket.get();
        redissonClient.shutdown();
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


}
