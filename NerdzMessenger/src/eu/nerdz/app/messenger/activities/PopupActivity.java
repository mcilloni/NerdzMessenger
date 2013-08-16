package eu.nerdz.app.messenger.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;


@SuppressLint("Registered")
public class PopupActivity extends Activity {
    
    protected void popUp(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        try {
            builder.setMessage(msg);
            builder.create().show();
            return;
        } catch (Throwable t) {}
    }
    
    protected void shortToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}
