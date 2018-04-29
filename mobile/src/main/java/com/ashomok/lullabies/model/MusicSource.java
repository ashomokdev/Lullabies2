package com.ashomok.lullabies.model;

import android.content.Context;
import android.net.Uri;

import com.ashomok.lullabies.MyApplication;
import com.ashomok.lullabies.R;

import java.util.ArrayList;

public class MusicSource {
    private ArrayList<TrackData> musicSource = new ArrayList<>();

    public ArrayList<TrackData> getMusicSource() {
        Context context = MyApplication.getAppContext();
        return getMusicSource(context);
    }

    public ArrayList<TrackData> getMusicSource(Context context) {
        if (musicSource.size() > 0) {
            return musicSource;
        } else {
            musicSource.add(new TrackData(context.getString(R.string.lion), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music1, "http://animals.sandiegozoo.org/sites/default/files/2016-08/category-thumbnail-mammals_0.jpg", 0, 10, 166034));//

            musicSource.add(new TrackData(context.getString(R.string.koala), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music2, "http://animals.sandiegozoo.org/sites/default/files/2016-08/category-thumbnail-mammals_0.jpg", 1, 10, 208405));//

            musicSource.add(new TrackData(context.getString(R.string.giraffe), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music3, "http://animals.sandiegozoo.org/sites/default/files/2016-08/category-thumbnail-mammals_0.jpg", 2, 10, 266173));//

            musicSource.add(new TrackData(context.getString(R.string.crocodile), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music4, "http://animals.sandiegozoo.org/sites/default/files/2016-08/category-thumbnail-mammals_0.jpg", 3, 10, 205846));//

            musicSource.add(new TrackData(context.getString(R.string.elephant), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music5, "http://animals.sandiegozoo.org/sites/default/files/2016-08/category-thumbnail-mammals_0.jpg", 4, 10, 215355));//

            musicSource.add(new TrackData(context.getString(R.string.whale), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music6, "http://animals.sandiegozoo.org/sites/default/files/2016-08/category-thumbnail-mammals_0.jpg", 5, 10, 204932));//

            musicSource.add(new TrackData(context.getString(R.string.chameleon), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music7, "http://animals.sandiegozoo.org/sites/default/files/2016-08/category-thumbnail-mammals_0.jpg", 6, 10, 241560));//

            musicSource.add(new TrackData(context.getString(R.string.turtle), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music8, "http://animals.sandiegozoo.org/sites/default/files/2016-08/category-thumbnail-mammals_0.jpg", 7, 10, 260904));//

            musicSource.add(new TrackData(context.getString(R.string.monkey), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music9, "http://animals.sandiegozoo.org/sites/default/files/2016-08/category-thumbnail-mammals_0.jpg", 8, 10, 190459));//

            musicSource.add(new TrackData(context.getString(R.string.penguin), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music10, "http://animals.sandiegozoo.org/sites/default/files/2016-08/category-thumbnail-mammals_0.jpg", 9, 10, 224000));
            return musicSource;
        }
    }
}
