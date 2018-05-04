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
    abstract MusicFragment musicFragment();

    @Provides
    static MusicProvider provideMusicProvider(Context context){
        return new MusicProvider(context);
    }

    @Provides
    static MusicSource provideMusicSource(Context context){
        return new MusicSource(context);
    }
}