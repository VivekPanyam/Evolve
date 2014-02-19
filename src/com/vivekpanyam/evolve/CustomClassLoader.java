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

import android.content.Context;
import dalvik.system.DexClassLoader;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.android.DexFile;

import java.io.File;
import java.util.HashMap;

public class CustomClassLoader extends ClassLoader {

    // Keep track of the dummy classes we've generated so far
    private static HashMap<String, Class> dummyToClassMap = new HashMap<String, Class>();

    // Used as a two way map
    private static HashMap<String, String> superToDummyMap = new HashMap<String, String>();
    private static HashMap<String, String> dummyToSuperMap = new HashMap<String, String>();

    private static int currentDummy = 1;
    private Context context;


    public CustomClassLoader(ClassLoader parent, Context context) {
        super(parent);
        this.context = context;
    }

    public static String getDummy(String superClass) {
        if (superToDummyMap.containsKey(superClass)) return superToDummyMap.get(superClass);

        String name = "com.vivekpanyam.Evolve.dummy.Dummy" + (currentDummy++);
        superToDummyMap.put(superClass, name);
        dummyToSuperMap.put(name, superClass);
        return name;
    }

    @Override
    public Class findClass (String name) throws ClassNotFoundException{
        if (!name.contains("dummy")) {
            if (Evolve.debugMode()) System.out.println("THIS RETURNS NULL " + name);
            return null;
        }
        if (dummyToClassMap.containsKey(name)) return dummyToClassMap.get(name);

        try {
            final ClassPool cp = ClassPool.getDefault(context);
            CtClass cls = cp.makeClass(name);
            cls.setSuperclass(cp.get(dummyToSuperMap.get(name)));
            DexFile df = new DexFile();
            cls.writeFile(context.getFilesDir().getAbsolutePath());
            df.addClass(new File(context.getFilesDir(), name.replace('.', File.separatorChar) + ".class"));

            String temppath = File.createTempFile("tempdex",".dex").getAbsolutePath();
            df.writeFile(temppath);
            DexClassLoader classLoader = new DexClassLoader(temppath, context.getFilesDir().getAbsolutePath(), null, getClass().getClassLoader());
            Class c = classLoader.loadClass(name);
            dummyToClassMap.put(name, c);
            return c;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

