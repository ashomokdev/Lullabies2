package com.ashomok.lullabies.ui.main;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.view.ViewGroup;

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

    public void addFragment(MediaBrowserCompat.MediaItem item) {
        if (item != null) {
            myFragments.add(FragmentFactory.newInstance(item));
        } else {
            Log.d(TAG, "mediaitem == null");
        }
    }

    public void clear() {
        myFragments.clear();
    }

    //todo reduntsnt?
    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

    @NonNull
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object ret = super.instantiateItem(container, position);
        myFragments.set(position, (MusicFragment) ret);
        return ret;
    }
}
