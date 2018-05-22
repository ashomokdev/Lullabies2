package com.ashomok.lullabies.model;

import android.content.Context;

import com.ashomok.lullabies.R;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MusicSource {

    private ArrayList<TrackData> musicSource = new ArrayList<>();
    private Context context;

    public ArrayList<TrackData> getMusicSource() {
        return getMusicSource(context);
    }

    @Inject
    public MusicSource(Context context) {
        this.context = context;
    }

    public ArrayList<TrackData> getMusicSource(Context context) {
        if (musicSource.size() > 0) {
            return musicSource;
        } else {
            musicSource.add(new TrackData(context.getString(R.string.lion), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music1, R.drawable.lion, 0, 10, 166034));//

            musicSource.add(new TrackData(context.getString(R.string.koala), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music2, R.drawable.koala, 1, 10, 208405));//

            musicSource.add(new TrackData(context.getString(R.string.giraffe), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music3, R.drawable.giraffe, 2, 10, 266173));//

            musicSource.add(new TrackData(context.getString(R.string.crocodile), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music4, R.drawable.crocodile, 3, 10, 205846));//

            musicSource.add(new TrackData(context.getString(R.string.elephant), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music5, R.drawable.elephant, 4, 10, 215355));//

            musicSource.add(new TrackData(context.getString(R.string.whale), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music6, R.drawable.whale, 5, 10, 204932));//

            musicSource.add(new TrackData(context.getString(R.string.chameleon), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music7, R.drawable.chameleon, 6, 10, 241560));//

            musicSource.add(new TrackData(context.getString(R.string.turtle), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music8, R.drawable.turtle, 7, 10, 260904));//

            musicSource.add(new TrackData(context.getString(R.string.monkey), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music9, R.drawable.monkey, 8, 10, 190459));//

            musicSource.add(new TrackData(context.getString(R.string.penguin), "Lullabies",
                    context.getString(R.string.lullaby_songs), "Lullabies",
                    R.raw.music10, R.drawable.penguin, 9, 10, 224000));
            return musicSource;
        }
    }
}
