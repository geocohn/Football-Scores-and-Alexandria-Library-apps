package barqsoft.footballscores.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.contentprovider.ScoresContract;
import barqsoft.footballscores.contentprovider.ScoresProvider;

/**
 * Created by geo on 2/10/16.
 *
 * Implements a collection widget as a ListView
 * It uses the app's content provider as its data source,
 * and when an item is pressed, it opens the app to show the selected item
 */
class ScoresRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String LOG_TAG = ScoresRemoteViewsFactory.class.getSimpleName();
    private static final String[] S_PROJECTION =
            {ScoresContract.scores_table.AWAY_COL,
                    ScoresContract.scores_table.AWAY_GOALS_COL,
                    ScoresContract.scores_table.DATE_COL,
                    ScoresContract.scores_table.HOME_COL,
                    ScoresContract.scores_table.HOME_GOALS_COL,
                    ScoresContract.scores_table.MATCH_ID,
                    ScoresContract.scores_table.TIME_COL
    };
    private static final int COL_AWAY = 0;
    private static final int COL_AWAY_GOALS = 1;
    private static final int COL_DATE = 2;
    private static final int COL_HOME = 3;
    private static final int COL_HOME_GOALS = 4;
    private static final int COL_MATCH_ID = 5;
    private static final int COL_TIME = 6;

    private Context mContext;
    private Cursor mCursor;

    public ScoresRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        int mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public void onCreate() {
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.

        ScoresProvider scoresProvider = new ScoresProvider();

        Long now = System.currentTimeMillis();
        long daysInMillis = (MainActivity.NUM_DAYS / 2) * 86400000;
        Date startDate =  new Date(now - daysInMillis);
        Date endDate = new Date(now + daysInMillis);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String[] selection = {format.format(startDate), format.format(endDate)};
        // Log.v(LOG_TAG, "Start date, end date = " + format.format(startDate) + ", " + format.format(endDate));

        mCursor = scoresProvider.query(
                ScoresContract.BASE_CONTENT_URI,
                S_PROJECTION,
                ScoresContract.scores_table.DATE_COL + " BETWEEN ? AND ?",
                selection,
                ScoresContract.scores_table.DATE_COL + " ASC");
    }

    public void onDestroy() {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public int getCount() {
        return mCursor.getCount();
    }

    public RemoteViews getViewAt(int position) {
        // position will always range from 0 to getCount() - 1.

        // We construct a remote views item based on our widget item xml file, and set up the
        // views based on the position.
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);
        mCursor.moveToPosition(position);
        String matchDate = mCursor.getString(COL_DATE);
        long matchDateMillis = dateToMillis(matchDate);
        rv.setTextViewText(R.id.home, mCursor.getString(COL_HOME));
        rv.setTextViewText(R.id.score,
                Utilies.getScores(mCursor.getInt(COL_HOME_GOALS), mCursor.getInt(COL_AWAY_GOALS)));
        rv.setTextViewText(R.id.date, dayName(matchDateMillis));
        rv.setTextViewText(R.id.time, mCursor.getString(COL_TIME));
        rv.setTextViewText(R.id.away, mCursor.getString(COL_AWAY));

        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in ScoresWidgetProvider.
        Bundle extras = new Bundle();
        long todayMillis = System.currentTimeMillis();
        Time time = new Time();
        time.setToNow();
        int julianMatchDay = Time.getJulianDay(matchDateMillis, time.gmtoff);
        int julianToday = Time.getJulianDay(todayMillis, time.gmtoff);
        int page = (MainActivity.NUM_DAYS / 2) + (julianMatchDay - julianToday);
        String matchId = mCursor.getString(COL_MATCH_ID);
        extras.putInt(ScoresWidgetProvider.EXTRA_PAGE_NUMBER, page);
        extras.putString(ScoresWidgetProvider.EXTRA_MATCH_ID, matchId);
        // the app shows football matches one day per page. Find the item's position within the page
        // by counting how many times it takes decrementing the cursor to get to the beginning
        // or to a different date, whichever comes first.
        int pagePosition = 0;
        while (mCursor.moveToPrevious() && mCursor.getString(COL_DATE).equals(matchDate)) {
            pagePosition++;
        }
        extras.putInt(ScoresWidgetProvider.EXTRA_POSITION, pagePosition);

        // we're sending the following extras to the app:
        // 1. page number
        // 2. match id
        // 3. position within the page
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.widget_row, fillInIntent);

        // Return the remote views object.
        return rv;
    }

    public RemoteViews getLoadingView() {
        // You can create a custom loading view (for instance when getViewAt() is slow.) If you
        // return null here, you will get the default loading view.
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        // This is triggered when you call AppWidgetManager notifyAppWidgetViewDataChanged
        // on the collection view corresponding to this factory. You can do heaving lifting in
        // here, synchronously. For example, if you need to process an image, fetch something
        // from the network, etc., it is ok to do it here, synchronously. The widget will remain
        // in its current state while work is being done here, so you don't need to worry about
        // locking up the widget.
    }

    public long dateToMillis(String date)
    {
        //String date_ = date;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date mDate = sdf.parse(date);
            return mDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String dayName(long dateInMillis) {
        // If the date is today, return the localized version of "Today" instead of the actual
        // day name.

        Time t = new Time();
        t.setToNow();
        int julianDay = Time.getJulianDay(dateInMillis, t.gmtoff);
        int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);
        if (julianDay == currentJulianDay) {
            return mContext.getString(R.string.today);
        } else if ( julianDay == currentJulianDay +1 ) {
            return mContext.getString(R.string.tomorrow);
        }
        else if ( julianDay == currentJulianDay -1)
        {
            return mContext.getString(R.string.yesterday);
        }
        else
        {
            Time time = new Time();
            time.setToNow();
            // Otherwise, the format is just the day of the week (e.g "Wednesday".
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(dateInMillis);
        }
    }
}
