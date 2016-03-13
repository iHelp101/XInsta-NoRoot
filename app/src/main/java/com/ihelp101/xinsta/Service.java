package com.ihelp101.xinsta;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class Service extends android.app.Service{

    ClipboardManager mClipboard;
    Context mContext = this;
    ArrayList apiList = new ArrayList();
    int id = 1;
    int apiInt = 0;
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    String linkToDownload;
    String clipText;
    String continueDownload;
    String fileName;
    String notifTitle;
    String API_Key;
    String Author;
    String MediaID;
    String JSONInfo;
    String SAVE = "Instagram";

    class DownloadFileAsync extends AsyncTask<Object, String, String> {
        @Override
        protected String doInBackground(Object... aurl) {
            int count;
            try {

                SAVE = SAVE + "/" + fileName;
                SAVE = SAVE.replace("%20", " ");

                notifTitle = notifTitle.substring(0, 1).toUpperCase() + notifTitle.substring(1);

                mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                mBuilder = new NotificationCompat.Builder(mContext);
                mBuilder.setContentTitle(notifTitle)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentText(ResourceHelper.getString(mContext, R.string.DownloadDots));
                mNotifyManager.notify(id, mBuilder.build());

                URL url = new URL(linkToDownload);
                URLConnection conexion = url.openConnection();
                conexion.connect();

                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = new FileOutputStream(SAVE);

                byte data[] = new byte[1024];

                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
            } catch (Exception e) {
                Error.setError("Download Error: " +e);
                mBuilder.setContentTitle(fileName);
                mBuilder.setContentText(ResourceHelper.getString(mContext, R.string.Download_Failed));
                mBuilder.setTicker(ResourceHelper.getString(mContext, R.string.Download_Failed));
                mBuilder.setSmallIcon(R.drawable.ic_launcher);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String unused) {
            Toast(ResourceHelper.getString(mContext, R.string.Download_Completed));

            mBuilder.setContentTitle(notifTitle);
            mBuilder.setContentText(ResourceHelper.getString(mContext, R.string.Download_Completed));
            mBuilder.setTicker(ResourceHelper.getString(mContext, R.string.Download_Completed));
            mBuilder.setSmallIcon(R.drawable.ic_launcher);
            mBuilder.setAutoCancel(true);

            Intent notificationIntent = new Intent();
            notificationIntent.setAction(Intent.ACTION_VIEW);

            File file = new File(SAVE);
            if (fileName.contains("jpg")) {
                notificationIntent.setDataAndType(Uri.fromFile(file), "image/*");
            } else {
                notificationIntent.setDataAndType(Uri.fromFile(file), "video/*");
            }
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentIntent(contentIntent);
            mNotifyManager.notify(id, mBuilder.build());

            MediaScannerConnection.scanFile(mContext,
                    new String[]{SAVE}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {

                        }
                    });
        }
    }

    @SuppressLint("NewApi")
    CharSequence coerceToText(ClipData.Item item) {
        // If this Item has an explicit textual value, simply return that.
        CharSequence text = item.getText();
        if (text != null) {
            return text;
        }

        // If this Item has a URI value, try using that.
        Uri uri = item.getUri();
        if (uri != null) {

            // First see if the URI can be opened as a plain text stream
            // (of any sub-type). If so, this is the best textual
            // representation for it.
            FileInputStream stream = null;
            try {
                // Ask for a stream of the desired type.
                AssetFileDescriptor descr = getContentResolver()
                        .openTypedAssetFileDescriptor(uri, "text/*", null);
                stream = descr.createInputStream();
                InputStreamReader reader = new InputStreamReader(stream,
                        "UTF-8");

                // Got it... copy the stream into a local string and return it.
                StringBuilder builder = new StringBuilder(128);
                char[] buffer = new char[8192];
                int len;
                while ((len = reader.read(buffer)) > 0) {
                    builder.append(buffer, 0, len);
                }
                return builder.toString();

            } catch (FileNotFoundException e) {
                // Unable to open content URI as text... not really an
                // error, just something to ignore.

            } catch (IOException e) {
                // Something bad has happened.
                return e.toString();

            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                }
            }

            // If we couldn't open the URI as a stream, then the URI itself
            // probably serves fairly well as a textual representation.
            return uri.toString();
        }

        // Finally, if all we have is an Intent, then we can just turn that
        // into text. Not the most user-friendly thing, but it's something.
        Intent intent = item.getIntent();
        if (intent != null) {
            return intent.toUri(Intent.URI_INTENT_SCHEME);
        }

        // Shouldn't get here, but just in case...
        return "";
    }

    ClipboardManager.OnPrimaryClipChangedListener mPrimaryChangeListener = new ClipboardManager.OnPrimaryClipChangedListener() {
        public void onPrimaryClipChanged() {
            final ClipData.Item item = mClipboard.getPrimaryClip().getItemAt(0);
            clipText  = coerceToText(item).toString();

            if (android.os.Build.VERSION.SDK_INT >= 23) {
                if (ContextCompat.checkSelfPermission(Service.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED | ContextCompat.checkSelfPermission(Service.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Intent myIntent = new Intent(Service.this, Permission.class);
                    myIntent.putExtra("link", clipText);
                    myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(myIntent);
                } else {
                    continueDownload();
                }
            } else {
                continueDownload();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        mClipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        mClipboard.addPrimaryClipChangedListener(mPrimaryChangeListener);
    }

    void continueDownload() {
        Random r = new Random();
        int i1 = r.nextInt(80000000 - 65) + 65;
        id = i1;

        if (clipText.contains("instagram.com/p/")) {
            try {
                apiList.clear();
                apiList.add("678ed777a09f486298a84f25240f798d");
                apiList.add("6e28050761c649d497534a54af1ae653");

                checkAPIKey(apiList.get(0).toString());
                continueDownload = "Feed";
            } catch (Exception e) {

            }
        }

        if (clipText.contains("instagram.com") && !clipText.contains("instagram.com/p/")) {
            try {
                apiList.clear();
                apiList.add("678ed777a09f486298a84f25240f798d");
                apiList.add("6e28050761c649d497534a54af1ae653");

                if (!Helper.getProfileSetting(getApplicationContext())) {
                    checkAPIKey(apiList.get(0).toString());
                    continueDownload = "Profile";
                }
            } catch (Exception e) {
            }
        }
    }

    void checkAPIKey(final String API_KEY) {
        Thread getMediaID = new Thread() {
            public void run() {
                try {
                    URL obj = new URL("https://api.instagram.com/v1/media/1156714854957031582_528817151?client_id=" +API_KEY);
                    URLConnection conn = obj.openConnection();
                    Map<String, List<String>> map = conn.getHeaderFields();


                    for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                        if (entry.getKey() != null && entry.getKey().equals("X-Ratelimit-Remaining")) {
                            String rateLimit = entry.getValue().toString();
                            rateLimit = rateLimit.replace("[","");
                            rateLimit = rateLimit.replace("]","");
                            if (Integer.parseInt(rateLimit) < 10) {
                                if (apiInt != (apiList.size() - 1)) {
                                    apiInt = apiInt + 1;
                                    checkAPIKey(apiList.get(apiInt).toString());
                                }
                            } else {
                                API_Key = apiList.get(apiInt).toString();
                                apiInt = 0;

                                if (continueDownload.equals("Profile")) {
                                    downloadProfile();
                                } else {
                                    downloadFeed();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Info: " + e);
                }
            }
        };

        getMediaID.start();
    }

    void downloadFeed() {
        mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setContentTitle("Fetching Feed Image")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentText(ResourceHelper.getString(mContext, R.string.DownloadDots));
        mNotifyManager.notify(id, mBuilder.build());

        Thread getMediaID = new Thread() {
            public void run() {
                try {
                    try {
                        URL u = new URL("https://api.instagram.com/publicapi/oembed/?url=" + clipText);
                        URLConnection c = u.openConnection();
                        c.connect();

                        InputStream inputStream = c.getInputStream();


                        JSONInfo = Helper.convertStreamToString(inputStream);
                    } catch (Exception e) {
                        Error.setError("Feed Download Fetch Failed: " +e);
                        JSONInfo = "Nope";
                    }

                    JSONObject myjson = new JSONObject(JSONInfo);
                    MediaID = myjson.getString("media_id");
                    Author = myjson.getString("author_name");
                    String imageVideoCheck = myjson.getString("html");

                    linkToDownload = clipText + "media?size=l";
                    fileName = Author + "_" + MediaID + ".jpg";
                    notifTitle = Author + "'s Photos";

                    SAVE = Helper.getSaveLocation(getApplicationContext(), getResources().getString(R.string.Image));

                    if (imageVideoCheck.contains("A photo posted by")) {
                        try {
                            new DownloadFileAsync().execute();
                        } catch (Exception e) {
                        }
                    } else {
                        try {
                            downloadVideo();
                        } catch (Exception e) {
                        }
                    }
                } catch (Exception e) {
                    Error.setError("Feed Download Failed: " + e);
                    return;
                }
            }
        };
        getMediaID.start();
    }

    void downloadProfile() {
        mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setContentTitle("Fetching Profile Image")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentText(ResourceHelper.getString(mContext, R.string.DownloadDots));
        mNotifyManager.notify(id, mBuilder.build());

        Thread getMediaID = new Thread() {
            public void run() {
                String userName = clipText.split("instagram.com/")[1];
                userName = userName.replace("/", "");

                Author = userName;

                try {
                    URL u = new URL("https://api.instagram.com/v1/users/search?q="+userName+"&client_id=" +API_Key);
                    URLConnection c = u.openConnection();
                    c.connect();

                    InputStream inputStream = c.getInputStream();

                    JSONInfo = Helper.convertStreamToString(inputStream);
                } catch (Exception e) {
                    Error.setError("Profile Download Fetch Failed: " +e);
                    JSONInfo = "Nope";
                }

                System.out.println("Info: " +JSONInfo);

                try {
                    JSONObject myjson = new JSONObject(JSONInfo);
                    JSONArray jsonArray = myjson.getJSONArray("data");
                    JSONObject dataJSON = jsonArray.getJSONObject(0);

                    MediaID = dataJSON.getString("id");
                    linkToDownload = dataJSON.getString("profile_picture");
                    linkToDownload = linkToDownload.replace("150x150", "");

                    fileName = Author + "_" + MediaID + "-Profile.jpg";
                    notifTitle = Author + "'s Profile Picture";

                    SAVE = Helper.getSaveLocation(getApplicationContext(), getResources().getString(R.string.Profile));

                    new DownloadFileAsync().execute();
                } catch (Exception e) {
                    Error.setError("Profile Download Failed: " + e);

                    mBuilder.setContentTitle(notifTitle);
                    mBuilder.setContentText(ResourceHelper.getString(mContext, R.string.Download_Failed));
                    mBuilder.setTicker(ResourceHelper.getString(mContext, R.string.Download_Failed));
                    mBuilder.setSmallIcon(R.drawable.ic_launcher);
                    mBuilder.setAutoCancel(true);
                    mNotifyManager.notify(id, mBuilder.build());
                }
            }
        };
        getMediaID.start();
    }

    void downloadVideo() {
        Thread downloadThreadVideo = new Thread() {
            public void run() {
                try {
                    try {
                        URL u = new URL("https://api.instagram.com/v1/media/" + MediaID + "?client_id=" +API_Key);
                        URLConnection c = u.openConnection();
                        c.connect();

                        InputStream inputStream = c.getInputStream();

                        JSONInfo = Helper.convertStreamToString(inputStream);
                    } catch (Exception e) {
                        Error.setError("Video Download Fetch Failed: " +e);
                        JSONInfo = "Nope";
                    }

                    JSONObject myjson = new JSONObject(JSONInfo);
                    String stringData = myjson.getString("data");
                    JSONObject data = new JSONObject(stringData);
                    String stringVideos = data.getString("videos");
                    JSONObject video = new JSONObject(stringVideos);
                    String stringStandard = video.getString("standard_resolution");
                    JSONObject url = new JSONObject(stringStandard);
                    linkToDownload = url.getString("url");
                    fileName = Author + "_" + MediaID + ".mp4";
                    notifTitle = Author + "'s Videos";

                    SAVE = Helper.getSaveLocation(getApplicationContext(), getResources().getString(R.string.Video));

                    new DownloadFileAsync().execute();
                } catch (Exception e) {
                    Error.setError("Video Download Failed: " +e);

                    mBuilder.setContentTitle(notifTitle);
                    mBuilder.setContentText(ResourceHelper.getString(mContext, R.string.Download_Failed));
                    mBuilder.setTicker(ResourceHelper.getString(mContext, R.string.Download_Failed));
                    mBuilder.setSmallIcon(R.drawable.ic_launcher);
                    mBuilder.setAutoCancel(true);
                    mNotifyManager.notify(id, mBuilder.build());
                }
            }
        };
        downloadThreadVideo.start();
    }

    void Toast (String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mClipboard.removePrimaryClipChangedListener(mPrimaryChangeListener);
    }
}
