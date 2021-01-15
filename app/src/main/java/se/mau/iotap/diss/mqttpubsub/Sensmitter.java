package se.mau.iotap.diss.mqttpubsub;

public abstract class Sensmitter extends Device {
    private double humidity = 0;
    private double temperature = 0;
    private int lightLevel = 0;
    private boolean updated = false;

    public Sensmitter(String uid, String environment, String device) {
        super(uid, environment, device);
    }

    public Sensmitter(String uid,
                      String environment,
                      String device,
                      double humidity,
                      double temperature,
                      int lightLevel) {
        super(uid, environment, device);
        this.humidity = humidity;
        this.temperature = temperature;
        this.lightLevel = lightLevel;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        if (humidity >= 0) {
            this.humidity = humidity;
        }
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public void setLightLevel(int lightLevel) {
        if (lightLevel >= 0) {
            this.lightLevel = lightLevel;
        }
    }
}
