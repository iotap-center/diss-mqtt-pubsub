package se.mau.iotap.diss.mqttpubsub;

public abstract class Device {
    private String uid = "";
    private String environment = "";
    private String device = "";

    public Device(String uid, String environment, String device) {
        init(uid, environment, device);
    }

    private void init(String uid, String environment, String device) {
        this.uid = uid;
        this.environment = environment;
        this.device = device;
    }

    public String getUid() {
        return uid;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getDevice() {
        return device;
    }

    public String generateMqttTopic() {
        return environment + "/" + device + "/sensmitter/" + uid;
    }

    protected String generateMqttMessage(String dataBlock) {
        String jsonMessage = "{" +
                "\"uid\":\"" + getUid() + "\"," +
                "\"timestamp\":" + System.currentTimeMillis() + "," +
                "\"msgtype\":\"data\"," +
                "\"data\":" + dataBlock +
                "}";

        return jsonMessage;
    }

    public abstract String generateMqttMessage();
}
