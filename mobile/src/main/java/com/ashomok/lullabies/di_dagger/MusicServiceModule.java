package com.ashomok.lullabies.di_dagger;

/**
 * Created by iuliia on 12/27/17.
 */

import android.content.Context;
import android.support.annotation.StringRes;

import com.ashomok.lullabies.model.MusicProvider;
import com.ashomok.lullabies.model.MusicSource;
import com.ashomok.lullabies.ui.MusicFragment;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

/**
 * This is a Dagger module.
 */
@Module
public abstract class MusicServiceModule {

    @Provides
    static MusicProvider provideMusicProvider(Context context){
        return new MusicProvider(context);
    }

    @Provides
    static MusicSource provideMusicSource(Context context){
        return new MusicSource(context);
    }
}
