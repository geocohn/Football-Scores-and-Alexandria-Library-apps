package barqsoft.footballscores;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import barqsoft.footballscores.widget.ScoresWidgetProvider;

public class MainActivity extends ActionBarActivity
{
    public static final int NUM_DAYS = 5; // Use an odd number for this
    public static int selected_match_id;
    public static int current_fragment = NUM_DAYS - (NUM_DAYS / 2) - 1;
    public static int selectedPosition;
    public static boolean fromWidget;
    public static String LOG_TAG = "MainActivity";
    private final String save_tag = "Save Test";
    private PagerFragment my_main;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        fromWidget = extras != null;
        if (fromWidget) {
            // get the page, match id, and item position
            // so that the app comes up showing the right stuff
            current_fragment = extras.getInt(ScoresWidgetProvider.EXTRA_PAGE_NUMBER, current_fragment);
            String matchId = extras.getString(ScoresWidgetProvider.EXTRA_MATCH_ID,
                    Integer.toString(selected_match_id));
            selected_match_id = Integer.parseInt(matchId);
            selectedPosition = extras.getInt(ScoresWidgetProvider.EXTRA_POSITION, 0);
            //Log.d(LOG_TAG,
            //        "Intent extras = fragment "
            //                + current_fragment
            //                + ", match id "
            //                + matchId
            //                + ", position "
            //                + selectedPosition);
        }
        setContentView(R.layout.activity_main);
        // Log.d(LOG_TAG, "Reached MainActivity onCreate");
        if (savedInstanceState == null) {
            my_main = new PagerFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, my_main)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about)
        {
            Intent start_about = new Intent(this,AboutActivity.class);
            startActivity(start_about);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        Log.v(save_tag,"will save");
        Log.v(save_tag,"fragment: "+String.valueOf(my_main.mPagerHandler.getCurrentItem()));
        Log.v(save_tag,"selected id: "+selected_match_id);
        outState.putInt("Pager_Current",my_main.mPagerHandler.getCurrentItem());
        outState.putInt("Selected_match",selected_match_id);
        getSupportFragmentManager().putFragment(outState,"my_main",my_main);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        Log.v(save_tag,"will retrieve");
        Log.v(save_tag,"fragment: "+String.valueOf(savedInstanceState.getInt("Pager_Current")));
        Log.v(save_tag,"selected id: "+savedInstanceState.getInt("Selected_match"));
        current_fragment = savedInstanceState.getInt("Pager_Current");
        selected_match_id = savedInstanceState.getInt("Selected_match");
        my_main = (PagerFragment) getSupportFragmentManager().getFragment(savedInstanceState,"my_main");
        super.onRestoreInstanceState(savedInstanceState);
    }
}
