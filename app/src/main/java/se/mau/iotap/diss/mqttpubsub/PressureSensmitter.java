package se.mau.iotap.diss.mqttpubsub;

public class PressureSensmitter extends Sensmitter {
    public float pressure = 0;

    public PressureSensmitter(String uid, String environment, String device) {
        super(uid, environment, device);
    }

    public PressureSensmitter(String uid,
                         String environment,
                         String device,
                         float humidity,
                         float temperature,
                         int lightLevel,
                         float pressure) {
        super(uid, environment, device, humidity, temperature, lightLevel);
        this.pressure = pressure;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        if (pressure >= 0) {
            this.pressure = pressure;
        }
    }

    @Override
    public String generateMqttMessage() {
        String dataBlock = "{" +
                "\"humidity\":" + getHumidity() + "," +
                "\"light_level\":" + getLightLevel() + "," +
                "\"pressure\":" + pressure + "," +
                "\"temperature\":" + getTemperature() +
                " }";

        return generateMqttMessage(dataBlock);
    }
}
