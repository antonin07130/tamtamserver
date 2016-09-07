package com.orange.pb.android.tablayout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by pascalb on 8/17/16.
 */
public class Pager extends FragmentPagerAdapter {

    private final static String LOG_TAG = "Pager";

    // Tab titles.
    private String[] mTabTitles;
    private int mTabCount;
    private Fragment[] mFragments;
    private boolean[] mFragmentCreated;

    public Pager(FragmentManager fm, String[] tabTitles) {

        super(fm);
        mTabTitles = tabTitles;
        mTabCount = tabTitles.length;
        mFragments = new Fragment[mTabCount];
        mFragmentCreated = new boolean[mTabCount];
        for (int i = 0; i < mTabCount; i++) {
            mFragmentCreated[i] = false;
        }

    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabTitles[position];
    }

    @Override
    public Fragment getItem(int position) {

        AppLog.d(LOG_TAG, "getItem() - " + position);
        switch(position) {
            case 0:
                if (!mFragmentCreated[0]) {
                    mFragments[0] = new Tab1();
                    mFragmentCreated[0] = true;
                }
                return mFragments[0];
            case 1:
                if (!mFragmentCreated[1]) {
                    mFragments[1] = new Tab2();
                    mFragmentCreated[1] = true;
                }
                return mFragments[1];
            case 2:
                if (!mFragmentCreated[2]) {
                    mFragments[2] = new Tab3();
                    mFragmentCreated[2] = true;
                }
                return mFragments[2];
            case 3:
                if (!mFragmentCreated[3]) {
                    mFragments[3] = new LogFragment();
                    mFragmentCreated[3] = true;
                }
                return mFragments[3];
            default:
                return null;
        }

    }

    @Override
    public int getCount() {
        return mTabCount;
    }
}

