package eu.nerdz.app.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import eu.nerdz.api.Nerdz;

/**
 * Static class for preferences management.
 */
public class Prefs {

    private static final String sAppVersion = "NerdzMessengerVers";
    private static final String sConditions = "NerdzMessengerCondsAccepted";
    private static final String sDefaultImplementation = "DefaultImplementation";
    private static final String sGcmRegId = "NerdzMessengerRegId";
    private static final String sQuickResponse = "NerdzMessengerQuickResp";

    private static final String TAG = "NdzMsgPrefs";

    static SharedPreferences sSharedPreferences = PreferenceManager.getDefaultSharedPreferences(NerdzMessenger.context);

    public static boolean accepted() {
        return Prefs.sSharedPreferences.getBoolean(Prefs.sConditions, false);
    }

    public static String getImplementationName() {
        return Prefs.sSharedPreferences.getString(Prefs.sDefaultImplementation, Implementation.FASTREVERSE);
    }

    public static String getQuickResponse() {
        String res = Prefs.sSharedPreferences.getString(Prefs.sQuickResponse, NerdzMessenger.context.getString(R.string.lol));
        Log.d(TAG, res);
        return res;
    }

    public static String getGcmRegId() {
        String regId = Prefs.sSharedPreferences.getString(Prefs.sGcmRegId, "");
        if (TextUtils.isEmpty(regId)) {
            return null;
        }

        int regVers = Prefs.sSharedPreferences.getInt(Prefs.sAppVersion, Integer.MIN_VALUE);

        if (regVers != NerdzMessenger.getAppVersion()) { // Check for version changes
            return null;
        }

        return regId;
    }

    public static boolean isRegisteredGcm() {
        return Prefs.sSharedPreferences.contains(Prefs.sGcmRegId);
    }

    public static boolean setAccepted() {
        return Prefs.sSharedPreferences.edit().putBoolean(Prefs.sConditions, true).commit();
    }

    public static boolean setGcmRegId(String regId) {
        return Prefs.saveAppVersion() && Prefs.sSharedPreferences.edit().putString(Prefs.sGcmRegId, regId).commit();
    }

    public static boolean setImplementationName(String name) {
        return Prefs.sSharedPreferences.edit().putString(Prefs.sDefaultImplementation, name).commit();
    }

    public static boolean setQuickResponse(String response) {
        return Prefs.sSharedPreferences.edit().putString(Prefs.sQuickResponse, response).commit();
    }

    public static boolean saveAppVersion() {
        return Prefs.sSharedPreferences.edit().putInt(Prefs.sAppVersion, NerdzMessenger.getAppVersion()).commit();
    }

}
