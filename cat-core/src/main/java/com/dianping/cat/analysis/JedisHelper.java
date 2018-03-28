package com.dianping.cat.analysis;

import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisHelper
{
  private static final JedisPool pool;
  private static final String es_prop_file = "config/es.properties";
  
  static
  {
    String redisAddr = loadESProperties().getProperty("redis-server-addr");
    String redisPort = loadESProperties().getProperty("redis-server-port");
    pool = new JedisPool(new JedisPoolConfig(), redisAddr, Integer.parseInt(redisPort));
  }
  
  public static JedisPool getInstance()
  {
    return pool;
  }
  
  public static void main(String[] args)
  {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost");
    Jedis jedis = null;
    try
    {
      jedis = pool.getResource();
      
      jedis.set("foo", "bar");
      String foobar = jedis.get("foo");
      jedis.zadd("sose", 0.0D, "car");
      jedis.zadd("sose", 0.0D, "bike");
      Set localSet = jedis.zrange("sose", 0L, -1L);
    }
    finally
    {
      if (jedis != null) {
        jedis.close();
      }
    }
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
