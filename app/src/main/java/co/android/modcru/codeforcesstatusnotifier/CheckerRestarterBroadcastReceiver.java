package co.android.modcru.codeforcesstatusnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by mahmoud on 23/04/18.
 */

public class CheckerRestarterBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(BuildConfig.DEBUG)
            Log.i(CheckerRestarterBroadcastReceiver.class.getSimpleName(), "Service Stops!!!!!");
        context.startService(new Intent(context, CheckerService.class));
    }
}
