package paho.mqtt.java.example.config;

public class Config {
    public static final String INSTANCE_ID = "mqtt-cn-zpr39b17203";
    public static final String END_POINT = "mqtt-cn-zpr39b17203.mqtt.aliyuncs.com";
    public static final String HOST = "tcp://mqtt-cn-zpr39b17203.mqtt.aliyuncs.com";
    public static final String PORT = "1883";
    public static final String GROUP_ID = "GID_gankao-mdm-group";
    /**
     * 账号 accesskey，从账号系统控制台获取
     * 阿里云账号AccessKey拥有所有API的访问权限，建议您使用RAM用户进行API访问或日常运维。
     * 强烈建议不要把AccessKey ID和AccessKey Secret保存到工程代码里，否则可能导致AccessKey泄露，威胁您账号下所有资源的安全。
     * 本示例以把AccessKey ID和AccessKey Secret保存在环境变量为例说明。运行本代码示例之前，请先配置环境变量MQTT_AK_ENV和MQTT_SK_ENV
     * 例如：export MQTT_AK_ENV=<access_key_id>
     * export MQTT_SK_ENV=<access_key_secret>
     * 需要将<access_key_id>替换为已准备好的AccessKey ID，<access_key_secret>替换为AccessKey Secret。
     */
    public static final String ACCESS_KEY = System.getenv("MQTT_AK_ENV");
    public static final String SECRET_KEY = System.getenv("MQTT_SK_ENV");
}
