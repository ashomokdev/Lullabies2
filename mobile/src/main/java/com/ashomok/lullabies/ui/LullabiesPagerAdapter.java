package com.ashomok.lullabies.ui;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.media.MediaBrowserCompat;

import com.ashomok.lullabies.utils.LogHelper;

import java.util.ArrayList;
import java.util.List;

public class LullabiesPagerAdapter extends FragmentStatePagerAdapter {

    private List<MusicFragment> myFragments = new ArrayList<>();

    public final String TAG = LogHelper.makeLogTag(FragmentPagerAdapter.class);

    public LullabiesPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    @Override
    public int getCount() {
        return myFragments.size();
    }

    @Override
    public MusicFragment getItem(int position) {
        return myFragments.get(position);
    }

    public void add(MediaBrowserCompat.MediaItem item) {
        myFragments.add(FragmentFactory.newInstance(item));
    }

    public void clear() {
        myFragments.clear();
    }
}
