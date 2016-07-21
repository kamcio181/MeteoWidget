package com.kaszubski.kamil.meteowidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class Utils {
    private static final String TAG = "Utils";
    private static Toast toast;
    private static ConnectivityManager connectivityManager;
    private static SharedPreferences preferences;
    private static ImagesDownloaderTask task;

    public static void showToast(Context context, String message){
        if(toast != null)
            toast.cancel();
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    private static void saveDate(Context context) {
        if(preferences == null)
            preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            preferences.edit().putString(Constants.LAST_UPDATE, String.format("%1$td.%1$tm.%1$tY", Calendar.getInstance())).apply();
    }

    public static void updateAllWidgets(Context context){
        WidgetProvider widgetProvider = new WidgetProvider();
        ComponentName componentName = new ComponentName(context, WidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        widgetProvider.onUpdate(context, appWidgetManager, appWidgetManager.getAppWidgetIds(componentName));
    }

    public interface OnRefreshTaskFinish {
        void onRefreshTaskFinished(Bitmap[] bitmaps);
    }

    public static void downloadGraphs(Context context, OnRefreshTaskFinish listener){
        if(connectivityManager == null)
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected()) {
            if (task != null && task.getStatus().equals(AsyncTask.Status.RUNNING))
                task.cancel(true);

            showToast(context, context.getString(R.string.downloading) + "...");
            task = new ImagesDownloaderTask(context, listener);
            task.execute();
        } else
            showToast(context, context.getString(R.string.check_internet_connection));
    }

    private static class ImagesDownloaderTask extends AsyncTask<Void, Void, Boolean> {
        private Context context;
        private OnRefreshTaskFinish listener;
        private Bitmap[] bitmaps;

        public ImagesDownloaderTask(Context context, OnRefreshTaskFinish listener) {
            this.context = context;
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            String date = String.format("%1$tY%1$tm%1$td", Calendar.getInstance()); // if you want to allow choose cities use array list with dynamically added desired city numbers
            Bitmap bitmap;
            for(int i = 0; i < Constants.CITY_URL.length; i++){
                bitmap = downloadBitmap(Constants.URL_CONST + date + Constants.CITY_URL[i]);
                if (isCancelled() || bitmap == null || bitmap.getWidth() < 400 || bitmap.getHeight() < 400) {
                    bitmaps = null;
                    return false;
                } else {
                    if(bitmaps == null)
                        bitmaps = new Bitmap[Constants.CITY_URL.length];

                    bitmaps[i] = bitmap;
                    splitAndSaveBitmaps(context, bitmap, i);
                }
            }
            saveDate(context);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            showToast(context, result? context.getString(R.string.graphs_refreshed) : context.getString(R.string.refresh_failed));
            if(listener != null)
                listener.onRefreshTaskFinished(bitmaps);
            else if(result)
                updateAllWidgets(context);
        }
    }

    private static Bitmap downloadBitmap(String url) {
        HttpURLConnection urlConnection = null;
        try {
            URL uri = new URL(url);
            urlConnection = (HttpURLConnection) uri.openConnection();

            int statusCode = urlConnection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                return null;
            }

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream != null) {

                return BitmapFactory.decodeStream(inputStream);
            }
        } catch (Exception e) {
            Log.d("URLCONNECTIONERROR", e.toString());
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            Log.w("ImageDownloader", "Error downloading image from " + url);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();

            }
        }
        return null;
    }

    private static void splitAndSaveBitmaps(Context context, Bitmap bitmap, int city){
        saveBitmapToFile(context, bitmap, String.valueOf(city)); //save in background
        Bitmap[] columns = new Bitmap[5];
        Bitmap[][] graphParts = new Bitmap[5][4];

        columns[0] = Bitmap.createBitmap(bitmap, 0, 0, Constants.COLUMN_WIDTHS[0], bitmap.getHeight()); //Legend
        columns[1] = Bitmap.createBitmap(bitmap, Constants.COLUMN_WIDTHS[0], 0, //Values
                Constants.COLUMN_WIDTHS[1], bitmap.getHeight());
        columns[2] = Bitmap.createBitmap(bitmap, Constants.COLUMN_WIDTHS[0] +
                Constants.COLUMN_WIDTHS[1] + 2, 0, Constants.COLUMN_WIDTHS[3], bitmap.getHeight()); //Today
        columns[3] = Bitmap.createBitmap(bitmap, Constants.COLUMN_WIDTHS[0] +
                Constants.COLUMN_WIDTHS[1] + Constants.COLUMN_WIDTHS[2] + 13, 0, //Tomorrow
                Constants.COLUMN_WIDTHS[3], bitmap.getHeight());
        columns[4] = Bitmap.createBitmap(bitmap, Constants.COLUMN_WIDTHS[0] +
                Constants.COLUMN_WIDTHS[1] + Constants.COLUMN_WIDTHS[2] +
                Constants.COLUMN_WIDTHS[3] + 25, 0, Constants.COLUMN_WIDTHS[4], bitmap.getHeight());//Next day

        for(int i = 0; i< graphParts.length; i++) {
            graphParts[i][0] = Bitmap.createBitmap(columns[i], 0, 30, columns[i].getWidth(), 25);
            saveBitmapToFile(context, graphParts[i][0], "" + city + i +0);
            graphParts[i][1] = Bitmap.createBitmap(columns[i], 0, 55, columns[i].getWidth(), 85);
            saveBitmapToFile(context, graphParts[i][1], "" + city + i +1);
            graphParts[i][2] = Bitmap.createBitmap(columns[i], 0, 140, columns[i].getWidth(), 89);
            saveBitmapToFile(context, graphParts[i][2], "" + city + i +2);
            graphParts[i][3] = Bitmap.createBitmap(columns[i], 0, 312, columns[i].getWidth(), 88);
            saveBitmapToFile(context, graphParts[i][3], "" + city + i +3);
        }
    }

    private static void saveBitmapToFile(Context context, Bitmap bitmap, String name){
        File file = new File(context.getCacheDir(), name + Constants.IMAGE_EXTENSION);
        Log.e(TAG, "savePath " + file.getAbsolutePath());
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
