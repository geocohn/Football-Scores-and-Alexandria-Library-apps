package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.samples.vision.barcodereader.BarcodeCaptureActivity;
import com.google.android.gms.samples.vision.barcodereader.ui.camera.CameraSource;
import com.google.android.gms.vision.barcode.Barcode;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;

public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = AddBook.class.getSimpleName();
    private static final int RC_BARCODE_CAPTURE = 9001;
    private EditText ean;
    private final int LOADER_ID = 1;
    private final String EAN_CONTENT="eanContent";
    private View mRootView;
    private ProgressBar mAddBookProgressBar;
    private ImageView mCoverView;
    private ProgressBar mProgressSpinner;


    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(ean!=null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ean = (EditText) mRootView.findViewById(R.id.ean);
        mAddBookProgressBar = (ProgressBar) mRootView.findViewById(R.id.addBookProgressBar);

        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ean = s.toString();
                if (ean.length() == 0) {
                    // this happens right after a save or delete, so just bail rather than
                    // flagging a malformed ISBN
                    clearFields();
                    return;
                }
                //catch isbn10 numbers
                if(ean.length() == 10 && !ean.startsWith("978")) {
                    ean = "978" + ean;
                }
                if (looksLikeISBN(ean)) {
                    setStatus(BookService.STATUS_OK);
                    // Once we have an ISBN, start a book intent
                    Intent bookIntent = new Intent(getActivity(), BookService.class);
                    bookIntent.putExtra(BookService.EAN, ean);
                    bookIntent.setAction(BookService.FETCH_BOOK);
                    getActivity().startService(bookIntent);
                    AddBook.this.restartLoader();
                    mRootView.findViewById(R.id.bookEmpty).setVisibility(View.GONE);
                    mAddBookProgressBar.setVisibility(View.VISIBLE);
//                    Log.d(LOG_TAG, "afterTextChanged has ISBN, empty view gone, progress spinner visible");
                } else {
                    clearFields();
                    setStatus(BookService.STATUS_BAD_ISBN);
                    showError(StatusString());
                    return;
                }
            }
        });

        if (CameraSource.hasBackFacingCamera()) {
            final CheckBox autoFocus = (CheckBox) mRootView.findViewById(R.id.auto_focus);
            final CheckBox useFlash = (CheckBox) mRootView.findViewById(R.id.use_flash);

            mRootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), BarcodeCaptureActivity.class);
                    intent.putExtra(BarcodeCaptureActivity.AutoFocus, autoFocus.isChecked());
                    intent.putExtra(BarcodeCaptureActivity.UseFlash, useFlash.isChecked());
                    startActivityForResult(intent, RC_BARCODE_CAPTURE);
                }
            });
        } else {
            mRootView.findViewById(R.id.scan_button).setVisibility(View.GONE);
            mRootView.findViewById(R.id.auto_focus).setVisibility(View.GONE);
            mRootView.findViewById(R.id.use_flash).setVisibility(View.GONE);
        }

        mRootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ean.setText("");
            }
        });

        mRootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean.getText().toString());
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                ean.setText("");
            }
        });

        if(savedInstanceState!=null){
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            ean.setHint("");
        }

        return mRootView;
    }

    private boolean looksLikeISBN(String ean) {
        // this is called before any ean lookup to keep the downstream code simpler and
        // to take care of barcodes that had non-numeric data
        return (ean.length() == 13)
                && isDigits(ean)
                && ean.startsWith("978");
    }

    private boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i)))
                return false;
        }
        return true;
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(ean.getText().length() == 0) {
            return null;
        }
        String eanStr = ean.getText().toString();

       if(eanStr.length()==10 && !eanStr.startsWith("978")){
            eanStr="978"+eanStr;
        }

        if (!looksLikeISBN(eanStr)) {
            setStatus(BookService.STATUS_BAD_ISBN);
            showError(StatusString());
            return null;
        }

        setStatus(BookService.STATUS_OK);

        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {

        mAddBookProgressBar.setVisibility(View.GONE);
//        Log.d(LOG_TAG, "Enter onLoadFinished, progress spinner and empty view gone");
        mRootView.findViewById(R.id.bookEmpty).setVisibility(View.GONE);

        if (!data.moveToFirst()) {
            showError(StatusString());
//            Log.d(LOG_TAG, "onLoadFinished returned early with " + StatusString());
            return;
        }

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        ((TextView) mRootView.findViewById(R.id.bookTitle)).setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        ((TextView) mRootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        if (authors != null) {
            String[] authorsArr = authors.split(",");
            ((TextView) mRootView.findViewById(R.id.authors)).setLines(authorsArr.length);
            ((TextView) mRootView.findViewById(R.id.authors)).setText(authors.replace(",", "\n"));
        }
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            mCoverView = (ImageView) mRootView.findViewById(R.id.bookCover);
            mProgressSpinner = (ProgressBar) mRootView.findViewById(R.id.bookCoverProgress);
            mProgressSpinner.setVisibility(View.VISIBLE);
            Picasso.with(getContext())
                    .load(imgUrl)
                    .into(mCoverView, new Callback() {
                        @Override
                        public void onSuccess() {
                            mProgressSpinner.setVisibility(View.GONE);
                        }
                        @Override
                        public void onError() {
                            mCoverView.setImageResource(R.mipmap.ic_missing);
                        }
                    });
            mCoverView.setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) mRootView.findViewById(R.id.categories)).setText(categories);

        mRootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
        mRootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
    }

    private void showError(String statusString) {
        clearFields();
        TextView tv = (TextView) mRootView.findViewById(R.id.bookEmpty);
        tv.setText(statusString);
        tv.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        ((TextView) mRootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) mRootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) mRootView.findViewById(R.id.authors)).setText("");
        ((TextView) mRootView.findViewById(R.id.categories)).setText("");
        mRootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        mRootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
        mRootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
        mRootView.findViewById(R.id.bookEmpty).setVisibility(View.GONE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }



    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * #RESULT_CANCELED if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     * <p/>
     * <p>You will receive this call immediately before onResume() when your
     * activity is re-starting.
     * <p/>
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     * @see #startActivityForResult
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
//                    statusMessage.setText(R.string.barcode_success);
                    ean.setText(barcode.displayValue);
//                    Log.d(LOG_TAG, "Barcode read: " + barcode.displayValue);
                } else {
//                    statusMessage.setText(R.string.barcode_failure);
//                    Log.d(LOG_TAG, "No barcode captured, intent data is null");
                }
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String StatusString() {
        String statusString = getString(R.string.book_not_found);
        @BookService.Status int locationStatus = getStatus();
        switch (locationStatus) {
            case BookService.STATUS_BAD_ISBN:
                statusString = getString(R.string.input_hint);
                break;
            case BookService.STATUS_SERVER_INVALID:
                statusString = getString(R.string.invalid_data);
                break;
            case BookService.STATUS_SERVER_DOWN:
                statusString = getString(R.string.server_down);
                // fall through on purpose
            default:
                if (!isOnline()) {
                    statusString = getString(R.string.no_network);
                }
        }
        return statusString;
    }

    private boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private int getStatus() {
        int status;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        status = sharedPref.getInt(getString(R.string.pref_location_status),
                BookService.STATUS_UNKNOWN);
//        Log.d(LOG_TAG, "Getting Status " + status);
        return status;
    }

    private void setStatus(@BookService.Status int status) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.pref_location_status), status);
        editor.apply();
    }
}
