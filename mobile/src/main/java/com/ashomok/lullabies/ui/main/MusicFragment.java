package com.ashomok.lullabies.ui.main;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ashomok.lullabies.AlbumArtCache;
import com.ashomok.lullabies.R;
import com.ashomok.lullabies.di_dagger.ActivityScoped;
import com.ashomok.lullabies.model.MusicProvider;
import com.ashomok.lullabies.rate_app.RateAppAsker;
import com.ashomok.lullabies.utils.LogHelper;
import com.ashomok.lullabies.utils.MediaIDHelper;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

import static com.ashomok.lullabies.model.MusicProviderSource.CUSTOM_METADATA_TRACK_IMAGE_DRAWABLE_ID;

/**
 * Created by iuliia on 03.05.16.
 */

//see FullScreenPlayerActivity if you want to add more components
@ActivityScoped
public class MusicFragment extends DaggerFragment {

    private static final String TAG = LogHelper.makeLogTag(MusicFragment.class);
    private static final String ARGUMENT_MEDIA_ITEM = "media_item";
    private MediaBrowserCompat.MediaItem mediaItem;
    private ImageView mBackgroundImage;
    private String mCurrentArtUrl;
    private int mCurrentDrawableID;
    private TextView textViewName;
    private TextView textViewGenre;

    @Inject
    MusicProvider musicProvider;

    @Inject
    public MusicFragment() {
        // Required empty public constructor
    }

    public MediaBrowserCompat.MediaItem getMediaItem() {
        return mediaItem;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaItem = getArguments().getParcelable(ARGUMENT_MEDIA_ITEM);

        RateAppAsker.init(getActivity());
    }

    static MusicFragment newInstance(MediaBrowserCompat.MediaItem mediaItem) {
        Log.d(TAG, "newInstance called with " + mediaItem.getDescription().getMediaId());
        MusicFragment pageFragment = new MusicFragment();
        Bundle arguments = new Bundle();

        arguments.putParcelable(ARGUMENT_MEDIA_ITEM, mediaItem);

        pageFragment.setArguments(arguments);
        return pageFragment;
    }


    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.music_fragment, null);
        mBackgroundImage = view.findViewById(R.id.image);
        MediaDescriptionCompat mediaItemDescription = mediaItem.getDescription();
        fetchImageAsync(mediaItemDescription);

        textViewName = view.findViewById(R.id.name);
        textViewGenre = view.findViewById(R.id.genre);
        updateTrackPreview(mediaItemDescription);
        return view;
    }

    private void updateTrackPreview(MediaDescriptionCompat description) {

        CharSequence name = description.getTitle();
        CharSequence category = description.getSubtitle();

        textViewName.setText(name);
        textViewGenre.setText(category);
    }

    private void fetchImageAsync(@NonNull MediaDescriptionCompat description) {
        AlbumArtCache cache = AlbumArtCache.getInstance();
        if (description.getIconUri() == null) {
            fetchImageFromDrawable(description, cache);
        } else {
            fetchImageFromUri(description, cache);
        }
    }

    private void fetchImageFromUri(@NonNull MediaDescriptionCompat description, AlbumArtCache cache) {
        Uri iconUri = description.getIconUri();
        String artUrl = iconUri.toString();
        mCurrentArtUrl = artUrl;
        Bitmap art = cache.getBigImage(artUrl);
        if (art == null) {
            art = description.getIconBitmap();
        }
        if (art != null) {
            // if we have the art cached or from the MediaDescription, use it:
            mBackgroundImage.setImageBitmap(art);
        } else {
            // otherwise, fetch a high res version and update:
            cache.fetch(artUrl, new AlbumArtCache.FetchUrlListener() {
                @Override
                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                    // sanity check, in case a new fetch request has been done while
                    // the previous hasn't yet returned:
                    if (artUrl.equals(mCurrentArtUrl)) {
                        mBackgroundImage.setImageBitmap(bitmap);
                    }
                }
            });
        }
    }

    private void fetchImageFromDrawable(@NonNull MediaDescriptionCompat description, AlbumArtCache cache) {
        if (musicProvider != null) {
            MediaMetadataCompat mMetadata =
                    musicProvider.getMusic(MediaIDHelper.extractMusicIDFromMediaID(mediaItem.getMediaId()));
            int drawableID = (int) mMetadata.getLong(CUSTOM_METADATA_TRACK_IMAGE_DRAWABLE_ID);
            if (drawableID != 0) {
                mCurrentDrawableID = drawableID;
                mBackgroundImage.setImageDrawable(getResources().getDrawable(drawableID));
            }
        }
    }
}
