package eu.nerdz.app.messenger.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import eu.nerdz.app.messenger.SettingsFragment;

public class SettingsActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getActionBar().setDisplayHomeAsUpEnabled(true);

        this.getFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content, new SettingsFragment())
            .commit();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
