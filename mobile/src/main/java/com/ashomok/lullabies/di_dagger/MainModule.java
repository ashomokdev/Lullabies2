package com.ashomok.lullabies.di_dagger;

import android.content.Context;

import com.ashomok.lullabies.model.MusicProvider;
import com.ashomok.lullabies.model.MusicSource;
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
}