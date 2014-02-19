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

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;

public class DynamicListActivity extends ListActivity {

    /**
     * Override getAssets() with our new asset manager only if the app is running
     * as a dynamic app. Otherwise run normally
     */
    @Override
    public AssetManager getAssets() {
        if (!Evolve.isDynamic()) return super.getAssets();
        if (Evolve.debugMode()) System.out.println("ASSETS");
        if (Evolve.assetManager == null) return super.getAssets();
        return Evolve.assetManager;
    }

    /**
     * Override getResources() with our new Resources only if the app is running
     * as a dynamic app. Otherwise run normally
     */
    @Override
    public Resources getResources() {
        if (!Evolve.isDynamic()) return super.getResources();
        if (Evolve.debugMode()) System.out.println("RESOURCES");
        if (Evolve.resources == null) {
            if (Evolve.assetManager == null) {
                if (Evolve.debugMode()) System.out.println("NULL RESOURCES");
                return super.getResources();
            }
            Evolve.resources = new Resources(Evolve.assetManager, null, null);
        }
        return Evolve.resources;
    }

    /**
    * Override startActivityForResult if the app is running as a dynamic app
    */
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (!Evolve.isDynamic())  {
            super.startActivityForResult(intent, requestCode);
            return;
        }
        String className = intent.getComponent().getClassName();
        if (Evolve.debugMode()) System.out.println("The class is " + className);
        intent.setClass(getApplicationContext(), Evolve.getDummy(className));
        super.startActivityForResult(intent, requestCode);
    }

}

