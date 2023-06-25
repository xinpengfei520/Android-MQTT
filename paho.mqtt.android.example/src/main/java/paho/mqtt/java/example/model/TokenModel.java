package paho.mqtt.java.example.model;

public class TokenModel {

    private int code;
    private DataDTO data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public DataDTO getData() {
        return data;
    }

    public void setData(DataDTO data) {
        this.data = data;
    }

    public static class DataDTO {
        private String token;
        private String actions;
        private int expireDays;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getActions() {
            return actions;
        }

        public void setActions(String actions) {
            this.actions = actions;
        }

        public int getExpireDays() {
            return expireDays;
        }

        public void setExpireDays(int expireDays) {
            this.expireDays = expireDays;
        }
    }
}
