package co.android.modcru.codeforcesstatusnotifier;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Intent mServiceIntent;
    private CheckerService mCheckerService;
    boolean debug = BuildConfig.DEBUG ,loading = false,more=true;
    int From = 1,Rate,get=0;
    List<Submission> list;
    String PicUrl, Handle, filter="";
    TextView handle,rate;
    ImageView iv;
    RecyclerView lv;
    RecyclerViewAdapter adapter;
    ProgressBar pb,dpb;
    SharedPreferences pref;
    SwipeRefreshLayout mSwipeRefreshLayout;
    RelativeLayout userlayout;
    Spinner sp;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(getSharedPreferences("CodeForcesStatusNotifier",MODE_PRIVATE).getBoolean("isFirst",true))
        {
            getSharedPreferences("CodeForcesStatusNotifier",MODE_PRIVATE).edit().putBoolean("isFirst",false).apply();
            Intent modifySettings=new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(modifySettings);
        }
        mCheckerService = new CheckerService();
        mServiceIntent = new Intent(this,mCheckerService.getClass());
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (!isMyServiceRunning(mCheckerService.getClass()) && pref.getBoolean("work",false)
                && pref.getBoolean("valid",false)) {
            startService(mServiceIntent);
        }

        //Ads
        MobileAds.initialize(this, "ca-app-pub-3401946114351861~2846470751");
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                mAdView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
                mAdView.setVisibility(View.GONE);
            }
        });

        handle = findViewById(R.id.handleTv);
        rate = findViewById(R.id.rate);
        iv = findViewById(R.id.iv);
        lv = findViewById(R.id.list);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        lv.setLayoutManager(linearLayoutManager);
        DividerItemDecoration itemDecorator = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider));
        lv.addItemDecoration(itemDecorator);
        pb = findViewById(R.id.progressBar);
        dpb = findViewById(R.id.dpb);
        pb.setVisibility(View.VISIBLE);
        userlayout = findViewById(R.id.userLayout);
        sp = findViewById(R.id.filter);

        final String filters[]={"All","Accepted","Not Accepted"};
        ArrayAdapter<String> sa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filters);
        sp.setAdapter(sa);
        sp.setSelection(0);
        filter = filters[0];

        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                filter = filters[i];
                refresh();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                filter = filters[0];
            }
        });

        userlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                ad.setTitle(Handle);
                ad.setMessage("Set new Handle ?");
                ad.setNegativeButton("Cancel",null);
                ad.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent modifySettings=new Intent(MainActivity.this,SettingsActivity.class);
                        startActivity(modifySettings);
                    }
                });
                ad.show();
            }
        });

        mSwipeRefreshLayout=findViewById(R.id.swipeContainer);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        list = new ArrayList<>();
        loadUser();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    void refresh()
    {
        From = 1;
        more=true;
        loadUser();
        loadData();
    }


    void loadlist()
    {
        if(mSwipeRefreshLayout.isRefreshing())
            mSwipeRefreshLayout.setRefreshing(false);
        dpb.setVisibility(View.GONE);
        pb.setVisibility(View.GONE);
        if(adapter==null)
        {
            adapter=new RecyclerViewAdapter(lv, MainActivity.this, list, new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Submission sub = list.get(i);
                    if(!sub.getContestId().equals("")) {
                        String url = "http://codeforces.com/contest/"+sub.getContestId()+"/submission/"+sub.getId();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(browserIntent);
                    }
                }
            });
            adapter.setOnLoadMoreListener(new RecyclerViewAdapter.OnLoadMoreListener() {
                @Override
                public void onLoadMore() {
                    if(!loading&&more)
                    {
                        loading = true;
                        dpb.setVisibility(View.VISIBLE);
                        Handle = pref.getString("handle","").trim();
                        get=0;
                        Log.d("HHHHH",Integer.toString(list.size()));
                        new getData().execute(Handle);
                    }else
                        adapter.setLoaded();
                }
            });
            lv.setAdapter(adapter);
        }
        else
        {
            adapter.notifyDataSetChanged();
            adapter.setLoaded();
        }
        loading = false;
    }

    void loadUser()
    {
        Handle = pref.getString("handle","").trim();
        new getUser().execute(Handle);
    }

    void loadData()
    {
        list.clear();
        if(adapter!=null)
            adapter.notifyDataSetChanged();
        Handle = pref.getString("handle","").trim();
        get=0;
        new getData().execute(Handle);
    }

    class getUser extends AsyncTask<String,String,String>
    {

        String H="";
        @Override
        protected String doInBackground(String... handle) {
            H=handle[0];
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            String urlTo = "http://codeforces.com/api/user.info?handles="+handle[0];
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
                return result;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return "f";
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s.equals("f"))
            {
                Toast.makeText(MainActivity.this,"Check network connection and handle",Toast.LENGTH_LONG).show();
            }
            else
            {
                try {
                    JSONObject object = new JSONObject(s);
                    JSONArray arr = object.getJSONArray("result");
                    JSONObject usr = arr.getJSONObject(0);
                    PicUrl = usr.getString("titlePhoto");
                    if(!PicUrl.contains("http"))
                        PicUrl = "https:" + PicUrl;
                    Log.d("ma3lsh",PicUrl);
                    Rate = usr.getInt("rating");
                    Picasso.with(MainActivity.this).load(PicUrl).fit().centerCrop().noFade().into(iv);
                    handle.setText(H);
                    rate.setText("Rating: "+Integer.toString(Rate));
                    switch (usr.getString("rank"))
                    {
                        case "specialist":
                            handle.setTextColor(MainActivity.this.getResources().getColor(R.color.sp));
                            break;
                        case "newbie":
                            handle.setTextColor(MainActivity.this.getResources().getColor(R.color.ni));
                            break;
                        case "pupil":
                            handle.setTextColor(MainActivity.this.getResources().getColor(R.color.pu));
                            break;
                        case "expert":
                            handle.setTextColor(MainActivity.this.getResources().getColor(R.color.ex));
                            break;
                        case "candidate master":
                            handle.setTextColor(MainActivity.this.getResources().getColor(R.color.cn));
                            break;
                        case "master":
                            handle.setTextColor(MainActivity.this.getResources().getColor(R.color.ms));
                            break;
                        case "international master":
                            handle.setTextColor(MainActivity.this.getResources().getColor(R.color.is));
                            break;
                        case "grandmaster":
                            handle.setTextColor(MainActivity.this.getResources().getColor(R.color.gm));
                            break;
                        case "international grandmaster":
                            handle.setTextColor(MainActivity.this.getResources().getColor(R.color.igm));
                            break;
                        case "legendary grandmaster":
                            handle.setTextColor(MainActivity.this.getResources().getColor(R.color.lgm));
                            break;
                    }
                    rate.setTextColor(handle.getTextColors());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    class getData extends AsyncTask<String,String,String>
    {

        String H="";
        @Override
        protected String doInBackground(String... handle) {
            H=handle[0];
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            String urlTo = "http://codeforces.com/api/user.status?handle="+H+"&from="+Integer.toString(From)+"&count=20";
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
                return result;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return "f";
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s.equals("f"))
            {
                Toast.makeText(MainActivity.this,"Check network connection and Handle",Toast.LENGTH_LONG).show();
            }
            else
            {
                try {
                    JSONObject object = new JSONObject(s);
                    JSONArray arr = object.getJSONArray("result");
                    int con=0;
                    if(arr.length()==0)more=false;
                    for(int i=0;i<arr.length();i++)
                    {
                        JSONObject sub = arr.getJSONObject(i);
                        Submission submission = new Submission();
                        submission.setId(sub.getString("id"));
                        submission.setContestId(sub.getString("contestId"));
                        submission.setTime(sub.getLong("creationTimeSeconds"));
                        JSONObject prob = sub.getJSONObject("problem");
                        submission.setIndex(prob.getString("index"));
                        submission.setName(prob.getString("name"));
                        submission.setVerdic(sub.getString("verdict"));
                        submission.setTests(sub.getInt("passedTestCount"));
                        submission.setRunTime(sub.getString("timeConsumedMillis"));
                        submission.setMemory(sub.getString("memoryConsumedBytes"));
                        if(filter.equals("Accepted")&&!submission.getVerdic().equals("OK"))
                            continue;
                        if(filter.equals("Not Accepted")&&submission.getVerdic().equals("OK"))
                            continue;
                        list.add(submission);
                        con++;
                    }
                    From += arr.length();
                    loadlist();
                    get += con;
                    if(get<20&&more)
                        new getData().execute(Handle);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent modifySettings=new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(modifySettings);
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if(debug)
                    Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        if(debug)
            Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    @Override
    protected void onDestroy() {
        stopService(mServiceIntent);
        if(debug)
            Log.i("DESTROY?", "onDestroy!");
        super.onDestroy();

    }
}
