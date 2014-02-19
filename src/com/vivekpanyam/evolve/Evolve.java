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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.widget.Toast;
import dalvik.system.DexClassLoader;


public class Evolve {
    public static enum OperationMode {
        TESTING_MODE, PRODUCTION_MODE
    }
    public static Context context;
    public static AssetManager assetManager;
    private static OperationMode mode = OperationMode.TESTING_MODE;
    public static Resources resources;
    private static int currentVersion = 99999;
    private static boolean debug = false;
    private static String newAPKUrl = "";
    private static String updateCheckURL = "";
    private static final String doneAPKFileName = "update_done.apk";
    private static final String newAPKFileName = "update_temp.apk";
    public static CustomClassLoader classLoader;

    /**
     * Updates these values. Must call this before checkForUpdate().
     *
     * @param updateAPKUrl The url for an updated APK file
     * (ex: http://www.vivekpanyam.com/app.apk)
     *
     * @param checkForUpdatesURL The url to check for updates. This page should
     * just be a plain text version number.
     * (ex: http://www.vivekpanyam.com/update.txt)
     *
     */
    public static void setValues(String updateAPKUrl, String checkForUpdatesURL) {
        newAPKUrl = updateAPKUrl;
        updateCheckURL = checkForUpdatesURL;
    }

    public static File getTempAPKFile() {
        return new File(context.getFilesDir(), newAPKFileName);
    }

    public static void doneDownloadingTempAPK() {
        getTempAPKFile().renameTo(new File(context.getFilesDir(), doneAPKFileName));
    }

    /**
     * Check if there is an update and if there is one, downloads it. It will
     * resume an interrupted download. Call after setValues.
     */
    public static void checkForUpdate() {
        Utils.checkForUpdate(updateCheckURL, currentVersion);
    }

    /**
     * Set Evolve to debug mode
     */
    public static void setDebug(boolean d) {
        debug = d;
    }

    /**
     * Check if Evolve is in debug mode
     * @return whether evolve is in debug mode
     */
    public static boolean debugMode() {
        return debug;
    }

    /**
     * Where to save a new APK file (a downloaded update)
     *
     * @return
     * @throws FileNotFoundException
     */
    public static FileOutputStream getNewAPKOutputStream(int filemode) throws FileNotFoundException {
        return context.openFileOutput(newAPKFileName, filemode);
    }

    public static String getAPKUrl() {
        return newAPKUrl;
    }

    public static boolean isDynamic() {
        return mode == OperationMode.PRODUCTION_MODE;
    }

    /**
     * Create a new AssetManager via reflection. This is used as the AssetManager
     * for the application.
     *
     * THIS METHOD IS CALLED IN THE BASE DYNAMIC APP.
     *
     * @param path Path to new APK
     */
    public static void loadResources(String path) {
        try {
            //layouts = new HashMap<Integer, String>();
            assetManager = AssetManager.class.getConstructor().newInstance();

            Class<?>[] parameterTypes = new Class[1];
            parameterTypes[0] = java.lang.String.class;
            Method m = assetManager.getClass().getDeclaredMethod("addAssetPath", parameterTypes);
            m.setAccessible(true);

            Object[] parameters = new Object[1];
            parameters[0] = path;
            m.invoke(assetManager, parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize the application as a dynamic application.
     * If this method is not called, the app behaves normally.
     *
     * THIS METHOD IS CALLED IN THE BASE DYNAMIC APP
     *
     * @param context The application context
     * @param version The version of this APK file.
     * @param dcl The DexClassLoader used to load assets from the new app
     */
    public static void init(Activity context, Integer version, DexClassLoader dcl) {
        Evolve.context = context;
        mode = OperationMode.PRODUCTION_MODE;
        currentVersion = version;
        Evolve.classLoader = new CustomClassLoader(dcl, context);
        Utils.setClassLoader(context, Evolve.classLoader);
        Thread.currentThread().setContextClassLoader(Evolve.classLoader);
    }

    /**
     * Function used in DynamicActivity in order to redirect startActivity
     * method calls to the correct Dummy activity (declared in the
     * AndroidManifest).
     *
     * @param className The name of the Activity class that startActivity was
     * called on.
     * @return The Dummy Activity to start
     */
    public static Class<?> getDummy(String className) {
        try{
            return Class.forName(CustomClassLoader.getDummy(className), false, Evolve.classLoader);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}

