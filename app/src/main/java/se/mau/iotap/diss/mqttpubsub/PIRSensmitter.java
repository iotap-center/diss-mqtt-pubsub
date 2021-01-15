package se.mau.iotap.diss.mqttpubsub;

public class PIRSensmitter extends Sensmitter {
    private boolean pirTriggered = false;

    public PIRSensmitter(String uid, String environment, String device) {
        super(uid, environment, device);
    }

    public PIRSensmitter(String uid,
                         String environment,
                         String device,
                         double humidity,
                         double temperature,
                         int lightLevel,
                         boolean pirTriggered) {
        super(uid, environment, device, humidity, temperature, lightLevel);
        this.pirTriggered = pirTriggered;
    }

    public boolean getPirTriggered() {
        return  pirTriggered;
    }

    public void setPirTriggered(boolean pirTriggered) {
        this.pirTriggered = pirTriggered;
    }

    private int pirToInt() {
        return pirTriggered ? 1 : 0;
    }

    @Override
    public String generateMqttMessage() {
        String dataBlock = "{" +
                "\"humidity\":" + getHumidity() + "," +
                "\"light_level\":" + getLightLevel() + "," +
                "\"movement\":" + pirToInt() + "," +
                "\"temperature\":" + getTemperature() +
                " }";

        return generateMqttMessage(dataBlock);
    }
}
