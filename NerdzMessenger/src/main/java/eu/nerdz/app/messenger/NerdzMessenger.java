package eu.nerdz.app.messenger;


import android.app.Application;
import android.content.Context;

/**
 * Created by marco on 8/25/13.
 */
public class NerdzMessenger extends Application {

    private static Context sContext;

    @Override
    public void onCreate(){
        super.onCreate();
        NerdzMessenger.sContext = this.getApplicationContext();
    }

    public static Context getAppContext() {
        return NerdzMessenger.sContext;
    }

}
