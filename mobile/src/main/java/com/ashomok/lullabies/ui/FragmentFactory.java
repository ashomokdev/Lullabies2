package com.ashomok.lullabies.ui;


import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;

/**
 * Created by Iuliia on 31.03.2016.
 */
public class FragmentFactory {

    public static Fragment newInstance(MediaBrowserCompat.MediaItem mediaItem) {

        return MusicFragment.newInstance(mediaItem);
    }
}
