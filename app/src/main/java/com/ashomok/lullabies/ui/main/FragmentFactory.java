package com.ashomok.lullabies.ui.main;


import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;

import com.ashomok.lullabies.ui.main.MusicFragment;

/**
 * Created by Iuliia on 31.03.2016.
 */
public class FragmentFactory {

    public static MusicFragment newInstance(MediaBrowserCompat.MediaItem mediaItem) {
        return MusicFragment.newInstance(mediaItem);
    }
}
