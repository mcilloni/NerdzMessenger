package eu.nerdz.app.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Static class for preferences management.
 */
public class Prefs {

    private static final String sPrefsFile = "NerdzMessengerPrefs";
    private static final String sDefaultImplementation = "DefaultImplementation";

    private static SharedPreferences sSharedPreferences = NerdzMessenger.getAppContext().getSharedPreferences(Prefs.sPrefsFile, Context.MODE_PRIVATE);

    public static String getImplementationName() {
        return Prefs.sSharedPreferences.getString(Prefs.sDefaultImplementation, Implementation.FASTREVERSE);
    }

    public static boolean setImplementationName(String name) {
        return Prefs.sSharedPreferences.edit().putString(Prefs.sDefaultImplementation, name).commit();
    }

}
