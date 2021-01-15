package se.mau.iotap.diss.mqttpubsub;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by zhizhong on 2017-01-09.
 */

public class DemoService extends IntentService {

    public DemoService() {
        super("DemoService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String data="111";
    }


}
