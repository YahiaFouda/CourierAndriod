package com.kadabra.courier.exception;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Locale;

public class CrashHandeller implements Thread.UncaughtExceptionHandler {

    private Activity context;
    Class<?> intentClass;

    public CrashHandeller(Activity context, Class<?> intentClass) {
        this.context = context;
        this.intentClass = intentClass;
    }

    public static void deploy(Activity context, Class<?> intentClass) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandeller(context, intentClass));
    }

    private StatFs getStatFs()
    {
        File path = Environment.getDataDirectory();
        return new StatFs(path.getPath());
    }

    private long getAvailableInternalMemorySize(StatFs stat)
    {
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    private long getTotalInternalMemorySize(StatFs stat)
    {
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    }

    private void addInformation(StringBuilder message)
    {
        message.append("Locale: ").append(Locale.getDefault()).append('\n');
        try
        {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi;
            pi = pm.getPackageInfo(context.getPackageName(), 0);
            message.append("Version: ").append(pi.versionName).append('\n');
            message.append("Package: ").append(pi.packageName).append('\n');
        }
        catch ( Exception e )
        {
            Log.e("CustomExceptionHandler", "Error", e);
            message.append("Could not get Version information for ").append(context.getPackageName());
        }
        message.append("Phone Model: ").append(android.os.Build.MODEL).append('\n');
        message.append("Android Version: ").append(android.os.Build.VERSION.RELEASE).append('\n');
        message.append("Board: ").append(android.os.Build.BOARD).append('\n');
        message.append("Brand: ").append(android.os.Build.BRAND).append('\n');
        message.append("Device: ").append(android.os.Build.DEVICE).append('\n');
        message.append("Host: ").append(android.os.Build.HOST).append('\n');
        message.append("ID: ").append(android.os.Build.ID).append('\n');
        message.append("Model: ").append(android.os.Build.MODEL).append('\n');
        message.append("Product: ").append(android.os.Build.PRODUCT).append('\n');
        message.append("Type: ").append(android.os.Build.TYPE).append('\n');
        StatFs stat = getStatFs();
        message.append("Total Internal memory: ").append(getTotalInternalMemorySize(stat)).append('\n');
        message.append("Available Internal memory: ").append(getAvailableInternalMemorySize(stat)).append('\n');
    }
    public void uncaughtException(Thread thread, Throwable exception) {
        try
        {
            StringBuilder report = new StringBuilder();
            String date = android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss a", new Date()).toString();
            report.append("Error Report collected on : ").append(date).append('\n').append('\n');
            report.append("Informations :").append('\n');
            addInformation(report);
            report.append('\n').append('\n');
            report.append("Stack:\n");
            StringWriter stackTrace = new StringWriter();
            exception.printStackTrace(new PrintWriter(stackTrace));
            System.err.println(stackTrace);
            report.append(stackTrace.toString());
            report.append('\n');
            report.append("**** End of current Report ***");
         //   sendError(report);
            Intent intent = new Intent(this.context, this.intentClass);
            intent.putExtra("stackTrace", report.toString());
            this.context.startActivity(intent);
            android.os.Process.killProcess(android.os.Process.myPid());
            this.context.finish();
            System.exit(10);

            Log.e(CrashHandeller.class.getName(), "Error while sendError" + report);

        }
        catch ( Throwable ignore )
        {
            Log.e(CrashHandeller.class.getName(), "Error while sending error ", ignore);
        }

    }


}