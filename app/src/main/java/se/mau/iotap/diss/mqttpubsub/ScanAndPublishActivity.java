/**
 * Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * <p>
 * http://aws.amazon.com/apache2.0
 * <p>
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package se.mau.iotap.diss.mqttpubsub;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

@TargetApi(27)


public class ScanAndPublishActivity extends Activity {

    // --- Constants to modify per your configuration ---
    static final String LOG_TAG = ScanAndPublishActivity.class.getCanonicalName();

    private static final int REQUEST_ENABLE_BT = 42;
    private static final int REQUEST_ENABLE_LOCATION = 417;

    // IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = BuildConfig.endpoint;
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = BuildConfig.pool;
    // Name of the AWS IoT policy to attach to a newly created certificate
    private static final String AWS_IOT_POLICY_NAME = BuildConfig.policy;
    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.fromName(BuildConfig.region);
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = BuildConfig.keystore;
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = BuildConfig.password;
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = BuildConfig.certificate;
    public double amplitudeDb;
    private PressureSensmitter sensmitter1 = new PressureSensmitter("sensmitter_1", "iotap_lab", "phone_1");
    private PIRSensmitter sensmitter2 = new PIRSensmitter("sensmitter_2", "iotap_lab", "phone_1");
    private PIRSensmitter sensmitter3 = new PIRSensmitter("sensmitter_3", "iotap_lab", "phone_1");
    private Phone phone = new Phone("phone_1", "iotap_lab", "phone_1");
    EditText txtSubcribe;
    EditText txtTopic;
    EditText txtMessage;
    TextView tvLastMessage;
    TextView tvClientId;
    TextView tvStatus;

    Button btnConnect;
    Button btnSubscribe;
    Button btnPublish;
    Button btnDisconnect;
    Button btnRecord;
    AWSIotClient mIotAndroidClient;
    AWSIotMqttManager mqttManager;
    String clientId;
    String keystorePath;
    String keystoreName;
    String keystorePassword;
    KeyStore clientKeyStore = null;
    String certificateId;
    CognitoCachingCredentialsProvider credentialsProvider;

    View.OnClickListener connectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, "clientId = " + clientId);

            try {
                mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                        runOnUiThread(() -> {
                            if (status == AWSIotMqttClientStatus.Connecting) {
                                tvStatus.setText("Connecting...");

                            } else if (status == AWSIotMqttClientStatus.Connected) {
                                tvStatus.setText("Connected");
                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                if (throwable != null) {
                                    Log.e(LOG_TAG, "Connection error.", throwable);
                                }
                                tvStatus.setText("Reconnecting");
                            } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                if (throwable != null) {
                                    Log.e(LOG_TAG, "Connection error.", throwable);
                                }
                                tvStatus.setText("Disconnected");
                            } else {
                                tvStatus.setText("Disconnected");
                            }
                        });
                    }
                });
            } catch (final Exception e) {
                Log.e(LOG_TAG, "Connection error.", e);
                tvStatus.setText("Error! " + e.getMessage());
            }
        }
    };

    View.OnClickListener subscribeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            final String topic = txtSubcribe.getText().toString();

            Log.d(LOG_TAG, "topic = " + topic);

            try {
                mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                        (topic1, data) -> runOnUiThread(() -> {
                            try {
                                String message = new String(data, "UTF-8");
                                Log.d(LOG_TAG, "Message arrived:");
                                Log.d(LOG_TAG, "   Topic: " + topic1);
                                Log.d(LOG_TAG, " Message: " + message);

                                tvLastMessage.setText(message);

                            } catch (UnsupportedEncodingException e) {
                                Log.e(LOG_TAG, "Message encoding error.", e);
                            }
                        }));
            } catch (Exception e) {
                Log.e(LOG_TAG, "Subscription error.", e);
            }
        }
    };

    View.OnClickListener publishClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            final String topic = txtTopic.getText().toString();
            final String msg = txtMessage.getText().toString();

            if (topic.trim().length() > 0) {
                try {
                    mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Publish error.", e);
                }
            } else {
                Log.e(LOG_TAG, "No topic.");
            }
        }
    };

    View.OnClickListener disconnectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mqttManager.disconnect();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Disconnect error.", e);
            }

        }
    };

    private SensorManager sensorManager = null;
    private SensorEventListener sensorListener = null;
    private Timer timerButton;
    private TimerTask taskButton;
    private boolean isUpload = true;
    private double noiseDate = 0.0;
    private DeviceSensor deviceSensor;
    private Context context;
    private long mAdvertisementCount = 0;
    private boolean bleScanning = false;
    private boolean recording = false;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            StringBuilder sb = new StringBuilder();
            byte[] scanRecord = result.getScanRecord().getBytes();
            for (byte b : scanRecord) {
                sb.append(IntToHex2(b & 0xff));
            }

            if ((result.getDevice().getAddress().contains("F3:E5:7F:73:4F:81"))) {
                updateSensmitter(scanRecord, sensmitter1);
                Log.i(LOG_TAG, "Sensmitter 1 detected");
            } else if ((result.getDevice().getAddress().contains("F8:1D:78:B0:03:A5"))) {
                updateSensmitter(scanRecord, sensmitter2);
                Log.i(LOG_TAG, "Sensmitter 2 detected");
            } else if ((result.getDevice().getAddress().contains("F8:1D:78:B0:03:A6"))) {
                updateSensmitter(scanRecord, sensmitter3);
                Log.i(LOG_TAG, "Sensmitter 3 detected");
            }

            bleScanning = true;
            Log.d("BLE data", "" + scanRecord);
        }
    };

    View.OnClickListener startClick = v -> tryBLEAndRecord();

    private void tryBLEAndRecord() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PERMISSION_DENIED) {
            Log.e(LOG_TAG, "Lacking permissions");
            requestPermissions();
        } else {
            Log.i(LOG_TAG, "Has permissions");
            startRecording();
            startBLE();
        }
    }

    public static String IntToHex2(int i) {
        char hex_2[] = {Character.forDigit((i >> 4) & 0x0f, 16), Character.forDigit(i & 0x0f, 16)};
        String hex_2_str = new String(hex_2);
        return hex_2_str.toUpperCase();
    }

    public double getNoiseDate() {
        return noiseDate;
    }

    public void setNoiseDate(double noiseDate) {
        this.noiseDate = noiseDate;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        deviceSensor = new se.mau.iotap.diss.mqttpubsub.DeviceSensor(context);

        txtSubcribe = (EditText) findViewById(R.id.txtSubcribe);
        txtTopic = (EditText) findViewById(R.id.txtTopic);
        txtMessage = (EditText) findViewById(R.id.txtMessage);

        tvLastMessage = (TextView) findViewById(R.id.tvLastMessage);
        tvClientId = (TextView) findViewById(R.id.tvClientId);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(connectClick);
        btnConnect.setEnabled(false);

        btnSubscribe = (Button) findViewById(R.id.btnSubscribe);
        btnSubscribe.setOnClickListener(subscribeClick);

        btnPublish = (Button) findViewById(R.id.btnPublish);
        btnPublish.setOnClickListener(publishClick);

        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(disconnectClick);

        btnRecord = (Button) findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(startClick);

        Button uploadButton = (Button) findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(view -> {
            isUpload = true;
            if (isUpload) {
                timerButton = new Timer();
                taskButton = new TimerTask() {
                    @Override
                    public void run() {
                        phone.setMovement(deviceSensor.getStateDeviceMovement() == "1");
                        phone.setSoundLevel(getNoiseDate());
                        mqttManager.publishString(
                                phone.generateMqttMessage(),
                                phone.generateMqttTopic(),
                                AWSIotMqttQos.QOS0
                        );
                        Log.d("SSSXS", "uping");
                    }
                };
                timerButton.scheduleAtFixedRate(taskButton, 1000, 10000);

            }

        });

        Button disconButton = (Button) findViewById(R.id.disconButton);
        disconButton.setOnClickListener(view -> {
            timerButton.cancel();
            isUpload = false;
            if (isUpload) {
                phone.setMovement(deviceSensor.getStateDeviceMovement() == "1");
                phone.setSoundLevel(getNoiseDate());
                mqttManager.publishString(
                        phone.generateMqttMessage(),
                        phone.generateMqttTopic(),
                        AWSIotMqttQos.QOS0
                );
                Log.d("SSSXS", "upi1111ng");
            }
            Log.d("SSSXS", "dddd");
        });

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();
        tvClientId.setText(clientId);

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        keystorePath = getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                    btnConnect.setEnabled(true);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
                File f = new File(keystorePath + "/" + keystoreName);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(() -> {
                try {
                    // Create a new private key and certificate. This call
                    // creates both on the server and returns them to the
                    // device.
                    CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                            new CreateKeysAndCertificateRequest();
                    createKeysAndCertificateRequest.setSetAsActive(true);
                    final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                    createKeysAndCertificateResult =
                            mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                    Log.i(LOG_TAG,
                            "Cert ID: " +
                                    createKeysAndCertificateResult.getCertificateId() +
                                    " created.");

                    // store in keystore for use in MQTT client
                    // saved as alias "default" so a new certificate isn't
                    // generated each run of this application
                    Log.i(LOG_TAG, "Saving certs to " + keystorePath + "/" + keystoreName);
                    AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                            createKeysAndCertificateResult.getCertificatePem(),
                            createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                            keystorePath, keystoreName, keystorePassword);

                    // load keystore from file into memory to pass on
                    // connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);

                    // Attach a policy to the newly created certificate.
                    // This flow assumes the policy was already created in
                    // AWS IoT and we are now just attaching it to the
                    // certificate.
                    AttachPrincipalPolicyRequest policyAttachRequest =
                            new AttachPrincipalPolicyRequest();
                    policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                    policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                            .getCertificateArn());
                    mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                    Log.i(LOG_TAG, "Cert/key created");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnConnect.setEnabled(true);
                        }
                    });
                } catch (Exception e) {
                    Log.e(LOG_TAG,
                            "Exception occurred when generating new private key and certificate.",
                            e);
                }
            }).start();
        }

        // Start BLE sniffing and sound recording - if allowed, of course
        tryBLEAndRecord();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length == 1 && grantResults[0] == PERMISSION_GRANTED) {
                startRecording();
                startBLE();
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Too bad, can't use some functionality.", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void startBLE() {
        if (!bleScanning) {
            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            ScanFilter filter = new ScanFilter.Builder().setDeviceName("W").build();
            filters.add(filter);
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .setReportDelay(0L)
                    .build();

            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);
            BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

            // Start BLE
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            BluetoothLeScanner bleScanner = mBluetoothAdapter.getBluetoothLeScanner();
            bleScanner.startScan(filters, scanSettings, scanCallback);
            Log.i(LOG_TAG, "BLE scanning started");
        }
    }

    private void startRecording() {
        if (!recording) {
            recording = true;
            MediaRecorder mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            try {
                mRecorder.prepare();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mRecorder.start();
            Timer timer = new Timer();

            timer.scheduleAtFixedRate(new SendTimerTask(mRecorder), 1000, 1000);
            Log.i(LOG_TAG, "Sound recording started");
        }
    }

    private void requestPermissions() {
        // Permission has not been granted and must be requested.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(LOG_TAG, "Bluetooth permissions needed");
            Snackbar.make(findViewById(android.R.id.content), "You need to accept in order to use Bluetooth.",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", view -> {
                        // Request the permission
                        ActivityCompat.requestPermissions(ScanAndPublishActivity.this,
                                new String[]{
                                        Manifest.permission.BLUETOOTH,
                                        Manifest.permission.BLUETOOTH_ADMIN
                                },
                                REQUEST_ENABLE_BT);
                    }).show();
        } else {
            Log.i(LOG_TAG, "Bluetooth permissions granted");
        }

        // Permission has not been granted and must be requested.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(LOG_TAG, "Location permissions needed");
            Snackbar.make(findViewById(android.R.id.content), "You need to accept in order to use Location.",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(ScanAndPublishActivity.this,
                            new String[]{
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                            },
                            REQUEST_ENABLE_LOCATION);
                }
            }).show();
        } else {
            Log.i(LOG_TAG, "Location permissions granted");
        }

        // Permission has not been granted and must be requested.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.i(LOG_TAG, "Recording permissions needed");
            Snackbar.make(findViewById(android.R.id.content), "You need to accept in order to record sound.",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(ScanAndPublishActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_ENABLE_LOCATION);
                }
            }).show();
        } else {
            Log.i(LOG_TAG, "Recording permissions granted");
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("PubSub Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        deviceSensor.startDeviceSensor();
   /*     if(tvStatus.getText()=="Connected") {
            Log.d("22323","in");
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new SendTimerTask(), 90000, 20000);
            Log.d("22323","out");
        }*/

    }

    public void updateSensmitter(byte[] state, Sensmitter sensmitter) {
        StringBuilder name1 = new StringBuilder();
        name1.append(IntToHex2(state[33] & 0xff));
        name1.append(IntToHex2(state[34] & 0xff));

        StringBuilder value1 = new StringBuilder();
        value1.append(IntToHex2(state[35] & 0xff));
        value1.append(IntToHex2(state[36] & 0xff));

        Integer temp = Integer.parseInt(value1.toString(), 16);
        float tempV = temp / 10.00f;

        StringBuilder name2 = new StringBuilder();
        name2.append(IntToHex2(state[37] & 0xff));
        name2.append(IntToHex2(state[38] & 0xff));

        StringBuilder value2 = new StringBuilder();
        value2.append(IntToHex2(state[39] & 0xff));
        value2.append(IntToHex2(state[40] & 0xff));

        Integer humidity = Integer.parseInt(value2.toString(), 16);
        float humidityV = humidity / 10.00f;

        StringBuilder name3 = new StringBuilder();
        name3.append(IntToHex2(state[41] & 0xff));
        name3.append(IntToHex2(state[42] & 0xff));

        StringBuilder value3 = new StringBuilder();
        value3.append(IntToHex2(state[43] & 0xff));
        value3.append(IntToHex2(state[44] & 0xff));

        Integer light = Integer.parseInt(value3.toString(), 16);

        StringBuilder name4 = new StringBuilder();
        name4.append(IntToHex2(state[45] & 0xff));
        name4.append(IntToHex2(state[46] & 0xff));
        float valueD = 0.00f;

        if (name4.toString().equals("0008")) {
            StringBuilder value4 = new StringBuilder();

            value4.append(IntToHex2(state[47] & 0xff));

            valueD = Integer.parseInt(value4.toString(), 16);

        } else if (name4.toString().equals("0009")) {
            StringBuilder value4 = new StringBuilder();
            value4.append(IntToHex2(state[47] & 0xff));
            value4.append(IntToHex2(state[48] & 0xff));

            valueD = Integer.parseInt(value4.toString(), 16) / 10.00f;
        }

        sensmitter.setHumidity(humidityV);
        sensmitter.setTemperature(tempV);
        sensmitter.setLightLevel(light);
        sensmitter.setUpdated(true);

        if (sensmitter instanceof PIRSensmitter) {
            ((PIRSensmitter) sensmitter).setPirTriggered(valueD == 1 ? true : false);
        } else if (sensmitter instanceof PressureSensmitter) {
            ((PressureSensmitter) sensmitter).setPressure(valueD);
        }
    }

    private class SendTimerTask extends TimerTask {
        private MediaRecorder recorder;

        public SendTimerTask() {

        }

        public SendTimerTask(MediaRecorder recorder) {
            this.recorder = recorder;

        }

        @Override
        public void run() {
            try {
                float audiometers = recorder.getMaxAmplitude();
                amplitudeDb = 20 * Math.log10((double) Math.abs(audiometers));
                setNoiseDate(amplitudeDb);

                Log.d("223SSS", String.valueOf(amplitudeDb));
                if (sensmitter1.isUpdated()) {
                    sensmitter1.setUpdated(false);
                    mqttManager.publishString(
                            sensmitter1.generateMqttMessage(),
                            sensmitter1.generateMqttTopic(),
                            AWSIotMqttQos.QOS0
                    );
                }
                if (sensmitter2.isUpdated()) {
                    sensmitter2.setUpdated(false);
                    mqttManager.publishString(
                            sensmitter2.generateMqttMessage(),
                            sensmitter2.generateMqttTopic(),
                            AWSIotMqttQos.QOS0
                    );
                }
                if (sensmitter3.isUpdated()) {
                    sensmitter3.setUpdated(false);
                    mqttManager.publishString(
                            sensmitter3.generateMqttMessage(),
                            sensmitter3.generateMqttTopic(),
                            AWSIotMqttQos.QOS0
                    );
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }
        }
    }
}