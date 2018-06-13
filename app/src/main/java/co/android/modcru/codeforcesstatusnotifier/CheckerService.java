package co.android.modcru.codeforcesstatusnotifier;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by mahmoud on 23/04/18.
 */

public class CheckerService extends Service {

    boolean debug = BuildConfig.DEBUG;
    boolean isChecking = false;
    SharedPreferences settings;
    public CheckerService()
    {
        super();
        if(debug)
            Log.i("HERE", "here I am!");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if(settings.getBoolean("valid",false) && settings.getBoolean("work",false)) {
            startTimer();
            return START_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(debug)
            Log.i("EXIT", "onDestroy!");
        stopTimerTask();
        if(!settings.getBoolean("work",false) || !settings.getBoolean("valid",false))
            return;
        Intent broadcastIntent = new Intent("co.android.modcru.RestartChecker");
        sendBroadcast(broadcastIntent);
    }

    private Timer timer;
    private TimerTask timerTask;
    public void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        //schedule the timer, to wake up every 30 seconds
        timer.schedule(timerTask, 30000, 30000);
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                if(!isChecking)
                    Check();
                if(debug)
                    Log.i("Check", "here I am starting !");
            }
        };
    }

    public void stopTimerTask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void Check()
    {
        isChecking = true;
        String result="";
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        SharedPreferences pref = getApplicationContext().getSharedPreferences("CodeforcesData", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        String lastAcID = pref.getString("lastAcID","");
        String lastWaID = pref.getString("lastWaID","");
        String handle = settings.getString("handle","").trim();
        String urlTo = "http://codeforces.com/api/user.status?handle="+handle+"&from=1&count=10";
        try {
            URL url = new URL(urlTo);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.connect();

            int code = connection.getResponseCode();
            isChecking = false;
            if(code != 200)
                return;
            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuilder buffer = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            result = buffer.toString();
            connection.disconnect();

        }catch (IOException e) {
            if(debug)
                Log.i("NETWORK",e.toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {
            }
        }
        try {
            JSONObject object = new JSONObject(result);
            if(!object.getString("status").equals("OK"))
                return;
            JSONArray arr = object.getJSONArray("result");
            if(arr == null)
                return;
            for(int i = 0; i < arr.length(); i++)
            {
                JSONObject statu = arr.getJSONObject(i);
                if(statu.getString("verdict").equals("OK"))
                {
                    if(statu.getString("id").equals(lastAcID))
                        return;
                    String probName = statu.getJSONObject("problem").getString("name");
                    editor.putString("lastAcID",statu.getString("id"));
                    editor.apply();
                    sendNotification("Ohh! \n You have a new Accepted in problem : "+probName,statu.getString("id"),"Accepted");
                    break;
                }
                else
                {
                    if(statu.getString("id").equals(lastWaID))
                        return;
                    String probName = statu.getJSONObject("problem").getString("name");
                    editor.putString("lastWaID",statu.getString("id"));
                    editor.apply();
                    String verdict = statu.getString("verdict");
                    sendNotification("Ops! \n You have a"+ verdict +"in problem : "+probName,statu.getString("id"),verdict);
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        isChecking = false;
    }

    public void sendNotification(String msg,String id,String type)
    {
//        Intent notificationIntent = new Intent(this, Notification.class);
        //PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Uri sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.n1);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        //.setContentIntent(pendingIntent)
                        .setSmallIcon(R.drawable.codeforces_logo)
                        .setContentTitle(type)
                        .setContentText(msg)
                        .setSound(sound);
        if(type.equals("Accepted"))
        {
            String dir = settings.getString("accepted_ringtone",null);
            if(dir != null)
                mBuilder.setSound(Uri.parse(dir));
        }else
        {
            String dir = settings.getString("wa_ringtone",null);
            if(dir != null)
                mBuilder.setSound(Uri.parse(dir));
        }
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if((type.equals("Accepted") && settings.getBoolean("ac_sound",false))
            ||(!type.equals("Accepted") && settings.getBoolean("wa_sound",false)))
            if (mgr != null)
                mgr.notify(Integer.parseInt(id), mBuilder.build());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
