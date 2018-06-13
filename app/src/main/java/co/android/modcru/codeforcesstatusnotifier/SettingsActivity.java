package co.android.modcru.codeforcesstatusnotifier;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SettingsActivity extends PreferenceActivity {

    Intent mServiceIntent;
    CheckerService mCheckerService;
    SharedPreferences pref;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        mCheckerService = new CheckerService();
        mServiceIntent = new Intent(this, mCheckerService.getClass());
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
                if(key.equals("handle")) {
                    new Validate().execute(pref.getString(key, "").trim());
                }
            }
        };
        pref.registerOnSharedPreferenceChangeListener(listener);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if(BuildConfig.DEBUG)
                    Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        if(BuildConfig.DEBUG)
            Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    class Validate extends AsyncTask<String,String,String>
    {
        String H="";
        @Override
        protected String doInBackground(String... handle) {
            H=handle[0];
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            String urlTo = "http://codeforces.com/api/user.status?handle="+handle[0]+"&from=1&count=1";
            String result ="";
            try {
                URL url = new URL(urlTo);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.connect();

                int code = connection.getResponseCode();
                if(code != 200)
                    return "f";
                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                }

                result = buffer.toString();
                connection.disconnect();

            }catch (IOException e) {
                if(BuildConfig.DEBUG)
                    Log.i("NETWORK",e.toString());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                }
            }
            try {
                JSONObject object = new JSONObject(result);
                if(!object.getString("status").equals("OK"))
                    return "f";
                return "t";
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return "f";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s.equals("t"))
            {
                pref.edit().putBoolean("valid",true).apply();
                if (!isMyServiceRunning(mCheckerService.getClass()) && pref.getBoolean("work",false)) {
                    startService(mServiceIntent);
                }
            }
            else
            {
                Toast.makeText(SettingsActivity.this,"User with handle "+H+" not found",Toast.LENGTH_LONG).show();
                pref.edit().putBoolean("valid",false).apply();
            }
        }
    }

}
