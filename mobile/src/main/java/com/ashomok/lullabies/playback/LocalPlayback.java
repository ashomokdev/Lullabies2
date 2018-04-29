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
package com.ashomok.lullabies.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.ashomok.lullabies.MusicService;
import com.ashomok.lullabies.model.MusicProvider;
import com.ashomok.lullabies.model.MusicProviderSource;
import com.ashomok.lullabies.utils.MediaIDHelper;

import java.io.IOException;

import static android.support.v4.media.session.MediaSessionCompat.QueueItem;

/**
 * A class that implements local media playback using {@link android.media.MediaPlayer}
 */
public class LocalPlayback implements Playback, AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener {

    private static final String TAG = LocalPlayback.class.getSimpleName();

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED = 2;

    private final Context mContext;
    private int mState;
    private boolean mPlayOnFocusGain;
    private Callback mCallback;
    private final MusicProvider mMusicProvider;
    private volatile boolean mAudioNoisyReceiverRegistered;
    private volatile int mCurrentPosition;
    private volatile String mCurrentMediaId;

    // Type of audio focus we have:
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private final AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;

    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Log.d(TAG, "Headphones disconnected.");
                if (isPlaying()) {
                    Intent i = new Intent(context, MusicService.class);
                    i.setAction(MusicService.ACTION_CMD);
                    i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
                    mContext.startService(i);
                }
            }
        }
    };

    public LocalPlayback(Context context, MusicProvider musicProvider) {
        this.mContext = context;
        this.mMusicProvider = musicProvider;
        this.mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.mState = PlaybackStateCompat.STATE_NONE;
        Log.d(TAG, "LocalPlayback created");
    }

    @Override
    public void start() {
    }

    @Override
    public void stop(boolean notifyListeners) {
        mState = PlaybackStateCompat.STATE_STOPPED;
        if (notifyListeners && mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        mCurrentPosition = getCurrentStreamPosition();
        // Give up Audio focus
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        // Relax all resources
        relaxResources(true);
    }

    @Override
    public void setState(int state) {
        this.mState = state;
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isPlaying() {
        return mPlayOnFocusGain || (mMediaPlayer != null && mMediaPlayer.isPlaying());
    }

    @Override
    public int getCurrentStreamPosition() {
        return mMediaPlayer != null ?
                mMediaPlayer.getCurrentPosition() : mCurrentPosition;
    }

    @Override
    public void updateLastKnownStreamPosition() {
        if (mMediaPlayer != null) {
            mCurrentPosition = mMediaPlayer.getCurrentPosition();
        }
    }

    @Override
    public void play(QueueItem item) {
        Log.d(TAG, "play called");
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        String mediaId = item.getDescription().getMediaId();
        boolean mediaHasChanged = !TextUtils.equals(mediaId, mCurrentMediaId);
        if (mediaHasChanged) {
            mCurrentPosition = 0;
            mCurrentMediaId = mediaId;
        }

        if (mState == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && mMediaPlayer != null) {
            configMediaPlayerState();
        } else {
            mState = PlaybackStateCompat.STATE_STOPPED;
            relaxResources(false); // release everything except MediaPlayer

            MediaMetadataCompat track = mMusicProvider.getMusic(
                    MediaIDHelper.extractMusicIDFromMediaID(item.getDescription().getMediaId()));
            //noinspection ResourceType
            int musicResId = (int)track.getLong(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);

            try {
                createMediaPlayerIfNeeded();

                mState = PlaybackStateCompat.STATE_BUFFERING;

                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                setMusicSource(mMediaPlayer, musicResId);

                // Starts preparing the media player in the background. When
                // it's done, it will call our OnPreparedListener (that is,
                // the onPrepared() method on this class, since we set the
                // listener to 'this'). Until the media player is prepared,
                // we *cannot* call start() on it!
                mMediaPlayer.prepareAsync();

                if (mCallback != null) {
                    mCallback.onPlaybackStatusChanged(mState);
                }

            } catch (Exception ex) {
                Log.e(TAG, ex + "Exception playing song");
                if (mCallback != null) {
                    mCallback.onError(ex.getMessage());
                }
            }
        }
    }

    private void setMusicSource(MediaPlayer mediaPlayer, int musicResId) {
        AssetFileDescriptor afd = mContext.getResources().openRawResourceFd(musicResId);
        if (afd == null) {
            return;
        }
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            int millSecond = Integer.parseInt(durationStr);
            Log.d(TAG, "setMusicSource. musicResId = " + musicResId + " music duration = " + millSecond);

            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pause() {
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentPosition = mMediaPlayer.getCurrentPosition();
            }
            // while paused, retain the MediaPlayer but give up audio focus
            relaxResources(false);
        }
        mState = PlaybackStateCompat.STATE_PAUSED;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        unregisterAudioNoisyReceiver();
    }

    @Override
    public void seekTo(int position) {
        Log.d(TAG, "seekTo called with " + position);

        if (mMediaPlayer == null) {
            // If we do not have a current media player, simply update the current position
            mCurrentPosition = position;
        } else {
            if (mMediaPlayer.isPlaying()) {
                mState = PlaybackStateCompat.STATE_BUFFERING;
            }
            registerAudioNoisyReceiver();
            mMediaPlayer.seekTo(position);
            if (mCallback != null) {
                mCallback.onPlaybackStatusChanged(mState);
            }
        }
    }

    @Override
    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        this.mCurrentMediaId = mediaId;
    }

    @Override
    public String getCurrentMediaId() {
        return mCurrentMediaId;
    }

    /**
     * Try to get the system audio focus.
     */
    private void tryToGetAudioFocus() {
        Log.d(TAG, "tryToGetAudioFocus");
        int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mAudioFocus = AUDIO_FOCUSED;
        } else {
            mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    /**
     * Give up the audio focus.
     */
    private void giveUpAudioFocus() {
        Log.d(TAG, "giveUpAudioFocus");
        if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the MediaPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes mPlayer !=
     * null, so if you are calling it, you have to do so from a context where
     * you are sure this is the case.
     */
    private void configMediaPlayerState() {
        Log.d(TAG, "configMediaPlayerState. mAudioFocus=" + mAudioFocus);
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                pause();
            }
        } else {  // we have audio focus:
            registerAudioNoisyReceiver();
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
            } else {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
                } // else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
                    Log.d(TAG, "configMediaPlayerState startMediaPlayer. seeking to " +
                            mCurrentPosition);
                    if (mCurrentPosition == mMediaPlayer.getCurrentPosition()) {
                        mMediaPlayer.start();
                        Log.d(TAG, "media player started");
                        mState = PlaybackStateCompat.STATE_PLAYING;
                    } else {
                        mMediaPlayer.seekTo(mCurrentPosition);
                        mState = PlaybackStateCompat.STATE_BUFFERING;
                    }
                }
                mPlayOnFocusGain = false;
            }
        }
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    /**
     * Called by AudioManager on audio focus changes.
     * Implementation of {@link android.media.AudioManager.OnAudioFocusChangeListener}
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "onAudioFocusChange. focusChange=" + focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            mAudioFocus = AUDIO_FOCUSED;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (mState == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            Log.e(TAG, "onAudioFocusChange: Ignoring unsupported focusChange: " + focusChange);
        }
        configMediaPlayerState();
    }

    /**
     * Called when MediaPlayer has completed a seek
     *
     * @see MediaPlayer.OnSeekCompleteListener
     */
    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.d(TAG, "onSeekComplete from MediaPlayer:" + mp.getCurrentPosition());
        mCurrentPosition = mp.getCurrentPosition();
        if (mState == PlaybackStateCompat.STATE_BUFFERING) {
            registerAudioNoisyReceiver();
            mMediaPlayer.start();
            mState = PlaybackStateCompat.STATE_PLAYING;
        }
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    /**
     * Called when media player is done playing current song.
     *
     * @see MediaPlayer.OnCompletionListener
     */
    @Override
    public void onCompletion(MediaPlayer player) {
        Log.d(TAG, "onCompletion from MediaPlayer");
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (mCallback != null) {
            mCallback.onCompletion();
        }
    }

    /**
     * Called when media player is done preparing.
     *
     * @see MediaPlayer.OnPreparedListener
     */
    @Override
    public void onPrepared(MediaPlayer player) {
        Log.d(TAG, "onPrepared from MediaPlayer");
        // The media player is done preparing. That means we can start playing if we
        // have audio focus.
        configMediaPlayerState();
    }

    /**
     * Called when there's an error playing media. When this happens, the media
     * player goes to the Error state. We warn the user about the error and
     * reset the media player.
     *
     * @see MediaPlayer.OnErrorListener
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Media player error: what=" + what + ", extra=" + extra);
        if (mCallback != null) {
            mCallback.onError("MediaPlayer error " + what + " (" + extra + ")");
        }
        return true; // true indicates we handled the error
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private void createMediaPlayerIfNeeded() {
        Log.d(TAG, "createMediaPlayerIfNeeded. needed? " + (mMediaPlayer == null));
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mMediaPlayer.setWakeMode(mContext.getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing,
            // and when it's done playing:
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
        } else {
            mMediaPlayer.reset();
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     *                           be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        Log.d(TAG, "relaxResources. releaseMediaPlayer=" + releaseMediaPlayer);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mContext.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mContext.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }
}

///**
// * A class that implements local media playback using {@link
// * com.google.android.exoplayer2.ExoPlayer}
// */
//public final class LocalPlayback implements Playback {
//
//    private static final String TAG = LogHelper.makeLogTag(LocalPlayback.class);
//
//    // The volume we set the media player to when we lose audio focus, but are
//    // allowed to reduce the volume instead of stopping playback.
//    public static final float VOLUME_DUCK = 0.2f;
//    // The volume we set the media player when we have audio focus.
//    public static final float VOLUME_NORMAL = 1.0f;
//
//    // we don't have audio focus, and can't duck (play at a low volume)
//    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
//    // we don't have focus, but can duck (play at a low volume)
//    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
//    // we have full audio focus
//    private static final int AUDIO_FOCUSED = 2;
//
//    private final Context mContext;
//    private final WifiManager.WifiLock mWifiLock;
//    private boolean mPlayOnFocusGain;
//    private Callback mCallback;
//    private final MusicProvider mMusicProvider;
//    private boolean mAudioNoisyReceiverRegistered;
//    private String mCurrentMediaId;
//
//    private int mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
//    private final AudioManager mAudioManager;
//    private SimpleExoPlayer mExoPlayer;
//    private final ExoPlayerEventListener mEventListener = new ExoPlayerEventListener();
//
//    // Whether to return STATE_NONE or STATE_STOPPED when mExoPlayer is null;
//    private boolean mExoPlayerNullIsStopped =  false;
//
//    private final IntentFilter mAudioNoisyIntentFilter =
//            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
//
//    private final BroadcastReceiver mAudioNoisyReceiver =
//            new BroadcastReceiver() {
//                @Override
//                public void onReceive(Context context, Intent intent) {
//                    if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
//                        LogHelper.d(TAG, "Headphones disconnected.");
//                        if (isPlaying()) {
//                            Intent i = new Intent(context, MusicService.class);
//                            i.setAction(MusicService.ACTION_CMD);
//                            i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
//                            mContext.startService(i);
//                        }
//                    }
//                }
//            };
//
//    public LocalPlayback(Context context, MusicProvider musicProvider) {
//        Context applicationContext = context.getApplicationContext();
//        this.mContext = applicationContext;
//        this.mMusicProvider = musicProvider;
//
//        this.mAudioManager =
//                (AudioManager) applicationContext.getSystemService(Context.AUDIO_SERVICE);
//        // Create the Wifi lock (this does not acquire the lock, this just creates it)
//        this.mWifiLock =
//                ((WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE))
//                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "uAmp_lock");
//    }
//
//    @Override
//    public void start() {
//        // Nothing to do
//    }
//
//    @Override
//    public void stop(boolean notifyListeners) {
//        giveUpAudioFocus();
//        unregisterAudioNoisyReceiver();
//        releaseResources(true);
//    }
//
//    @Override
//    public void setState(int state) {
//        // Nothing to do (mExoPlayer holds its own state).
//    }
//
//    @Override
//    public int getState() {
//        if (mExoPlayer == null) {
//            return mExoPlayerNullIsStopped
//                    ? PlaybackStateCompat.STATE_STOPPED
//                    : PlaybackStateCompat.STATE_NONE;
//        }
//        switch (mExoPlayer.getPlaybackState()) {
//            case ExoPlayer.STATE_IDLE:
//                return PlaybackStateCompat.STATE_PAUSED;
//            case ExoPlayer.STATE_BUFFERING:
//                return PlaybackStateCompat.STATE_BUFFERING;
//            case ExoPlayer.STATE_READY:
//                return mExoPlayer.getPlayWhenReady()
//                        ? PlaybackStateCompat.STATE_PLAYING
//                        : PlaybackStateCompat.STATE_PAUSED;
//            case ExoPlayer.STATE_ENDED:
//                return PlaybackStateCompat.STATE_PAUSED;
//            default:
//                return PlaybackStateCompat.STATE_NONE;
//        }
//    }
//
//    @Override
//    public boolean isConnected() {
//        return true;
//    }
//
//    @Override
//    public boolean isPlaying() {
//        return mPlayOnFocusGain || (mExoPlayer != null && mExoPlayer.getPlayWhenReady());
//    }
//
//    @Override
//    public long getCurrentStreamPosition() {
//        return mExoPlayer != null ? mExoPlayer.getCurrentPosition() : 0;
//    }
//
//    @Override
//    public void updateLastKnownStreamPosition() {
//        // Nothing to do. Position maintained by ExoPlayer.
//    }
//
//    @Override
//    public void play(QueueItem item) {
//        mPlayOnFocusGain = true;
//        tryToGetAudioFocus();
//        registerAudioNoisyReceiver();
//        String mediaId = item.getDescription().getMediaId();
//        boolean mediaHasChanged = !TextUtils.equals(mediaId, mCurrentMediaId);
//        if (mediaHasChanged) {
//            mCurrentMediaId = mediaId;
//        }
//
//        if (mediaHasChanged || mExoPlayer == null) {
//            releaseResources(false); // release everything except the player
//            MediaMetadataCompat track =
//                    mMusicProvider.getMusic(
//                            MediaIDHelper.extractMusicIDFromMediaID(
//                                    item.getDescription().getMediaId()));
//
//            String source = track.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
//            if (source != null) {
//                source = source.replaceAll(" ", "%20"); // Escape spaces for URLs
//            }
//
//            if (mExoPlayer == null) {
//                mExoPlayer =
//                        ExoPlayerFactory.newSimpleInstance(
//                                mContext, new DefaultTrackSelector(), new DefaultLoadControl());
//                mExoPlayer.addListener(mEventListener);
//            }
//
//            // Android "O" makes much greater use of AudioAttributes, especially
//            // with regards to AudioFocus. All of UAMP's tracks are music, but
//            // if your content includes spoken word such as audiobooks or podcasts
//            // then the content type should be set to CONTENT_TYPE_SPEECH for those
//            // tracks.
//            final AudioAttributes audioAttributes = new AudioAttributes.Builder()
//                    .setContentType(CONTENT_TYPE_MUSIC)
//                    .setUsage(USAGE_MEDIA)
//                    .build();
//            mExoPlayer.setAudioAttributes(audioAttributes);
//
//            // Produces DataSource instances through which media data is loaded.
//            DataSource.Factory dataSourceFactory =
//                    new DefaultDataSourceFactory(
//                            mContext, Util.getUserAgent(mContext, "uamp"), null);
//            // Produces Extractor instances for parsing the media data.
//            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
//            // The MediaSource represents the media to be played.
//            MediaSource mediaSource =
//                    new ExtractorMediaSource(
//                            Uri.parse(source), dataSourceFactory, extractorsFactory, null, null);
//
//            // Prepares media to play (happens on background thread) and triggers
//            // {@code onPlayerStateChanged} callback when the stream is ready to play.
//            mExoPlayer.prepare(mediaSource);
//
//            // If we are streaming from the internet, we want to hold a
//            // Wifi lock, which prevents the Wifi radio from going to
//            // sleep while the song is playing.
//            mWifiLock.acquire();
//        }
//
//        configurePlayerState();
//    }
//
//    @Override
//    public void pause() {
//        // Pause player and cancel the 'foreground service' state.
//        if (mExoPlayer != null) {
//            mExoPlayer.setPlayWhenReady(false);
//        }
//        // While paused, retain the player instance, but give up audio focus.
//        releaseResources(false);
//        unregisterAudioNoisyReceiver();
//    }
//
//    @Override
//    public void seekTo(long position) {
//        LogHelper.d(TAG, "seekTo called with ", position);
//        if (mExoPlayer != null) {
//            registerAudioNoisyReceiver();
//            mExoPlayer.seekTo(position);
//        }
//    }
//
//    @Override
//    public void setCallback(Callback callback) {
//        this.mCallback = callback;
//    }
//
//    @Override
//    public void setCurrentMediaId(String mediaId) {
//        this.mCurrentMediaId = mediaId;
//    }
//
//    @Override
//    public String getCurrentMediaId() {
//        return mCurrentMediaId;
//    }
//
//    private void tryToGetAudioFocus() {
//        LogHelper.d(TAG, "tryToGetAudioFocus");
//        int result =
//                mAudioManager.requestAudioFocus(
//                        mOnAudioFocusChangeListener,
//                        AudioManager.STREAM_MUSIC,
//                        AudioManager.AUDIOFOCUS_GAIN);
//        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//            mCurrentAudioFocusState = AUDIO_FOCUSED;
//        } else {
//            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
//        }
//    }
//
//    private void giveUpAudioFocus() {
//        LogHelper.d(TAG, "giveUpAudioFocus");
//        if (mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener)
//                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
//        }
//    }
//
//    /**
//     * Reconfigures the player according to audio focus settings and starts/restarts it. This method
//     * starts/restarts the ExoPlayer instance respecting the current audio focus state. So if we
//     * have focus, it will play normally; if we don't have focus, it will either leave the player
//     * paused or set it to a low volume, depending on what is permitted by the current focus
//     * settings.
//     */
//    private void configurePlayerState() {
//        LogHelper.d(TAG, "configurePlayerState. mCurrentAudioFocusState=", mCurrentAudioFocusState);
//        if (mCurrentAudioFocusState == AUDIO_NO_FOCUS_NO_DUCK) {
//            // We don't have audio focus and can't duck, so we have to pause
//            pause();
//        } else {
//            registerAudioNoisyReceiver();
//
//            if (mCurrentAudioFocusState == AUDIO_NO_FOCUS_CAN_DUCK) {
//                // We're permitted to play, but only if we 'duck', ie: play softly
//                mExoPlayer.setVolume(VOLUME_DUCK);
//            } else {
//                mExoPlayer.setVolume(VOLUME_NORMAL);
//            }
//
//            // If we were playing when we lost focus, we need to resume playing.
//            if (mPlayOnFocusGain) {
//                mExoPlayer.setPlayWhenReady(true);
//                mPlayOnFocusGain = false;
//            }
//        }
//    }
//
//    private final AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener =
//            new AudioManager.OnAudioFocusChangeListener() {
//                @Override
//                public void onAudioFocusChange(int focusChange) {
//                    LogHelper.d(TAG, "onAudioFocusChange. focusChange=", focusChange);
//                    switch (focusChange) {
//                        case AudioManager.AUDIOFOCUS_GAIN:
//                            mCurrentAudioFocusState = AUDIO_FOCUSED;
//                            break;
//                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
//                            // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
//                            mCurrentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK;
//                            break;
//                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
//                            // Lost audio focus, but will gain it back (shortly), so note whether
//                            // playback should resume
//                            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
//                            mPlayOnFocusGain = mExoPlayer != null && mExoPlayer.getPlayWhenReady();
//                            break;
//                        case AudioManager.AUDIOFOCUS_LOSS:
//                            // Lost audio focus, probably "permanently"
//                            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
//                            break;
//                    }
//
//                    if (mExoPlayer != null) {
//                        // Update the player state based on the change
//                        configurePlayerState();
//                    }
//                }
//            };
//
//    /**
//     * Releases resources used by the service for playback, which is mostly just the WiFi lock for
//     * local playback. If requested, the ExoPlayer instance is also released.
//     *
//     * @param releasePlayer Indicates whether the player should also be released
//     */
//    private void releaseResources(boolean releasePlayer) {
//        LogHelper.d(TAG, "releaseResources. releasePlayer=", releasePlayer);
//
//        // Stops and releases player (if requested and available).
//        if (releasePlayer && mExoPlayer != null) {
//            mExoPlayer.release();
//            mExoPlayer.removeListener(mEventListener);
//            mExoPlayer = null;
//            mExoPlayerNullIsStopped = true;
//            mPlayOnFocusGain = false;
//        }
//
//        if (mWifiLock.isHeld()) {
//            mWifiLock.release();
//        }
//    }
//
//    private void registerAudioNoisyReceiver() {
//        if (!mAudioNoisyReceiverRegistered) {
//            mContext.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
//            mAudioNoisyReceiverRegistered = true;
//        }
//    }
//
//    private void unregisterAudioNoisyReceiver() {
//        if (mAudioNoisyReceiverRegistered) {
//            mContext.unregisterReceiver(mAudioNoisyReceiver);
//            mAudioNoisyReceiverRegistered = false;
//        }
//    }
//
//    private final class ExoPlayerEventListener implements ExoPlayer.EventListener {
//        @Override
//        public void onTimelineChanged(Timeline timeline, Object manifest) {
//            // Nothing to do.
//        }
//
//        @Override
//        public void onTracksChanged(
//                TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
//            // Nothing to do.
//        }
//
//        @Override
//        public void onLoadingChanged(boolean isLoading) {
//            // Nothing to do.
//        }
//
//        @Override
//        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//            switch (playbackState) {
//                case ExoPlayer.STATE_IDLE:
//                case ExoPlayer.STATE_BUFFERING:
//                case ExoPlayer.STATE_READY:
//                    if (mCallback != null) {
//                        mCallback.onPlaybackStatusChanged(getState());
//                    }
//                    break;
//                case ExoPlayer.STATE_ENDED:
//                    // The media player finished playing the current song.
//                    if (mCallback != null) {
//                        mCallback.onCompletion();
//                    }
//                    break;
//            }
//        }
//
//        @Override
//        public void onPlayerError(ExoPlaybackException error) {
//            final String what;
//            switch (error.type) {
//                case ExoPlaybackException.TYPE_SOURCE:
//                    what = error.getSourceException().getMessage();
//                    break;
//                case ExoPlaybackException.TYPE_RENDERER:
//                    what = error.getRendererException().getMessage();
//                    break;
//                case ExoPlaybackException.TYPE_UNEXPECTED:
//                    what = error.getUnexpectedException().getMessage();
//                    break;
//                default:
//                    what = "Unknown: " + error;
//            }
//
//            LogHelper.e(TAG, "ExoPlayer error: what=" + what);
//            if (mCallback != null) {
//                mCallback.onError("ExoPlayer error " + what);
//            }
//        }
//
//        @Override
//        public void onPositionDiscontinuity() {
//            // Nothing to do.
//        }
//
//        @Override
//        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
//            // Nothing to do.
//        }
//
//        @Override
//        public void onRepeatModeChanged(int repeatMode) {
//            // Nothing to do.
//        }
//    }
//}
