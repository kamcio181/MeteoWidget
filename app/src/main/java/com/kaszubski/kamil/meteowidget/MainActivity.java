package com.kaszubski.kamil.meteowidget;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{
    private static final String TAG = "MainActivity";

    private ImageView imageView, imageView2;
    private CheckBox checkBox, checkBox2, checkBox3, checkBox4;
    private SharedPreferences preferences;
    private boolean prefsOrBitmapWasChanged;
    private ConnectivityManager connectivityManager;
    private boolean daySaved = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        imageView = (ImageView) findViewById(R.id.imageView);
        imageView2 = (ImageView) findViewById(R.id.imageView2);
        checkBox = (CheckBox) findViewById(R.id.checkBox);
        checkBox2 = (CheckBox) findViewById(R.id.checkBox2);
        checkBox3 = (CheckBox) findViewById(R.id.checkBox3);
        checkBox4 = (CheckBox) findViewById(R.id.checkBox4);
        checkBox.setOnCheckedChangeListener(this);
        checkBox2.setOnCheckedChangeListener(this);
        checkBox3.setOnCheckedChangeListener(this);
        checkBox4.setOnCheckedChangeListener(this);

        checkBox.setChecked(preferences.getBoolean(Constants.TEMPERATURE, true));
        checkBox2.setChecked(preferences.getBoolean(Constants.FALL, true));
        checkBox3.setChecked(preferences.getBoolean(Constants.WIND, true));
        checkBox4.setChecked(preferences.getBoolean(Constants.SHOW_LEGEND, true));

        if(!loadBitmapFromFile(Constants.WARSAW) || !loadBitmapFromFile(Constants.LODZ))
            refresh();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(prefsOrBitmapWasChanged){
            Utils.updateAllWidgets(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        prefsOrBitmapWasChanged = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.action_refresh:
                refresh();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean loadBitmapFromFile(int city){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Log.e(TAG, "loadPath " + getCacheDir().getPath() +"/" + city + ".jpg");
        Bitmap bitmap = BitmapFactory.decodeFile(getCacheDir().getPath() + "/" + city + ".jpg", options);
        if(bitmap == null)
            return false;
        setImageViewBitmap(bitmap, city);
        return true;
    }

    private void refresh(){
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected()) {
            Utils.downloadGraph(this, Constants.WARSAW, new Utils.OnRefreshTaskFinish() {
                @Override
                public void onRefreshTaskFinished(Bitmap bitmap) {
                    if (bitmap != null) {
                        setImageViewBitmap(bitmap, Constants.WARSAW);
                        saveDate();
                    }
                }
            });
            Utils.downloadGraph(this, Constants.LODZ, new Utils.OnRefreshTaskFinish() {
                @Override
                public void onRefreshTaskFinished(Bitmap bitmap) {
                    if (bitmap != null) {
                        setImageViewBitmap(bitmap, Constants.LODZ);
                        saveDate();
                    }
                }
            });
        } else
            Utils.showToast(this, getString(R.string.check_internet_connection));
    }

    private void saveDate() {
        if (!daySaved) {
            preferences.edit().putString(Constants.LAST_UPDATE, String.format("%1$td.%1$tm.%1$tY", Calendar.getInstance())).apply();
            daySaved = true;
        }
    }

    private void setImageViewBitmap(Bitmap bitmap, int city){
        prefsOrBitmapWasChanged = true;
        switch (city){
            case Constants.WARSAW:
                imageView.setImageBitmap(bitmap);
                break;
            case Constants.LODZ:
                imageView2.setImageBitmap(bitmap);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.checkBox:
                if(!isChecked && !isAnyCheckBoxChecked()){
                    buttonView.setChecked(true);
                } else {
                    preferences.edit().putBoolean(Constants.TEMPERATURE, isChecked).apply();
                    prefsOrBitmapWasChanged = true;
                }

//                enabledParts[1] = isChecked;
//                new ImageMarge().execute();
                break;
            case R.id.checkBox2:
                if(!isChecked && !isAnyCheckBoxChecked()){
                    buttonView.setChecked(true);
                } else {
                    preferences.edit().putBoolean(Constants.FALL, isChecked).apply();
                    prefsOrBitmapWasChanged = true;
                }
                break;
            case R.id.checkBox3:
                if(!isChecked && !isAnyCheckBoxChecked()){
                    buttonView.setChecked(true);
                } else {
                    preferences.edit().putBoolean(Constants.WIND, isChecked).apply();
                    prefsOrBitmapWasChanged = true;
                }
                break;
            case R.id.checkBox4:
                preferences.edit().putBoolean(Constants.SHOW_LEGEND, isChecked).apply();
                prefsOrBitmapWasChanged = true;
                break;
        }
    }

    private boolean isAnyCheckBoxChecked(){
        if(checkBox.isChecked() || checkBox2.isChecked() || checkBox3.isChecked())
            return true;

        Utils.showToast(this, getString(R.string.one_graph_has_to_be_selected_at_least));
        return false;
    }
}
