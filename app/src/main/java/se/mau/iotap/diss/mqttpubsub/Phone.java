package se.mau.iotap.diss.mqttpubsub;

public class Phone extends Device {
    private boolean movement = false;
    private double soundLevel = 0;

    public Phone(String uid, String environment, String device) {
        super(uid, environment, device);
    }

    public Phone(String uid, String environment, String device, boolean movement, int soundLevel) {
        super(uid, environment, device);
        this.movement = movement;
        this.soundLevel = soundLevel;
    }

    public boolean getMovement() {
        return movement;
    }

    public void setMovement(boolean movement) {
        this.movement = movement;
    }

    public double getSoundLevel() {
        return soundLevel;
    }

    public void setSoundLevel(double soundLevel) {
        if (soundLevel >= 0) {
            this.soundLevel = soundLevel;
        }
    }

    private int movementToInt() {
        return movement ? 1 : 0;
    }

    @Override
    public String generateMqttMessage() {
        String dataBlock = "{" +
                "\"movement\":" + movementToInt() + "," +
                "\"sound_level\":" + soundLevel +
                " }";

        return generateMqttMessage(dataBlock);
    }
}
