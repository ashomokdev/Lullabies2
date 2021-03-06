/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ashomok.lullabies.model;

import android.support.v4.media.MediaMetadataCompat;
import com.ashomok.lullabies.utils.LogHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class SimpleMusicProviderSource implements MusicProviderSource {

    private static final String TAG = LogHelper.makeLogTag(SimpleMusicProviderSource.class);
    private List<MediaMetadataCompat> mData = new ArrayList<>();


    public void add(String title, String album, String artist, String genre, int source,
                    String albumArtUri, int imageDrawableId, long trackNumber, long totalTrackCount, long durationMs) {
        String id = String.valueOf(trackNumber);

        //noinspection ResourceType
        mData.add(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                .putLong(CUSTOM_METADATA_TRACK_SOURCE, source)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumArtUri)
                .putLong(CUSTOM_METADATA_TRACK_IMAGE_DRAWABLE_ID, imageDrawableId)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, totalTrackCount)
                .build());
    }

    public void add(TrackData trackData) {
        add(trackData.getTitle(), trackData.getAlbum(), trackData.getArtist(), trackData.getGenre(),
                trackData.getSource(), trackData.getAlbumArtUri(), trackData.getImageDrawableId(),
                trackData.getTrackNumber(), trackData.getTotalTrackCount(), trackData.getDurationMs());
    }

    @Override
    public Iterator<MediaMetadataCompat> iterator() {
        return mData.iterator();
    }

}
