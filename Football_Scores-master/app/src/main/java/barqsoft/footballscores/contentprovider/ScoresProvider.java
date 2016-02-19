package barqsoft.footballscores.contentprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * Created by yehya khaled on 2/25/2015.
 */
public class ScoresProvider extends ContentProvider
{
    private static final String LOG_TAG = ScoresProvider.class.getSimpleName();
    private static ScoresDBHelper mOpenHelper;
    private static final int MATCHES = 100;
    private static final int MATCHES_WITH_LEAGUE = 101;
    private static final int MATCHES_WITH_ID = 102;
    private static final int MATCHES_WITH_DATE = 103;
    private UriMatcher muriMatcher = buildUriMatcher();
    private static final SQLiteQueryBuilder ScoreQuery =
            new SQLiteQueryBuilder();
    private static final String SCORES_BY_LEAGUE = ScoresContract.scores_table.LEAGUE_COL + " = ?";
    private static final String SCORES_BY_DATE =
            ScoresContract.scores_table.DATE_COL + " LIKE ?";
    private static final String SCORES_BY_ID =
            ScoresContract.scores_table.MATCH_ID + " = ?";


    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ScoresContract.BASE_CONTENT_URI.toString();
        matcher.addURI(authority, null , MATCHES);
        matcher.addURI(authority, "league" , MATCHES_WITH_LEAGUE);
        matcher.addURI(authority, "id" , MATCHES_WITH_ID);
        matcher.addURI(authority, "date" , MATCHES_WITH_DATE);
        return matcher;
    }

    private int match_uri(Uri uri)
    {
        String link = uri.toString();
        {
           if(link.contentEquals(ScoresContract.BASE_CONTENT_URI.toString()))
           {
               return MATCHES;
           }
           else if(link.contentEquals(ScoresContract.scores_table.buildScoreWithDate().toString()))
           {
               return MATCHES_WITH_DATE;
           }
           else if(link.contentEquals(ScoresContract.scores_table.buildScoreWithId().toString()))
           {
               return MATCHES_WITH_ID;
           }
           else if(link.contentEquals(ScoresContract.scores_table.buildScoreWithLeague().toString()))
           {
               return MATCHES_WITH_LEAGUE;
           }
        }
        return -1;
    }
    @Override
    public boolean onCreate()
    {
        mOpenHelper = new ScoresDBHelper(getContext());
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        return 0;
    }

    @Override
    public String getType(Uri uri)
    {
        final int match = muriMatcher.match(uri);
        switch (match) {
            case MATCHES:
                return ScoresContract.scores_table.CONTENT_TYPE;
            case MATCHES_WITH_LEAGUE:
                return ScoresContract.scores_table.CONTENT_TYPE;
            case MATCHES_WITH_ID:
                return ScoresContract.scores_table.CONTENT_ITEM_TYPE;
            case MATCHES_WITH_DATE:
                return ScoresContract.scores_table.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri :" + uri );
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        Cursor retCursor;
        // Log.v(LOG_TAG, uri.getPathSegments().toString());
        int match = match_uri(uri);
        // Log.v(LOG_TAG, "selectionArgs[0] = " + selectionArgs[0]);
        // Log.v(LOG_TAG, "URI match = " + String.valueOf(match));
        switch (match)
        {
            case MATCHES: retCursor = mOpenHelper.getReadableDatabase().query(
                    ScoresContract.SCORES_TABLE,
                    projection,selection,selectionArgs,null,null,sortOrder); break;
            case MATCHES_WITH_DATE:
                    retCursor = mOpenHelper.getReadableDatabase().query(
                    ScoresContract.SCORES_TABLE,
                    projection,SCORES_BY_DATE,selectionArgs,null,null,sortOrder); break;
            case MATCHES_WITH_ID: retCursor = mOpenHelper.getReadableDatabase().query(
                    ScoresContract.SCORES_TABLE,
                    projection,SCORES_BY_ID,selectionArgs,null,null,sortOrder); break;
            case MATCHES_WITH_LEAGUE: retCursor = mOpenHelper.getReadableDatabase().query(
                    ScoresContract.SCORES_TABLE,
                    projection,SCORES_BY_LEAGUE,selectionArgs,null,null,sortOrder); break;
            default: throw new UnsupportedOperationException("Unknown Uri" + uri);
        }
        // Need to see if a context exists, in case we got here via a widget
        if (getContext() != null) {
            retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        return null;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values)
    {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        //db.delete(ScoresContract.SCORES_TABLE,null,null);
        //Log.v(FetchScoreTask.LOG_TAG,String.valueOf(muriMatcher.match(uri)));
        switch (match_uri(uri))
        {
            case MATCHES:
                db.beginTransaction();
                int returncount = 0;
                try
                {
                    for(ContentValues value : values)
                    {
                        long _id = db.insertWithOnConflict(ScoresContract.SCORES_TABLE, null, value,
                                SQLiteDatabase.CONFLICT_REPLACE);
                        if (_id != -1)
                        {
                            returncount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri,null);
                return returncount;
            default:
                return super.bulkInsert(uri,values);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }
}
