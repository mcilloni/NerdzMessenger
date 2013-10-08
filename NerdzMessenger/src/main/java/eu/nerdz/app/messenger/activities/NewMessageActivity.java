package eu.nerdz.app.messenger.activities;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

import eu.nerdz.app.messenger.R;

public class NewMessageActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_new_message);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_message, menu);
        return true;
    }
    
}
