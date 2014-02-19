Evolve is a library for Android Developers that lets them deploy new versions of an app without going through Google Play or asking users to download an update. It works by using reflection and dynamic bytecode generation to "trick" Android into running new code.

### Using Evolve

The developer initially deploys a wrapper app that declares a bunch of "dummy" activities in AndroidManifest.xml

	<activity android:name="com.vivekpanyam.testapp.HomeActivity"></activity>
    <activity android:name=".dummy.Dummy1"></activity>
    <activity android:name=".dummy.Dummy2"></activity>
    <activity android:name=".dummy.Dummy3"></activity>
    <activity android:name=".dummy.Dummy4"></activity>
    ...
    
The main activity of the wrapper app may look like the following (A sample wrapper application is provided at https://github.com/VivekPanyam/EvolveWrapper).

	...
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Init App with the APK file specified
        DynamicApp.init(this, "MyActualApp.apk");

        /* Start App by calling this activity. It is the entry point for the new
         * application.
         *
         * THIS ACTIVITY MUST BE DECLARED IN THE ANDROID MANIFEST
         */
        startApp("com.vivekpanyam.testapp.HomeActivity");

        //Close this activity so the user doesn't see it when they close the app.
        finish();
    }
    ...

    
They then include Evolve in their actual app and change `Activity` to `DynamicActivity` (which is provided by Evolve) in all of their Activities. In their main activity, they should configure URLs to check for updates. For example:

	public class MainActivity extends DynamicActivity {
    
    	@Override
		protected void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
        	Evolve.setValues(
                "https://example.com/app.apk?androidVersion=" +
                        Build.VERSION.RELEASE,
                "https://example.com/updatecheck?androidVersion=" +
                        Build.VERSION.RELEASE);

        	//Runs asynchronously
        	Evolve.checkForUpdate();
			...
		}
    }
    
If the version number returned at `/updatecheck` is larger than the current version number, `/app.apk` will be downloaded in the background. The next time the app is launched, the new version will run. The Android version ( `Build.VERSION.RELEASE`) is included to allow the server to send back different values for different versions of Android.

In the initial release of the app, `MyActualApp.apk` needs to be included in the assets folder of the wrapper application. This is the version that will be run the first time the wrapper is opened.

That is all they should have to do! (**Note: Evolve is in alpha so you will probably need to do some debugging**)

### How It Works

Evolve intercepts all calls to startActivityForResult, getResources, and some other functions. When you try to start an activity, Evolve generates a dummy class and sets its superclass to the activity you want to start. For example if you call `startActivity(new Intent(this, PhotoShareActivity.class))`, Evolve will do the following:

1. Check if it has already generated a dummy class for `PhotoShareActivity`, if so it changes the Intent to start that dummy class.

2. If not, Evolve dynamically generates the bytecode for a class that looks something like this:
`public class Dummy1 extends PhotoShareActivity {}`<br>
Because of inheritance, Dummy1 has the same functionality as PhotoShareActivity. This new class is stored in a map from activities to their Dummy counterparts.

3. Evolve then changes the intent to start the appropriate Dummy class.

Evolve also uses a custom classloader and a DexClassLoader to load code from the new APK. It uses reflection to update the classloaders for the main thread and to create an AssetManager to handle resources. There are lots of intricacies dealing with Contexts, Resources, and ClassLoaders so check out the code!

**Important: Evolve is in Alpha. This means that it probably won't work without some debugging. Please find issues and submit pull requests!**

### Dependencies and Building Evolve

Evolve depends on [Javassist-Android](https://github.com/crimsonwoods/javassist-android). 

There are also Evolve binaries available on the [release page](https://github.com/VivekPanyam/Evolve/releases)

### IMPORTANT

According to the Google Play Developer Program Policies (http://play.google.com/about/developer-content-policy.html), "An app downloaded from Google Play may not modify, replace or update its own APK binary code using any method other than Google Play's update mechanism."

This means that Evolve **CANNOT** be used in apps on Google Play.

Evolve is targeted towards apps not on Google Play and Beta tests. Some of its intended use cases are enterprise apps (fix security holes without bugging all your employees) and beta tests (push updates to beta testers instead of waiting for them to download it).

### License

See the LICENSE file for more info
