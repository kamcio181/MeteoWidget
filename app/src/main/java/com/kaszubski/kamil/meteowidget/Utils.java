package com.kaszubski.kamil.meteowidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    public static void showToast(Context context, String message){
        if(toast != null)
            toast.cancel();
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void updateAllWidgets(Context context){
        WidgetProvider widgetProvider = new WidgetProvider();
        ComponentName componentName = new ComponentName(context, WidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        widgetProvider.onUpdate(context, appWidgetManager, appWidgetManager.getAppWidgetIds(componentName));
    }

    public interface OnRefreshTaskFinish {
        void onRefreshTaskFinished(Bitmap bitmap);
    }

    public static void downloadGraph(Context context, int city, OnRefreshTaskFinish listener){
        Utils.showToast(context, context.getString(R.string.downloading) + "...");
        new ImageDownloaderTask(context, city, listener).execute(getUrl(city));
    }

    public static String getUrl(int city){
        String date = String.format("%1$tY%1$tm%1$td", Calendar.getInstance());
        switch (city){
            default:
            case Constants.WARSAW:
                return  "http://www.meteo.pl/um/metco/mgram_pict.php?ntype=0u&fdate=" + date + "00&row=406&col=250&lang=pl"; //TODO other hours 
            case Constants.LODZ:
                return  "http://www.meteo.pl/um/metco/mgram_pict.php?ntype=0u&fdate=" + date + "00&row=418&col=223&lang=pl";
        }
    }

    private static class ImageDownloaderTask extends AsyncTask<String, Void, Bitmap> {
        private Context context;
        private int city;
        private OnRefreshTaskFinish listener;

        public ImageDownloaderTask(Context context, int city, OnRefreshTaskFinish listener) {
            this.context = context;
            this.city = city;
            this.listener = listener;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = downloadBitmap(params[0]);

            if (isCancelled() || bitmap == null) {
                return null;
            } else {
                splitAndSaveBitmaps(context, bitmap, city);
                return bitmap;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            showToast(context, bitmap != null? context.getString(R.string.graph_refreshed) : context.getString(R.string.refresh_failed));

            if(listener != null)
                listener.onRefreshTaskFinished(bitmap);
            else if(bitmap != null)
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
        Bitmap[] dayGraphs = new Bitmap[4];
        Bitmap[][] graphParts = new Bitmap[4][4];
        dayGraphs[0] = Bitmap.createBitmap(bitmap, 0, 0, Constants.DAY_WIDTHS[0], bitmap.getHeight());
        dayGraphs[1] = Bitmap.createBitmap(bitmap, Constants.DAY_WIDTHS[0]+2, 0, Constants.DAY_WIDTHS[1], bitmap.getHeight());
        dayGraphs[2] = Bitmap.createBitmap(bitmap, Constants.DAY_WIDTHS[0] + Constants.DAY_WIDTHS[1]+13, 0, Constants.DAY_WIDTHS[2], bitmap.getHeight());
        dayGraphs[3] = Bitmap.createBitmap(bitmap, Constants.DAY_WIDTHS[0] + Constants.DAY_WIDTHS[1]+25 + Constants.DAY_WIDTHS[2], 0, Constants.DAY_WIDTHS[3], bitmap.getHeight());

        for(int i = 0; i< graphParts.length; i++) {
            graphParts[i][0] = Bitmap.createBitmap(dayGraphs[i], 0, 30, dayGraphs[i].getWidth(), 25);
            saveBitmapToFile(context, graphParts[i][0], "" + city + i +0);
            graphParts[i][1] = Bitmap.createBitmap(dayGraphs[i], 0, 55, dayGraphs[i].getWidth(), 85);
            saveBitmapToFile(context, graphParts[i][1], "" + city + i +1);
            graphParts[i][2] = Bitmap.createBitmap(dayGraphs[i], 0, 140, dayGraphs[i].getWidth(), 89);
            saveBitmapToFile(context, graphParts[i][2], "" + city + i +2);
            graphParts[i][3] = Bitmap.createBitmap(dayGraphs[i], 0, 312, dayGraphs[i].getWidth(), 88);
            saveBitmapToFile(context, graphParts[i][3], "" + city + i +3);
        }
    }

    private static void saveBitmapToFile(Context context, Bitmap bitmap, String name){
        File file = new File(context.getCacheDir(), name + ".jpg");
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
