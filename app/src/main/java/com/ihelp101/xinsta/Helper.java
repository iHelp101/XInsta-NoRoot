package com.ihelp101.xinsta;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class Helper {

    static boolean getProfileSetting(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("com.ihelp101.xinsta", Context.MODE_PRIVATE);

        return prefs.getBoolean(context.getResources().getString(R.string.disable_profile), false);
    }

    static String getSaveLocation(Context context, String saveName) {
        SharedPreferences prefs = context.getSharedPreferences("com.ihelp101.xinsta", Context.MODE_PRIVATE);

        return prefs.getString(saveName, "Instagram").replace("file://", "").replaceAll("%20", " ");
    }

    static void setSetting(Context context, String prefName, String prefData) {
        SharedPreferences.Editor editPrefs = context.getSharedPreferences("com.ihelp101.xinsta", Context.MODE_PRIVATE).edit();
        editPrefs.putString(prefName, prefData);
        editPrefs.commit();
    }

    static void setSetting(Context context, String prefName, boolean prefData) {
        SharedPreferences.Editor editPrefs = context.getSharedPreferences("com.ihelp101.xinsta", Context.MODE_PRIVATE).edit();
        editPrefs.putBoolean(prefName, prefData);
        editPrefs.commit();
    }

    static String convertStreamToString(InputStream is) throws UnsupportedEncodingException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

}
