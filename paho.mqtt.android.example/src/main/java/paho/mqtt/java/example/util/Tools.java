package paho.mqtt.java.example.util;

import android.util.Base64;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by alvin on 17-3-29.
 */
public class Tools {

    /**
     * @param text      要签名的文本
     * @param secretKey 阿里云MQ secretKey
     * @return 加密后的字符串
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     */
    public static String macSignature(String text,
                                      String secretKey) throws InvalidKeyException, NoSuchAlgorithmException {
        Charset charset = Charset.forName("UTF-8");
        String algorithm = "HmacSHA1";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(secretKey.getBytes(charset), algorithm));
        byte[] bytes = mac.doFinal(text.getBytes(charset));
        return new String(Base64.encode(bytes, Base64.NO_WRAP), charset);
    }
}
