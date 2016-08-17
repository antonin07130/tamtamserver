/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.slidingtabsbasic;

import com.example.android.common.view.SlidingTabLayout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * A basic sample which shows how to use {@link com.example.android.common.view.SlidingTabLayout}
 * to display a custom {@link ViewPager} title strip which gives continuous feedback to the user
 * when scrolling.
 */
public class SlidingTabsBasicFragment extends Fragment {

    private final static String LOG_TAG = "SlidingTabs";
    private final static int NB_OF_TABS = 4;

    private String[] mTabTitles = new String[NB_OF_TABS];
    private View[] mTabViews = new View[NB_OF_TABS];
    private boolean[] mViewAdded = new boolean[NB_OF_TABS];

    /** For log tab. */
    private final static int LOG_MAX_NB = 128;
    private ArrayAdapter<String> mLog;

    /**
     * A custom {@link ViewPager} title strip which looks much like Tabs present in Android v4.0 and
     * above, but is designed to give continuous feedback to the user when scrolling.
     */
    private SlidingTabLayout mSlidingTabLayout;

    /**
     * A {@link ViewPager} which will be used in conjunction with the {@link SlidingTabLayout} above.
     */
    private ViewPager mViewPager;

    /**
     * Inflates the {@link View} which will be displayed by this {@link Fragment}, from the app's
     * resources.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mTabTitles[0] = getResources().getString(R.string.tab_my_things);
        mTabTitles[1] = getResources().getString(R.string.tab_around_me);
        mTabTitles[2] = getResources().getString(R.string.tab_tracked_things);
        mTabTitles[3] = getResources().getString(R.string.tab_log);

        for (int i = 0; i < 3; i++) {
            mTabViews[i] = getActivity().getLayoutInflater().inflate(R.layout.pager_item,
                    container, false);
            mViewAdded[i] = false;
        }
        mTabViews[3] = getActivity().getLayoutInflater().inflate(R.layout.pager_log,
                container, false);
        mViewAdded[3] = false;

        return inflater.inflate(R.layout.fragment_sample, container, false);
    }

    // BEGIN_INCLUDE (fragment_onviewcreated)
    /**
     * This is called after the {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has finished.
     * Here we can pick out the {@link View}s we need to configure from the content view.
     *
     * We set the {@link ViewPager}'s adapter to be an instance of {@link AppPagerAdapter}. The
     * {@link SlidingTabLayout} is then given the {@link ViewPager} so that it can populate itself.
     *
     * @param view View created in {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // BEGIN_INCLUDE (setup_viewpager)
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(new AppPagerAdapter());
        // END_INCLUDE (setup_viewpager)

        // BEGIN_INCLUDE (setup_slidingtablayout)
        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
        // END_INCLUDE (setup_slidingtablayout)
    }
    // END_INCLUDE (fragment_onviewcreated)

    /**
     * The {@link android.support.v4.view.PagerAdapter} used to display pages in this sample.
     * The individual pages are simple and just display two lines of text. The important section of
     * this class is the {@link #getPageTitle(int)} method which controls what is displayed in the
     * {@link SlidingTabLayout}.
     */
    class AppPagerAdapter extends PagerAdapter {

        /**
         * @return the number of pages to display
         */
        @Override
        public int getCount() {
            //return 10;
            return NB_OF_TABS;
        }

        /**
         * @return true if the value returned from {@link #instantiateItem(ViewGroup, int)} is the
         * same object as the {@link View} added to the {@link ViewPager}.
         */
        @Override
        public boolean isViewFromObject(View view, Object o) {
            return o == view;
        }

        // BEGIN_INCLUDE (pageradapter_getpagetitle)
        /**
         * Return the title of the item at {@code position}. This is important as what this method
         * returns is what is displayed in the {@link SlidingTabLayout}.
         * <p>
         * Here we construct one using the position value, but for real application the title should
         * refer to the item's contents.
         */
        @Override
        public CharSequence getPageTitle(int position) {
            //return "Item " + (position + 1);
            return mTabTitles[position];
        }
        // END_INCLUDE (pageradapter_getpagetitle)

        /**
         * Instantiate the {@link View} which should be displayed at {@code position}. Here we
         * inflate a layout from the apps resources and then change the text view to signify the position.
         */
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            AppLog.d(LOG_TAG, "instantiateItem() [position: " + position + "]");
            if (mViewAdded[position]) {
                return mTabViews[position];
            }
            // Inflate a new layout from our resources
            mViewAdded[position] = true;
            View view = mTabViews[position];
            if (position < 3) {
                // Add the newly created View to the ViewPager
                container.addView(view);
                // Retrieve a TextView from the inflated View, and update it's text
                TextView title = (TextView) view.findViewById(R.id.item_title);
                title.setText(String.valueOf(position + 1));
                Log.i(LOG_TAG, "instantiateItem() [position: " + position + "]");
            } else {
                // Add log view.
                container.addView(view);
                // Create array adapte for log display.
                mLog = new ArrayAdapter<String>(getActivity(),
                        R.layout.pager_log_item);
                // And bind it to the list view.
                ListView listView = (ListView) getActivity().findViewById(R.id.pager_log_lv);
                listView.setAdapter(mLog);
                // Configure log class.
                AppLog.setFragment(SlidingTabsBasicFragment.this);
                AppLog.d(LOG_TAG, "Ready");
            }

            // Return the View
            return view;
        }

        /**
         * Destroy the item from the {@link ViewPager}. In our case this is simply removing the
         * {@link View}.
         */
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
//            container.removeView((View) object);
            AppLog.d(LOG_TAG, "destroyItem() [position: " + position + "]");
        }

    }

    /**
     * Adds a new log message to the messages being displayed. If max number of
     * messages is reached, the oldest one is removed. The new message is added at
     * the top of the message list.
     *
     * @param trace
     */
    public void addLog(String trace) {

        mLog.insert(trace, 0);
        int tracesNb = mLog.getCount();
        if (tracesNb > LOG_MAX_NB) {
            String traceToRemove = mLog.getItem(tracesNb - 1);
            mLog.remove(traceToRemove);
        }
        ViewPager viewPager = (ViewPager) getActivity().findViewById(R.id.viewpager);
        viewPager.invalidate();

    }

}
