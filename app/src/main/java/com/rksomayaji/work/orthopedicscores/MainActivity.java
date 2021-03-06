package com.rksomayaji.work.orthopedicscores;

import android.Manifest;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.rksomayaji.work.orthopedicscores.helper.HTTPHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rksomayaji.work.orthopedicscores.helper.TestXMLParserHelper;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    DrawerLayout mainMenu;
    ArrayAdapter<String> menuAdapter;
    ListView menuList;
    private static final int WRITE_PERMISSION_REQUEST_CODE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent appShortcutIntent = getIntent();
        int launch = appShortcutIntent.getIntExtra("APP_SHORTCUT",0);
        if (launch == 1){
            Log.i("MAIN", String.valueOf(launch));
            launchTest(appShortcutIntent.getIntExtra(OrthoScores.TEST_NUMBER,0));
        }else {
            Log.i("MAIN", String.valueOf(launch));
            if(findViewById(R.id.fragment_container) != null){
                if (savedInstanceState != null) return;

                MainFragment mainFragment = new MainFragment();

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(R.id.fragment_container,mainFragment);
                ft.commit();
            }
        }

        mainMenu = (DrawerLayout) findViewById(R.id.drawer);

        ArrayList<String> testArray = getAvailableTests();
        menuAdapter = new ArrayAdapter<>(this, R.layout.list_layout,testArray);
        menuList = (ListView) findViewById(R.id.test_list);
        menuList.setAdapter(menuAdapter);

        menuList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                launchTest(i);
                mainMenu.closeDrawer(GravityCompat.START);
            }
        });
        checkForUpdate();
    }

    private void checkForUpdate() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        if(settings.getBoolean("auto_update",false) && settings.getBoolean("auto_install",false)){
            if(Build.VERSION.SDK_INT >= 23){
                if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    if(checkInternet()) new getUpdate().execute();
                }else{
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION_REQUEST_CODE);
                }
            }else{
                if(checkInternet()) new getUpdate().execute();
            }
        }else if (settings.getBoolean("auto_update",false) && !settings.getBoolean("auto_install",false)){
            if(checkInternet()) new getUpdate().execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case WRITE_PERMISSION_REQUEST_CODE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(checkInternet()) new getUpdate().execute();
                }else{
                    Toast.makeText(this,
                            "Kindly give permission for writing on external storage in app settings to download the update",
                            Toast.LENGTH_SHORT)
                            .show();
                }
        }
    }

    private ArrayList<String> getAvailableTests() {
        TestXMLParserHelper helper = new TestXMLParserHelper(this);

        return helper.getTestList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_about:
                Intent aboutIntent = new Intent(this,AboutActivity.class);
                aboutIntent.putExtra(OrthoScores.NOTIFICATION,false);
                startActivity(aboutIntent);
                return true;
            case R.id.menu_settings:
                Intent settingsIntent = new Intent(this,SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void launchTest (int i){
        TestFragment fragment = new TestFragment();
        Bundle args = new Bundle();
        args.putInt(OrthoScores.TEST_NUMBER,i);
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,fragment)
                .commit();
    }


    private boolean checkInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean networkIsPresent = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        Log.i("Main", String.valueOf(networkIsPresent));
        return networkIsPresent;
    }

    private class getUpdate extends AsyncTask<Void,Void,String[]> {

        String url = getString(R.string.url_download);
        int notificationID = 1;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i("OrthoScores2","Checking for update");
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            HTTPHelper sh = new HTTPHelper();

            String jsonString = sh.makeServiceCall(url);
            String[] tagName = new String[2];

            if(jsonString != null){
                try{
                    JSONObject jsonObject = new JSONObject(jsonString);

                    Log.i("Main",jsonObject.getString("tag_name"));
                    tagName[0] = jsonObject.getString("tag_name");
                    JSONArray assets = jsonObject.getJSONArray("assets");
                    JSONObject c = assets.getJSONObject(0);
                    tagName[1] = c.getString("browser_download_url");

                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
            return tagName;
        }

        @Override
        protected void onPostExecute(String[] s) {
            super.onPostExecute(s);
            try{
                String versionInstalled = getPackageManager().getPackageInfo(getPackageName(),0).versionName;
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String ignoreTag = sp.getString(OrthoScores.IGNORE_TAG,null);
                boolean downloadNow = sp.getBoolean("auto_install",false);
                Log.i("Main", "Ignoring " + ignoreTag);

                if(!s[0].equals(versionInstalled) && !s[0].equals(ignoreTag)) {
                    Intent updateIntent = new Intent(getApplicationContext(),AboutActivity.class);
                    updateIntent.putExtra(OrthoScores.NOTIFICATION,true);

                    PendingIntent updatePI =  PendingIntent.getActivity(getApplicationContext(),
                            0,
                            updateIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    Intent downloadIntent = new Intent();
                    downloadIntent.setAction(OrthoScores.DOWNLOAD_UPDATE);
                    downloadIntent.putExtra(OrthoScores.TAG_OR_URL,s[1]);
                    downloadIntent.putExtra(OrthoScores.NOTIFICATION_ID,notificationID);

                    PendingIntent downloadPI = PendingIntent.getBroadcast(getApplicationContext(),
                            1,
                            downloadIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    Intent ignoreIntent = new Intent();
                    ignoreIntent.setAction(OrthoScores.IGNORE_UPDATE);
                    ignoreIntent.putExtra(OrthoScores.TAG_OR_URL,s[0]);
                    ignoreIntent.putExtra(OrthoScores.NOTIFICATION_ID,notificationID);

                    PendingIntent ignorePI = PendingIntent.getBroadcast(getApplicationContext(),
                            2,
                            ignoreIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    NotificationManager notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    NotificationCompat.Builder updateNotification = new NotificationCompat.Builder(getApplicationContext());

                    Log.i("Main", String.valueOf(downloadNow));

                    if(!downloadNow) {
                        updateNotification.setSmallIcon(R.drawable.ic_notifications_black_24dp)
                                .setContentTitle("Update Available")
                                .setContentText("Version: " + s[0])
                                .setContentIntent(updatePI)
                                .addAction(0,"DOWNLOAD",downloadPI)
                                .addAction(0,"IGNORE",ignorePI)
                                .setAutoCancel(true);
                    }else{
                        Uri address = Uri.parse(s[1]);
                        long downloadID = downloadUpdate(address);
                        updateNotification.setSmallIcon(R.drawable.ic_notifications_black_24dp)
                                .setAutoCancel(true)
                                .setContentText("Downloading update apk file version: " + s[0])
                                .setContentTitle("Downloading...");

                        Log.i("Main", "Downloading " + String.valueOf(downloadID));
                    }

                    notifManager.notify(notificationID,updateNotification.build());
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        private long downloadUpdate(Uri address) {
            String destination = OrthoScores.DESTINATION;
            Uri dest = Uri.parse("file://" + destination);

            File file = new File(String.valueOf(dest));
            if(file.exists())file.delete();

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(address);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            request.setDestinationUri(dest);

            return manager.enqueue(request);
        }
    }
}
