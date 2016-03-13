package com.ihelp101.xinsta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Error {


    static void setError(String status) {
        try {
            System.out.println("Info: " +status);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String time = sdf.format(new Date());

            status = time + " - " + status;

            File root = new File(Environment.getExternalStorageDirectory(), ".XInsta");
            if (!root.exists()) {
                root.mkdirs();
            }
            File file = new File(root, "Error.txt");
            BufferedWriter buf = new BufferedWriter(new FileWriter(file, true));
            buf.newLine();
            buf.append(status);
            buf.close();
        } catch (IOException e) {

        }
    }

    static void startErrorLog(String status) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String time = sdf.format(new Date());

            status = time + " - " + status;

            File root = new File(Environment.getExternalStorageDirectory(), ".XInsta");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, "Error.txt");
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(status);
            writer.flush();
            writer.close();
        } catch (IOException e) {

        }
    }
}
