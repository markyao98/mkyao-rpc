package MicroRpc.framework.redis.Registry.core;


import MicroRpc.framework.beans.Url;
import MicroRpc.framework.redis.Registry.utils.RedisRegistryConfig;
import MicroRpc.framework.redis.Registry.utils.RedisUtils;
import MicroRpc.framework.tools.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j(topic = "m.RedisRegistry")
public class RedisRegistry {
    protected final static String SERVER_REGISTRY_KEY="SERVER:REGISTRY:";
    protected final static String SERVICE_REGISTRY_KEY="SERVICE:REGISTRY:";
    protected final static String REFRENCE_REGISTRY_KEY="REFRENCE:REGISTRY:";
    protected final static String APPNAME="APPNAME";
    protected final static String URL="URL";
    protected final static String WEIGHT="WEIGHT";

    protected final static String SERVICE_SELECTED_KEY="SERVICE:SELECTED:KEY:";
    protected final static String SERVICE_SELECTED_IDX_KEY="SERVICE:SELECTED:IDX:";

    protected final static String CONNECT_KEY="CONNECT:KEY:";
    protected final static String KILL_CONNECT_KEY="KILLCONNECT:KEY:";
    protected final static String RE_CONNECT_KEY="RECONNECT:KEY:";

    protected final static String EXCPTION_URL="EXCPTION:URL:";
    protected final static String ERROR_URL="ERROR:URL:";
    private final static int MAX_FAIL_TTL=10;

    protected final static String PROVIDERS_KEY="PROVIDERS_KEY:";

    protected final static String URL_MAP_KEY="URLMAP:";

    private final static ObjectMapper objmapper=new ObjectMapper();

    private static RedisRegistryConfig config;

    private static JedisPool jedisPool;

    private final static String HEARTBEAT = "HEARTBEAT";

    private static String thisAppName=null;
    private static Url thisUrl=null;
    private static Set<String>thisProviders=new HashSet<>();

    public static void init(RedisRegistryConfig redisRegistryConfig){
        config=redisRegistryConfig;
        //为jedispool设置基本信息
        JedisPoolConfig jedisPoolConfig=new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(null==config.getMaxtotal()?100:config.getMaxtotal());
        jedisPool=new JedisPool(jedisPoolConfig,
                config.getHost(),
                config.getPort(),
                1000,
                (config.getPassword()==null||config.getPassword().length()==0)? null: config.getPassword()
                ,(null==config.getDb())?0:config.getDb());

        try(Jedis jedis = jedisPool.getResource()) {
            if (!jedis.exists(HEARTBEAT)){
                log.info("【RedisRegistry---初始化注册中心...】");
//                clearAll();
                new Thread(()->{
                    log.info("【RedisRegistry--- redis注册中心活动心跳 ...】");
                    int ttl=8;
                    try {
                        while (true){
                            jedis.setex(HEARTBEAT,ttl,"1");
//                    jedis.expire(HEARTBEAT,ttl);
                            Thread.sleep(10*1000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                },"Global-heartbeat-thread").start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        log.info("【RedisRegistry--- redis服务中心初始化成功. 】");
    }

    public static void registAppName(String appName) {
        if (thisAppName==null){
            thisAppName=appName;
        }
        try (Jedis jedis = jedisPool.getResource()){
            jedis.hset(SERVER_REGISTRY_KEY+appName,APPNAME,appName);
        }
    }


    public static void registUrl(String appName, Url url) {
        if (thisUrl==null){
            thisUrl=new Url(url.getHost(),url.getPort());
        }
        String urls = null;
        try (Jedis jedis = jedisPool.getResource()){
            urls=objmapper.writeValueAsString(url);
            jedis.hset(SERVER_REGISTRY_KEY+appName,URL,urls);
            jedis.set(URL_MAP_KEY+urls,appName);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        String finalUrls = urls;
        new Thread(()->{
            while (true){
                try (Jedis jedis = jedisPool.getResource()){
                    String key = buildHeartBeatKey(appName, finalUrls);
                    jedis.set(key,"1");
                    jedis.expire(key,12);
                    Thread.sleep(10*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        },"heartbeat-thread").start();
    }

    public static String getAppname(String urls){
        try (Jedis jedis = jedisPool.getResource()){
            return jedis.get(URL_MAP_KEY+urls);
        }
    }
    public static String getAppname(String urls,Jedis jedis){
        return jedis.get(URL_MAP_KEY+urls);

//        try (Jedis jedis = jedisPool.getResource()){
//            return jedis.get(URL_MAP_KEY+urls);
//        }
    }
    public static String getUrlstr(Url url){
        String s = null;
        try {
            s = objmapper.writeValueAsString(url);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return s;
    }
    public static String getAppname(Url url){
        try {
            String urlstr=objmapper.writeValueAsString(url);
            return getAppname(urlstr);
        } catch (JsonProcessingException e) {
           throw new RuntimeException(e.getMessage());
        }
    }

    public static String buildHeartBeatKey(String appName,String urls){
        return HEARTBEAT + "-" + appName + "=>" + urls;
    }

    public static void registWeight(String appName,int weight){
        try (Jedis jedis = jedisPool.getResource()){
            jedis.hset(SERVER_REGISTRY_KEY+appName,WEIGHT,weight+"");
        }
    }

    public static int getWeight(String appName){
        try(Jedis jedis = jedisPool.getResource()){
            String ws = jedis.hget(SERVER_REGISTRY_KEY + appName, WEIGHT);
            return Integer.parseInt(ws);
        }
    }
    public static Url getUrl(String appName){
        try(Jedis jedis = jedisPool.getResource()) {
            String urls = jedis.hget(SERVER_REGISTRY_KEY + appName, URL);
            Url url = objmapper.readValue(urls, Url.class);
            return url;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getUrlStr(String appName){
        try (Jedis jedis = jedisPool.getResource()){
            String urls = jedis.hget(SERVER_REGISTRY_KEY + appName, URL);
            return urls;
        }
    }


    public static void registServer0(String appName, String interfaceNAME, Class<?> clazz) throws JsonProcessingException {
        try(Jedis jedis = jedisPool.getResource()) {
            String czs = objmapper.writeValueAsString(clazz);
            jedis.hset(SERVICE_REGISTRY_KEY+appName,interfaceNAME,czs);

            String url = getUrlStr(appName);
            //获取权重
            String wt = jedis.hget(SERVER_REGISTRY_KEY + appName, WEIGHT);
            jedis.hset(PROVIDERS_KEY+interfaceNAME,url,wt);
            thisProviders.add(interfaceNAME);

        }
    }

    public static int getProviderCount(String interfaceNAME){
        try(Jedis jedis = jedisPool.getResource())
        {
            return jedis.hgetAll(PROVIDERS_KEY + interfaceNAME).size();
        }
    }


    public static Map<String,String>getUrlForWeights(String interfaceNAME){
        try ( Jedis jedis = jedisPool.getResource()){
            return jedis.hgetAll(PROVIDERS_KEY+interfaceNAME);
        }
    }

    public static List<String> getUrlStrSeleceds(String interfaceNAME){
        try(Jedis jedis = jedisPool.getResource()) {
            List<String> us = jedis.lrange(SERVICE_SELECTED_KEY + interfaceNAME, 0, -1);
            return us;
        }
    }


    private static String getUrlStrByIdx(String interfaceNAME,int idx){
        try(Jedis jedis = jedisPool.getResource()) {
            List<String> us = jedis.lrange(SERVICE_SELECTED_KEY + interfaceNAME, idx, idx);
            return us.get(0);
        }
    }

    public static Url getUrlByIdx(String interfaceNAME,int idx){
        String us = getUrlStrByIdx(interfaceNAME, idx);
        Url url = null;
        try {
            url = objmapper.readValue(us, Url.class);
            return url;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Class getServiceClass(String appName,String interfaceNAME) throws IOException {
        try(Jedis jedis = jedisPool.getResource()) {
            String czs = jedis.hget(SERVICE_REGISTRY_KEY + appName, interfaceNAME);
            Class cz = objmapper.readValue(czs, Class.class);
            return cz;
        }
    }
    public static void registRefrence(String appName, String refrenceName) {
        try (Jedis jedis = jedisPool.getResource()){
            jedis.sadd(REFRENCE_REGISTRY_KEY+appName,refrenceName);
        }
    }
    public static Set<String> getRefrences(String appName){
        try(Jedis jedis = jedisPool.getResource()) {
            Set<String> rs = jedis.smembers(REFRENCE_REGISTRY_KEY + appName);
            return rs;
        }
    }


    public static long getUrlSize(Jedis jedis, String interfaceName) {
        long llen = jedis.llen(SERVICE_SELECTED_KEY + interfaceName);
        return llen;
    }

    public static Url getUrlByIdx(Jedis jedis, String interfaceName, int idx) {
        String us = getUrlStrByIdx(jedis,interfaceName, idx);
        Url url = null;
        try {
            url = objmapper.readValue(us, Url.class);
            return url;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getUrlStrByIdx(Jedis jedis, String interfaceName, int idx) {
        List<String> us = jedis.lrange(SERVICE_SELECTED_KEY + interfaceName, idx, idx);
        return us.get(0);
    }
//    public static Url getUrlForStr(String url) {
//        if (!StringUtils.hasText(url))
//            return null;
//        Url url0 = null;
//        try {
//            url0 = objmapper.readValue(url, Url.class);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return url0;
//    }
    public static Url getUrlForStr(String url) {
        if (!StringUtils.hasText(url))
            return null;
        Url url0 = null;
        //"{"host":"127.0.0.1","port":17001}"
        int id1=-1;
        int id2=-1;
        int id3=-1;
        int id4=-1;
        for (int i = 0; i < url.length(); i++) {
            if (url.charAt(i)==':' && id1==-1){
                id1=i+2;
            }
            else if (url.charAt(i)==',' &&id2==-1){
                id2=i-1;
            }
            else if (url.charAt(i)==':' &&id3==-1){
                id3=i+1;
            }else if (url.charAt(i)=='}' &&id4==-1){
                id4=i;
            }
        }
        String host=url.substring(id1,id2);
        int port=Integer.parseInt(url.substring(id3,id4));
        url0=new Url(host,port);
        return url0;
    }


    public static void adviceFail(Url url) {
        try ( Jedis jedis = jedisPool.getResource()){
            try {
                jedis.hset(EXCPTION_URL,objmapper.writeValueAsString(url), LocalDateTime.now().toString());
                jedis.expire(EXCPTION_URL,MAX_FAIL_TTL);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }
    public static Map<String,String> getFailMap(){
        try ( Jedis jedis = jedisPool.getResource()){
            return jedis.hgetAll(EXCPTION_URL);
        }
    }

    public static boolean containFails(){
        try ( Jedis jedis = jedisPool.getResource()){
            return jedis.exists(EXCPTION_URL);
        }
    }

    public static void clearAll(){
        log.info("【{}】 服务的善后工作....",thisAppName);
        try ( Jedis jedis = jedisPool.getResource()){
            RedisUtils.delScan(jedis,SERVER_REGISTRY_KEY+thisAppName);
            RedisUtils.delScan(jedis,SERVICE_REGISTRY_KEY+thisAppName);
            String urlstr = getUrlstr(thisUrl);
            RedisUtils.delScan(jedis,URL_MAP_KEY+ urlstr);
            for (String interfacename : thisProviders) {
//                log.info("删除key: {},urls: {}",PROVIDERS_KEY+interfacename,urlstr);
                jedis.hdel(PROVIDERS_KEY+interfacename, urlstr);
            }
//            RedisUtils.delScan(jedis,PROVIDERS_KEY);
            RedisUtils.delScan(jedis,REFRENCE_REGISTRY_KEY+thisAppName);
//            RedisUtils.delScan(jedis,SERVICE_SELECTED_KEY);
//            RedisUtils.delScan(jedis,SERVICE_SELECTED_IDX_KEY);
//            RedisUtils.delScan(jedis,EXCPTION_URL);
//            RedisUtils.delScan(jedis,ERROR_URL);

        }
    }



    public static void delUrlAndService(Url url) {
//        System.out.println("delUrlAndService");
        try ( Jedis jedis = jedisPool.getResource()){
            String urls = objmapper.writeValueAsString(url);
            String appName = jedis.get(URL_MAP_KEY + urls);
            System.out.println("待删除: "+urls +" appname: "+appName);
            jedis.del(SERVER_REGISTRY_KEY+appName);
            jedis.del(SERVICE_REGISTRY_KEY+appName);
            jedis.del(URL_MAP_KEY+urls);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    public static void adviceError(Url url, String interfaceName) {
        try( Jedis jedis = jedisPool.getResource()){
            String us = objmapper.writeValueAsString(url);
            jedis.hset(ERROR_URL, us, LocalDateTime.now().toString());
            jedis.expire(EXCPTION_URL,MAX_FAIL_TTL*6);
            jedis.hdel(PROVIDERS_KEY+interfaceName,us);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public static boolean isError(Url url) {
        try(Jedis jedis = jedisPool.getResource()){
            return jedis.hexists(ERROR_URL,objmapper.writeValueAsString(url));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean isError(String url) {
        try(Jedis jedis = jedisPool.getResource()){
            return jedis.hexists(ERROR_URL,url);
        }
    }

    public static String[] getProviderUrlStrByIntefaceName(String interfaceName) {
        try (Jedis jedis = jedisPool.getResource()){
            Set<String> keySet = jedis.hgetAll(PROVIDERS_KEY + interfaceName).keySet();
            String[] urls=new String[keySet.size()];
            int idx=0;
            for (String us : keySet) {
                urls[idx++]=us;
            }
            return urls;
        }
    }
    public static Url[] getProviderUrlsByIntefaceName(String interfaceName) {
        try (Jedis jedis = jedisPool.getResource()){
            Set<String> keySet = jedis.hgetAll(PROVIDERS_KEY + interfaceName).keySet();
            Url[] urls=new Url[keySet.size()];
            int idx=0;
            for (String us : keySet) {
                try {
                    Url url = objmapper.readValue(us, Url.class);
                    urls[idx++]=url;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return urls;
        }
    }

    public static boolean hasKey(String k){
        try (Jedis jedis=jedisPool.getResource()){
            return jedis.exists(k);
        }
    }

    public static void registConnect(Url targetUrl) {
        if (thisUrl==null || thisAppName==null){
            throw new RuntimeException("本服务初始化失败!");
        }
        try (Jedis jedis=jedisPool.getResource()){
            jedis.sadd(CONNECT_KEY+thisAppName,getAppname(targetUrl));
        }
    }

    public static void killConnect(Url url) {
        if (thisUrl==null || thisAppName==null){
            throw new RuntimeException("本服务初始化失败!");
        }
        try (Jedis jedis=jedisPool.getResource()){
            jedis.srem(CONNECT_KEY+thisAppName,getAppname(url));
        }
    }
    public static void killConnect(String url) {
        if (thisUrl==null || thisAppName==null){
            throw new RuntimeException("本服务初始化失败!");
        }
        try (Jedis jedis=jedisPool.getResource()){
            String appname = getAppname(url, jedis);
            jedis.srem(CONNECT_KEY+thisAppName, appname);
        }
    }

    public static Url[] hasKills() {
        Url[] urls=null;
        if (jedisPool==null){
            return null;
        }
        try (Jedis jedis=jedisPool.getResource()){
            boolean exist = jedis.exists(KILL_CONNECT_KEY + thisAppName).booleanValue();
            if (exist){
                Set<String> smembers = jedis.smembers(KILL_CONNECT_KEY + thisAppName);
                urls=new Url[smembers.size()];
                int i=0;
                for (String appname : smembers) {
                    Url url = getUrl(appname);
                    urls[i++]=url;
                }
            }
        }
        return urls;
    }

    public static void killOk() {
        try (Jedis jedis=jedisPool.getResource()){
            boolean exist = jedis.exists(KILL_CONNECT_KEY + thisAppName).booleanValue();
            if (exist){
                jedis.del(KILL_CONNECT_KEY + thisAppName);
            }
        }
    }

    public static Url[] hasReconnects() {
        Url[] urls=null;
        if (jedisPool==null){
            return null;
        }
        try (Jedis jedis=jedisPool.getResource()){
            boolean exist = jedis.exists(RE_CONNECT_KEY + thisAppName).booleanValue();
            if (exist){
                Set<String> smembers = jedis.smembers(RE_CONNECT_KEY + thisAppName);
                urls=new Url[smembers.size()];
                int i=0;
                for (String appname : smembers) {
                    Url url = getUrl(appname);
                    urls[i++]=url;
                }
            }
        }
        return urls;
    }
    public static void reconnectsOk() {
        try (Jedis jedis=jedisPool.getResource()){
            boolean exist = jedis.exists(RE_CONNECT_KEY + thisAppName).booleanValue();
            if (exist){
                jedis.del(RE_CONNECT_KEY + thisAppName);
            }
        }
    }

    /**
     * 获取需要预热连接的url列表
     * @return
     */
    public static List<Url> getInitConnects() {
        List<Url> initConnects=new ArrayList<>();
        Set<String> initSet=new HashSet<>();
        try (Jedis jedis= jedisPool.getResource()){
            Set<String> refrenceMethods = jedis.smembers(REFRENCE_REGISTRY_KEY + thisAppName);
            for (String refrenceMethod : refrenceMethods) {
                String hkey=PROVIDERS_KEY+refrenceMethod;
                Map<String, String> map = jedis.hgetAll(hkey);
                for (String s : map.keySet()) {
                    initSet.add(s);
                }
            }
            for (String urlStr : initSet) {
                boolean f=isHeartBeat(urlStr,jedis);
                if (f){
                    Url url = getUrlForStr(urlStr);
                    initConnects.add(url);
                }
            }
        }

        return initConnects;
    }

    private static boolean isHeartBeat(String urlStr,Jedis jedis) {
        String appname = getAppname(urlStr);
        String heartBeatKey = buildHeartBeatKey(appname, urlStr);
        return jedis.exists(heartBeatKey).booleanValue();
    }


}
