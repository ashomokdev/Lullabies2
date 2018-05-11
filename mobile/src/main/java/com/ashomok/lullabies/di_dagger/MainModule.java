package com.ashomok.lullabies.di_dagger;

import android.support.annotation.StringRes;

import com.ashomok.lullabies.R;
import com.ashomok.lullabies.Settings;
import com.ashomok.lullabies.ui.MusicFragment;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

/**
 * This is a Dagger module.
 */
@Module
public abstract class MainModule {

    @ContributesAndroidInjector
    @FragmentScoped
    abstract MusicFragment musicFragment();

    @Provides
    static @StringRes
    int provideAdBannerId() {
        if (Settings.isTestMode) {
            return R.string.test_banner;
        } else {
            return R.string.main_activity_banner;
        }
    }
}