package com.orange.pb.android.tablayout;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by pascalb on 8/17/16.
 */
public class Pager extends FragmentStatePagerAdapter {

    // Tab titles.
    private String[] mTabTitles;
    private int mTabCount;

    public Pager(FragmentManager fm, String[] tabTitles) {
        super(fm);
        mTabTitles = tabTitles;
        mTabCount = tabTitles.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabTitles[position];
    }

    @Override
    public Fragment getItem(int position) {
        //Returning the current tabs
        switch (position) {
            case 0:
                Tab1 tab1 = new Tab1();
                return tab1;
            case 1:
                Tab2 tab2 = new Tab2();
                return tab2;
            case 2:
                Tab3 tab3 = new Tab3();
                return tab3;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mTabCount;
    }
}

