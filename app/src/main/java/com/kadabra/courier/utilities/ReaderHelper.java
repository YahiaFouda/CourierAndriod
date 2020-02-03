package com.kadabra.courier.utilities;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public  class ReaderHelper {

    private ArrayList fArrayList;

    public void Search(File dir, String filePattern, String fileName) {

        fArrayList = new ArrayList();
        Log.d("check",
                "Environment.getExternalStorageDirectory()------"
                        + dir.getName());

        File FileList[] = dir.listFiles();
        Log.d("check",
                "filelist length---- "
                        + FileList.length);

        if (FileList != null) {
            for (int i = 0; i < FileList.length; i++) {

                if (FileList[i].isDirectory()) {
                    Search(FileList[i], filePattern, fileName);
                } else {

                    Log.d("check",
                            "for check from .pdf---- "
                                    + FileList[i].getName());
                    if (FileList[i].getName().equals(fileName)) {
                        // here you have that file.
                        File file = new File(FileList[i].getPath());
                        try {
                            readFile(file);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }


        }

    }


    public static String readFile(File filename) throws Exception {
        StringBuilder content = null;
        try {
            content = new StringBuilder();
            FileInputStream fis = new FileInputStream(filename);
            DataInputStream in = new DataInputStream(fis);
            BufferedReader br =
                    new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                content.append(strLine);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }
}