package club.wello.mmusic.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import club.wello.mmusic.AlbumArtCache;
import club.wello.mmusic.MusicService;
import club.wello.mmusic.R;
import club.wello.mmusic.util.LogHelper;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class PlayingActivity extends AppCompatActivity {

    private static final String TAG = LogHelper.makeLogTag(PlayingActivity.class);
    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    @BindView(R.id.album_art)
    ImageView albumArt;
    @BindView(R.id.tv_alarm_time_left)
    TextView alarmTimeLeft;
    @BindView(R.id.tv_time_played)
    TextView timePlayed;
    @BindView(R.id.tv_time_left)
    TextView timeLeft;
    @BindView(R.id.sb_progress)
    SeekBar progressBar;
    @BindView(R.id.tv_title)
    TextView title;
    @BindView(R.id.tv_subtite)
    TextView subtitle;
    @BindView(R.id.ib_add)
    ImageButton addButton;
    @BindView(R.id.ib_more)
    ImageButton moreButton;
    @BindView(R.id.sb_volume)
    SeekBar volumeBar;
    @BindView(R.id.ib_play_pause)
    ImageButton playPause;
    @BindView(R.id.ib_previous)
    ImageButton previous;
    @BindView(R.id.ib_next)
    ImageButton next;
    @BindView(R.id.progress)
    ProgressBar loadingIndicater;

    private Drawable playDrawable;
    private Drawable pauseDrawable;
    private Drawable previousDrawable;
    private Drawable nextDrawable;
    private Drawable replay30Drawable;
    private Drawable forward30Drawable;

    private String currentArtUrl;

    private final Handler handler = new Handler();
    private final ScheduledExecutorService executorService =
            Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledFuture;

    private MediaBrowserCompat mediaBrowser;
    private PlaybackStateCompat lastPlaybackState;

    private final Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            LogHelper.d(TAG, "onPlaybackstate changed", state);
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                updateMediaDescription(metadata.getDescription());
                updateDuration(metadata);
            }
        }
    };

    private final MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    LogHelper.d(TAG, "onConnected");
                    try {
                        connectToSession(mediaBrowser.getSessionToken());
                    } catch (RemoteException e) {
                        LogHelper.e(TAG, e, "could not connect media controller");
                    }
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing);
        playDrawable = ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_24dp);
        pauseDrawable = ContextCompat.getDrawable(this, R.drawable.ic_pause_black_24dp);
        previousDrawable = ContextCompat.getDrawable(this, R.drawable.ic_skip_previous_black_24dp);
        nextDrawable = ContextCompat.getDrawable(this, R.drawable.ic_skip_next_black_24dp);
        replay30Drawable = ContextCompat.getDrawable(this, R.drawable.ic_replay_30_black_24dp);
        forward30Drawable = ContextCompat.getDrawable(this, R.drawable.ic_forward_30_black_24dp);
        initView();

        // Only update from the intent if we are not recreating from a config change:
        if (savedInstanceState == null) {
            updateFromParams(getIntent());
        }

        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MusicService.class), connectionCallback, null);
    }

    private void initView() {
        ButterKnife.bind(this);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls =
                        MediaControllerCompat.getMediaController(PlayingActivity.this).getTransportControls();
                controls.skipToPrevious();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls =
                        MediaControllerCompat.getMediaController(PlayingActivity.this).getTransportControls();
                controls.skipToNext();
            }
        });

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackStateCompat state =
                        MediaControllerCompat.getMediaController(PlayingActivity.this).getPlaybackState();
                if (state != null) {
                    MediaControllerCompat.TransportControls controls =
                            MediaControllerCompat.getMediaController(PlayingActivity.this)
                                    .getTransportControls();
                    switch (state.getState()) {
                        case PlaybackStateCompat.STATE_PLAYING: // fall through
                        case PlaybackStateCompat.STATE_BUFFERING:
                            controls.pause();
                            stopSeekbarUpdate();
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                        case PlaybackStateCompat.STATE_STOPPED:
                            controls.play();
                            scheduleSeekbarUpdate();
                            break;
                        default:
                            LogHelper.d(TAG, "onClick with state ", state.getState());
                    }
                }
            }
        });

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                timePlayed.setText(DateUtils.formatElapsedTime(progress / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MediaControllerCompat.getMediaController(PlayingActivity.this)
                        .getTransportControls().seekTo(seekBar.getProgress());
                scheduleSeekbarUpdate();
            }
        });
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        MediaControllerCompat mediaController = new MediaControllerCompat(
                PlayingActivity.this, token);
        if (mediaController.getMetadata() == null) {
            finish();
            return;
        }
        MediaControllerCompat.setMediaController(PlayingActivity.this, mediaController);
        mediaController.registerCallback(callback);
        PlaybackStateCompat state = mediaController.getPlaybackState();
        updatePlaybackState(state);
        MediaMetadataCompat metadata = mediaController.getMetadata();
        if (metadata != null) {
            updateMediaDescription(metadata.getDescription());
            updateDuration(metadata);
        }
        updateProgress();
        if (state != null && (state.getState() == PlaybackStateCompat.STATE_PLAYING ||
                state.getState() == PlaybackStateCompat.STATE_BUFFERING)) {
            scheduleSeekbarUpdate();
        }
    }

    private void updateFromParams(Intent intent) {
        // TODO: 12/02/2018 deal with the params
//        if (intent != null) {
//            MediaDescriptionCompat description = intent.getParcelableExtra(
//                    MusicPlayerActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION);
//            if (description != null) {
//                updateMediaDescription(description);
//            }
//        }
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!executorService.isShutdown()) {
            scheduledFuture = executorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            handler.post(updateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mediaBrowser != null) {
            mediaBrowser.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mediaBrowser != null) {
            mediaBrowser.disconnect();
        }
        MediaControllerCompat controllerCompat =
                MediaControllerCompat.getMediaController(PlayingActivity.this);
        if (controllerCompat != null) {
            controllerCompat.unregisterCallback(callback);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSeekbarUpdate();
        executorService.shutdown();
    }

    private void fetchImageAsync(@NonNull MediaDescriptionCompat description) {
        if (description.getIconUri() == null) {
            return;
        }
        String artUrl = description.getIconUri().toString();
        currentArtUrl = artUrl;
        AlbumArtCache cache = AlbumArtCache.getInstance();
        Bitmap art = cache.getBigImage(artUrl);
        if (art == null) {
            art = description.getIconBitmap();
        }
        if (art != null) {
            // if we have the art cached or from the MediaDescription, use it:
            albumArt.setImageBitmap(art);
        } else {
            // otherwise, fetch a high res version and update:
            cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
                @Override
                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                    // sanity check, in case a new fetch request has been done while
                    // the previous hasn't yet returned:
                    if (artUrl.equals(currentArtUrl)) {
                        albumArt.setImageBitmap(bitmap);
                    }
                }
            });
        }
    }

    private void updateMediaDescription(MediaDescriptionCompat description) {
        if (description == null) {
            return;
        }
        LogHelper.d(TAG, "updateMediaDescription called ");
        title.setText(description.getTitle());
        subtitle.setText(description.getSubtitle());
        fetchImageAsync(description);
    }

    private void updateDuration(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }
        LogHelper.d(TAG, "updateDuration called ");
        int duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        progressBar.setMax(duration);
        timeLeft.setText(DateUtils.formatElapsedTime(duration / 1000));

    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }
        lastPlaybackState = state;

        // TODO: 12/02/2018 show cast messages
//        MediaControllerCompat controllerCompat =
//                MediaControllerCompat.getMediaController(PlayingActivity.this);
//        if (controllerCompat != null && controllerCompat.getExtras() != null) {
//            String castName = controllerCompat.getExtras().getString(MusicService.EXTRA_CONNECTED_CAST);
//            String line3Text = castName == null ? "" : getResources()
//                    .getString(R.string.casting_to_device, castName);
//            mLine3.setText(line3Text);
//        }

        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                loadingIndicater.setVisibility(INVISIBLE);
//                playPause.setVisibility(VISIBLE);
                playPause.setImageDrawable(pauseDrawable);
//                mControllers.setVisibility(VISIBLE);
                scheduleSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
//                mControllers.setVisibility(VISIBLE);
                loadingIndicater.setVisibility(INVISIBLE);
//                mPlayPause.setVisibility(VISIBLE);
                playPause.setImageDrawable(playDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                loadingIndicater.setVisibility(INVISIBLE);
//                mPlayPause.setVisibility(VISIBLE);
                playPause.setImageDrawable(playDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
//                mPlayPause.setVisibility(INVISIBLE);
                loadingIndicater.setVisibility(VISIBLE);
//                mLine3.setText(R.string.loading);
                stopSeekbarUpdate();
                break;
            default:
                LogHelper.d(TAG, "Unhandled state ", state.getState());
        }

        next.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) == 0
                ? INVISIBLE : VISIBLE );
        previous.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) == 0
                ? INVISIBLE : VISIBLE );
    }

    private void updateProgress() {
        if (lastPlaybackState == null) {
            return;
        }
        long currentPosition = lastPlaybackState.getPosition();
        if (lastPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() -
                    lastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * lastPlaybackState.getPlaybackSpeed();
        }
        progressBar.setProgress((int) currentPosition);
    }

}
