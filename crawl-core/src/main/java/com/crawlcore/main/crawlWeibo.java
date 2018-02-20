package com.crawlcore.main;

import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by beckyxu on 2018/2/21.
 */
public class crawlWeibo {

    private static final char[] DIGITS_LOWER = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static final char[] DIGITS_UPPER = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static void main(String args[]) throws Exception {
        String userName = "xxx"; //TODO 输入微博用户名
        String password = "xxx";//TODO 输入微博密码
        login(userName, password);
    }

    public static void login(String userName, String password) throws Exception {
        userName = getEncodeUserName(userName);
        Map<String, String> paramMap = resolveJson(prelogin(userName));
        password = encodePassword(paramMap, password);
        List<NameValuePair> nvps = getLoginList(userName, password, paramMap);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost post = new HttpPost("http://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.19)&_=" +
                System.currentTimeMillis());
        post.setEntity(new UrlEncodedFormEntity(nvps));

        //3.执行get请求并返回结果
        CloseableHttpResponse response = httpclient.execute(post);
        try {
            String res = EntityUtils.toString(response.getEntity());
            System.out.println("login resMsg:" + res);
        } finally {
            response.close();
        }
    }

    /**
     * 构造登陆请求参数
     * @param userName
     * @param password
     * @param paramMap
     * @return
     */
    private static List<NameValuePair> getLoginList(String userName, String password, Map<String, String> paramMap) {
        String servertime = paramMap.get("servertime").toString();
        String nonce = paramMap.get("nonce").toString();
        String rsakv = paramMap.get("rsakv").toString();

        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("entry", "account"));
        nvps.add(new BasicNameValuePair("gateway", "1"));
        nvps.add(new BasicNameValuePair("from", ""));
        nvps.add(new BasicNameValuePair("savestate", "30"));
        nvps.add(new BasicNameValuePair("qrcode_flag", "true"));
        nvps.add(new BasicNameValuePair("useticket", "1"));
        nvps.add(new BasicNameValuePair("pagerefer", "http://my.sina.com.cn/"));
        nvps.add(new BasicNameValuePair("vsnf", "1"));
        nvps.add(new BasicNameValuePair("su", userName));
        nvps.add(new BasicNameValuePair("service", "sso"));
        nvps.add(new BasicNameValuePair("servertime", servertime));
        nvps.add(new BasicNameValuePair("nonce", nonce));
        nvps.add(new BasicNameValuePair("pwencode", "rsa2"));
        nvps.add(new BasicNameValuePair("rsakv", rsakv));
        nvps.add(new BasicNameValuePair("sp", password));
        nvps.add(new BasicNameValuePair("sr", "1366*768"));
        nvps.add(new BasicNameValuePair("encoding", "UTF-8"));
        nvps.add(new BasicNameValuePair("cdult", "3"));
        nvps.add(new BasicNameValuePair("domain", "weibo.com"));
        nvps.add(new BasicNameValuePair("prelt", "154"));
        nvps.add(new BasicNameValuePair("url", "http://weibo.com/ajaxlogin.php?framelogin=1&callback=parent.sinaSSOController.feedBackUrlCallBack"));
        nvps.add(new BasicNameValuePair("domain", "sina.com.cn"));
        nvps.add(new BasicNameValuePair("prelt", "1117"));
        nvps.add(new BasicNameValuePair("returntype", "TEXT"));
        return nvps;
    }

    /**
     * 密码加密
     * @param json
     * @param password
     * @return
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws NoSuchPaddingException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws UnsupportedEncodingException
     */
    public static String encodePassword(Map<String, String> json, String password) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        String message = json.get("servertime") + "\t" + json.get("nonce") + "\n" + password;
        password = rsa(json.get("pubkey"), "10001", message);
        System.out.println("password:" + password);
        return password;
    }

    public static String rsa(String pubkey, String exponentHex, String pwd) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, InvalidKeyException {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(pubkey, 16), new BigInteger(exponentHex, 16));
        //创建公钥
        RSAPublicKey pub = (RSAPublicKey) factory.generatePublic(spec);
        Cipher enc = Cipher.getInstance("RSA");
        enc.init(Cipher.ENCRYPT_MODE, pub);
        byte[] encryptedContentKey = enc.doFinal(pwd.getBytes("UTF-8"));
        return new String(encodeHex(encryptedContentKey));
    }

    /**
     * 解析预登陆后返回参数
     * @param response
     * @return
     */
    public static Map<String, String> resolveJson(String response) {
        JSONObject json_test = JSONObject.fromObject(response);
        Map<String, String> map = new HashMap<String, String>();
        map.put("servertime", json_test.get("servertime").toString());
        map.put("nonce", json_test.get("nonce").toString());
        map.put("rsakv", json_test.get("rsakv").toString());
        map.put("pubkey", json_test.get("pubkey").toString());
        System.out.println("map:" + map.toString());
        return map;
    }

    /**
     * 预登陆
     * @param userName
     * @return
     * @throws Exception
     */
    public static String prelogin(String userName) throws Exception {
        String str = "http://login.sina.com.cn/sso/prelogin.php" + "?" +
                "entry=weibo&callback=sinaSSOController.preloginCallBack&"
                + "rsakt=mod&checkpin=1&client=ssologin.js(v1.4.18)&su=" + userName + "&_=" + System.currentTimeMillis();
        //TODO 可以改成使用连接池
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(str);
        CloseableHttpResponse response = httpclient.execute(httpget);
        String replace = "";
        try {
            HttpEntity entity = response.getEntity();
            String res = EntityUtils.toString(entity);
            System.out.println("res:" + res);
            replace = res.replaceAll("sinaSSOController.preloginCallBack\\((.*)\\)", "$1");
            System.out.println("replace:" + replace);
        } finally {
            response.close();
        }
        return replace;
    }

    /**
     * 用户名编码
     * @param userName
     * @return
     */
    public static String getEncodeUserName(String userName) {
        try {
            userName = Base64.encodeBase64String(URLEncoder.encode(userName, "UTF-8").getBytes()).toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.out.println("userName:" + userName);
        return userName;
    }

    protected static char[] encodeHex(final byte[] data, final char[] toDigits) {
        final int l = data.length;
        final char[] out = new char[l << 1];

        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
        return out;
    }

    public static char[] encodeHex(final byte[] data, final boolean toLowerCase) {
        return encodeHex(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
    }

    public static char[] encodeHex(final byte[] data) {
        return encodeHex(data, true);
    }
}
