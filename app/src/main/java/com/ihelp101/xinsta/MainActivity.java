package com.ihelp101.xinsta;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class MainActivity extends ListActivity {

    private ListView mAdapter;
    private static int FILE_CODE = 0;
    String SaveLocation = "None";
    String currentAction;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(new Intent(this, Service.class));

        mAdapter = new ListView(MainActivity.this);
        mAdapter.addSectionHeaderItem(getResources().getString(R.string.Save));
        mAdapter.addItem(getResources().getString(R.string.Image));
        mAdapter.addItem(getResources().getString(R.string.Video));
        mAdapter.addItem(getResources().getString(R.string.Profile));
        setListAdapter(mAdapter);

        final android.widget.ListView lv = (android.widget.ListView) findViewById(android.R.id.list);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                if (mAdapter.getItemViewType(position) != ListView.TYPE_SEPARATOR) {

                    currentAction = mAdapter.getItem(position);
                    checkPermission();
                }
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        if (Helper.getProfileSetting(getApplicationContext())) {
            menu.findItem(R.id.disable_profile).setChecked(true);
        } else {
            menu.findItem(R.id.disable_profile).setChecked(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        String clicked = (String) menuItem.getTitle();

        if (clicked.equals(getResources().getString(R.string.error_log))) {
            try {
                sendErrorLog();
            } catch (Exception e) {
            }
        }

        if (clicked.equals(getResources().getString(R.string.translations))) {
            try {
                sendTranslation();
            } catch (Exception e) {
            }
        }

        if (clicked.equals(getResources().getString(R.string.disable_profile))) {
            if (menuItem.isChecked()) {
                menuItem.setChecked(false);
                Helper.setSetting(getApplicationContext(), getResources().getString(R.string.disable_profile), menuItem.isChecked());
            } else {
                menuItem.setChecked(true);
                Helper.setSetting(getApplicationContext(), getResources().getString(R.string.disable_profile), menuItem.isChecked());
            }
        }

        return false;
    }

    void checkPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED | ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
                listAction();
            }
        } else {
            listAction();
        }
    }

    void listAction() {
        String saveLocation = Helper.getSaveLocation(getApplicationContext(), currentAction);

        Intent i = new Intent(com.ihelp101.xinsta.MainActivity.this, com.ihelp101.xinsta.FilePickerActivity.class);
        i.putExtra(com.ihelp101.xinsta.FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(com.ihelp101.xinsta.FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
        i.putExtra(com.ihelp101.xinsta.FilePickerActivity.EXTRA_MODE, com.ihelp101.xinsta.FilePickerActivity.MODE_DIR);
        if (!saveLocation.equals("Instagram")) {
            i.putExtra(FilePickerActivity.EXTRA_START_PATH, saveLocation);
        } else {
            i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/Instagram");
        }
        startActivityForResult(i, FILE_CODE);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 0) {
            SaveLocation = "None";
        }
        if (requestCode == FILE_CODE && resultCode == com.ihelp101.xinsta.MainActivity.RESULT_OK) {
            Uri Location = null;
            if (data.getBooleanExtra(com.ihelp101.xinsta.FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                // For JellyBean and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip = data.getClipData();

                    if (clip != null) {
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            Location = clip.getItemAt(i).getUri();
                        }
                    }
                    // For Ice Cream Sandwich
                } else {
                    ArrayList<String> paths = data.getStringArrayListExtra
                            (com.ihelp101.xinsta.FilePickerActivity.EXTRA_PATHS);

                    if (paths != null) {
                        for (String path : paths) {
                            Location = Uri.parse(path);
                        }
                    }
                }

            } else {
                Location = data.getData();
            }

            String toast;

            if (Location.toString().contains("/storage/") || Location.toString().contains("/mnt/") || Location.toString().contains("/sdcard/")) {
                toast = getResources().getString(R.string.Save_Changed);
                Helper.setSetting(getApplicationContext(), currentAction, Location.toString());
                SaveLocation = "None";
            } else {
                toast = getResources().getString(R.string.Incorrect_Location);

                String saveLocation = Helper.getSaveLocation(getApplicationContext(), currentAction);

                Intent i = new Intent(com.ihelp101.xinsta.MainActivity.this, com.ihelp101.xinsta.FilePickerActivity.class);
                i.putExtra(com.ihelp101.xinsta.FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(com.ihelp101.xinsta.FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                i.putExtra(com.ihelp101.xinsta.FilePickerActivity.EXTRA_MODE, com.ihelp101.xinsta.FilePickerActivity.MODE_DIR);
                if (!saveLocation.equals("Instagram")) {
                    i.putExtra(FilePickerActivity.EXTRA_START_PATH, saveLocation);
                } else {
                    i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/Instagram");
                }
                startActivityForResult(i, FILE_CODE);
            }

            setToast(toast);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    listAction();
                } else {
                    setToast("Permission denied. Unable to change save location.");
                }
            }
        }
    }

    void sendErrorLog() {
        String getDirectory = Environment.getExternalStorageDirectory().toString().replace("1", "0");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"XInsta@ihelp101.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "XInsta - Error Log");
        intent.putExtra(Intent.EXTRA_TEXT, "Please check out my XInsta Error Log.");
        File root = new File(getDirectory, ".XInsta");
        File file = new File(root, "Error.txt");
        Uri uri = Uri.fromFile(file);
        if (!file.exists() || !file.canRead()) {
            setToast("No Errors");
            return;
        }

        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, "Email"));
    }

    void sendTranslation() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    URL u = new URL("https://raw.githubusercontent.com/iHelp101/XInsta/master/Translate.txt");
                    URLConnection c = u.openConnection();
                    c.connect();

                    InputStream inputStream = c.getInputStream();

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"XInsta@ihelp101.com"});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "XInsta - Translation");
                    intent.putExtra(Intent.EXTRA_TEXT, Helper.convertStreamToString(inputStream));

                    startActivity(Intent.createChooser(intent, "Email"));
                } catch (Exception e) {
                    Error.setError("Translation - " +e);
                }
            }
        });

        thread.start();
    }

    void setToast(String message) {
        if (Build.VERSION.SDK_INT >= 21) {
            Snackbar.make(MainActivity.this.getListView(), message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
            TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
            if (v != null) v.setGravity(Gravity.CENTER);
            toast.show();
        }
    }
}

