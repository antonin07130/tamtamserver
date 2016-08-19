package com.orange.pb.android.tablayout;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by pascalb on 8/18/16.
 */
public class LogFragment extends ListFragment {

    private final static String LOG_TAG = "LogFragment";

    /** For log tab. */
    private final static int LOG_MAX_NB = 128;
    private ArrayAdapter<String> mLog;

    @Override
    public void onAttach (Context context) {

        super.onAttach(context);
        Log.d(LOG_TAG, "onAttach()");

    }

    @Override
    public void onCreate (Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate()");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // ListFragment provides a default implementation of onCreateView(), which returns
        // a FrameLayout. Embedded ListView's id is android.R.id.list.
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Log.d(LOG_TAG, "onCreateView()");

        return view;

    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        Log.d(LOG_TAG, "onActivityCreated()");
        // Create array adapter for log display.
        mLog = new ArrayAdapter<String>(getActivity(),
                R.layout.tab_log_item);
        // Bind it to the fragment list view.
        setListAdapter(mLog);
        // And tell log class we exist.
        AppLog.setFragment(this);

    }

    @Override
    public void onViewStateRestored (Bundle savedInstanceState) {

        super.onViewStateRestored(savedInstanceState);
        Log.d(LOG_TAG, "onViewStateRestored");

    }

    @Override
    public void onStart () {

        super.onStart();
        Log.d(LOG_TAG, "onStart()");

    }

    @Override
    public void onResume () {

        super.onResume();
        // Ask AppLog to resend us all log messages.
        displayLog(AppLog.getLogMsg());
        Log.d(LOG_TAG, "onResume()");

    }

    @Override
    public void onPause () {

        super.onPause();
        Log.d(LOG_TAG, "onPause()");

    }

    @Override
    public void onStop () {

        super.onStop();
        Log.d(LOG_TAG, "onStop()");

    }

    @Override
    public void onDestroyView () {

        super.onDestroyView();
        Log.d(LOG_TAG, "onDestroyView()");

    }

    @Override
    public void onDestroy () {

        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy()");

    }

    @Override
    public void onDetach () {

        super.onDetach();
        Log.d(LOG_TAG, "onDetach()");

    }

     public void displayLog(ArrayList<String> logMessages) {

         mLog.clear();
         mLog.addAll(logMessages);

    }

}
