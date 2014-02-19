/*    Copyright (C) 2014 Vivek Panyam.
 *
 *    This program is free software: you can redistribute it and/or  modify
 *    it under the terms of the GNU Affero General Public License, version 3,
 *    as published by the Free Software Foundation.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.
 *
 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.vivekpanyam.evolve;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import android.app.Activity;
import android.os.AsyncTask;
import org.apache.http.util.ByteArrayBuffer;

import android.content.Context;

public class Utils {

    /**
     * Set the ClassLoader for the running application
     *
     * @param a An activity in the currently running application
     * @param classLoader The ClassLoader used to load the new DEX file
     */

    public static void setClassLoader(Activity a, ClassLoader classLoader)
    {
        try {
            Field mMainThread = getField(Activity.class, "mMainThread");
            Object mainThread = mMainThread.get(a);
            Class<?> threadClass = mainThread.getClass();
            Field mPackages = getField(threadClass, "mPackages");

            HashMap<String,?> map = (HashMap<String,?>) mPackages.get(mainThread);
            WeakReference<?> ref = (WeakReference<?>) map.get(a.getPackageName());
            Object apk = ref.get();
            Class<?> apkClass = apk.getClass();
            Field mClassLoader = getField(apkClass, "mClassLoader");

            mClassLoader.set(apk, classLoader);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private static Field getField(Class<?> cls, String name)
    {
        for (Field field: cls.getDeclaredFields())
        {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Check for update and if there is one, download it.
     *
     * @param updateCheckURL The url to check for updates at (should return a version
     * number as defined by versionCode)
     * @param currentVersion Current version of dynamic APK as defined by
     * versionCode
     */
    public static void checkForUpdate(String updateCheckURL, int currentVersion) {
        new CheckForUpdateTask().execute(updateCheckURL, "" + currentVersion);
    }

    private static class CheckForUpdateTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... vals) {
            try{
                URL updateURL = new URL(vals[0]);
                URLConnection conn = updateURL.openConnection();
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                ByteArrayBuffer baf = new ByteArrayBuffer(50);

                int current = 0;
                while((current = bis.read()) != -1){
                    baf.append((byte)current);
                }

	            /* Convert the Bytes read to a String. */
                final String s = new String(baf.toByteArray());

	            /* Get current Version Number */
                int curVersion = Integer.valueOf(vals[1]);
                int newVersion = Integer.valueOf(s);

	            /* Is a higher version than the current already out? */
                if (newVersion > curVersion) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        protected void onPostExecute(Boolean needToUpdate) {
            if (needToUpdate)
                new DownloadFileTask().execute(Evolve.getAPKUrl());
        }

    }


    private static class DownloadFileTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... sUrl) {
            try {
                URL url = new URL(sUrl[0]);
                URLConnection connection = url.openConnection();

                int downloadMode = Context.MODE_PRIVATE;

                // TODO Make sure resuming a download works
                File f = Evolve.getTempAPKFile();
                if(f.exists()) {
                    connection.setRequestProperty("Range", "bytes="+(f.length())+"-");
                    downloadMode = Context.MODE_APPEND;
                }

                connection.connect();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = Evolve.getNewAPKOutputStream(downloadMode);
                int count;
                byte data[] = new byte[1024];
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);

                }

                output.flush();
                output.close();
                input.close();

                Evolve.doneDownloadingTempAPK();
            } catch (Exception e) {
            }
            return null;
        }
    }
}

