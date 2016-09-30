package com.orange.pb.android.tamtam;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by pascalb on 8/17/16.
 */
public class Pager extends FragmentPagerAdapter {

    private final static String LOG_TAG = "Pager";

    // Tab titles.
    private String[] mTabTitles;
    private int mTabCount;
    private Fragment[] mFragments;

    public Pager(FragmentManager fm, String[] tabTitles) {

        super(fm);
        mTabTitles = tabTitles;
        mTabCount = tabTitles.length;
        mFragments = new Fragment[mTabCount];

    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabTitles[position];
    }

    /**
     * Must be called before getItem() is called for the first time.
     */
    public void createFragments() {

        mFragments[0] = new Tab1();
        mFragments[1] = new Tab2();
        mFragments[2] = new Tab3();
        mFragments[3] = new LogFragment();
    }

    @Override
    public Fragment getItem(int position) {

        if (position > mTabCount) {
            return null;
        }
        return mFragments[position];

    }

    @Override
    public int getCount() {
        return mTabCount;
    }
}

