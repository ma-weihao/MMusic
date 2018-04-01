package club.wello.mmusic.ui;


import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import club.wello.mmusic.AlbumArtCache;
import club.wello.mmusic.R;
import club.wello.mmusic.util.LogHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlaybackControlsFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(PlaybackControlsFragment.class);

    @BindView(R.id.ib_play_pause)
    ImageButton playPause;
    @BindView(R.id.ib_previous)
    ImageButton playPrevious;
    @BindView(R.id.ib_next)
    ImageButton playNext;
    @BindView(R.id.tv_title)
    TextView mTitle;
    @BindView(R.id.tv_subtite)
    TextView mSubtitle;
    @BindView(R.id.iv_album_art)
    ImageView albumArt;

    private String mArtUrl;

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            LogHelper.d(TAG, "Received playback state change to state ", state.getState());
            PlaybackControlsFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) {
                return;
            }
            LogHelper.d(TAG, "Received metadata state change to mediaId=",
                    metadata.getDescription().getMediaId(),
                    " song=", metadata.getDescription().getTitle());
            PlaybackControlsFragment.this.onMetadataChanged(metadata);
        }
    };

    private final View.OnClickListener mButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
            PlaybackStateCompat stateObj = controller.getPlaybackState();
            final int state = stateObj == null ?
                    PlaybackStateCompat.STATE_NONE : stateObj.getState();
            LogHelper.d(TAG, "Button pressed, in state " + state);
            switch (v.getId()) {
                case R.id.ib_play_pause:
                    LogHelper.d(TAG, "Play button pressed, in state " + state);
                    if (state == PlaybackStateCompat.STATE_PAUSED ||
                            state == PlaybackStateCompat.STATE_STOPPED ||
                            state == PlaybackStateCompat.STATE_NONE) {
                        playMedia();
                    } else if (state == PlaybackStateCompat.STATE_PLAYING ||
                            state == PlaybackStateCompat.STATE_BUFFERING ||
                            state == PlaybackStateCompat.STATE_CONNECTING) {
                        pauseMedia();
                    }
                    break;
                case R.id.ib_next:
                    LogHelper.d(TAG, "next button pressed, in state " + state);
                    MediaControllerCompat.TransportControls controls =
                            MediaControllerCompat.getMediaController(getActivity()).getTransportControls();
                    controls.skipToNext();
                case R.id.ib_previous:
                    MediaControllerCompat.TransportControls controls1 =
                            MediaControllerCompat.getMediaController(getActivity()).getTransportControls();
                    controls1.skipToPrevious();
                    break;
                default:

            }
        }
    };

    public PlaybackControlsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.music_control_bar, container, false);
        ButterKnife.bind(this, rootView);
        playPause.setOnClickListener(mButtonListener);
        albumArt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PlayingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                // TODO: 12/02/2018 deal with params
//                MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
//                MediaMetadataCompat metadata = controller.getMetadata();
//                if (metadata != null) {
//                    intent.putExtra(MusicPlayerActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION,
//                            metadata.getDescription());
//                }
                startActivity(intent);
            }
        });
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        LogHelper.d(TAG, "fragment.onStart");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            onConnected();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        LogHelper.d(TAG, "fragment.onStop");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.unregisterCallback(mCallback);
        }
    }

    private void playMedia() {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }

    private void pauseMedia() {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.getTransportControls().pause();
        }
    }

    public void onConnected() {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        LogHelper.d(TAG, "onConnected, mediaController==null? ", controller == null);
        if (controller != null) {
            onMetadataChanged(controller.getMetadata());
            onPlaybackStateChanged(controller.getPlaybackState());
            controller.registerCallback(mCallback);
        }
    }

    private void onMetadataChanged(MediaMetadataCompat metadata) {
        LogHelper.d(TAG, "onMetadataChanged ", metadata);
        if (getActivity() == null) {
            LogHelper.w(TAG, "onMetadataChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (metadata == null) {
            return;
        }

        mTitle.setText(metadata.getDescription().getTitle());
        mSubtitle.setText(metadata.getDescription().getSubtitle());
        String artUrl = null;
        if (metadata.getDescription().getIconUri() != null) {
            artUrl = metadata.getDescription().getIconUri().toString();
        }
        if (!TextUtils.equals(artUrl, mArtUrl)) {
            mArtUrl = artUrl;
            Bitmap art = metadata.getDescription().getIconBitmap();
            AlbumArtCache cache = AlbumArtCache.getInstance();
            if (art == null) {
                art = cache.getIconImage(mArtUrl);
            }
            if (art != null) {
                albumArt.setImageBitmap(art);
            } else {
                cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
                            @Override
                            public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                                if (icon != null) {
                                    LogHelper.d(TAG, "album art icon of w=", icon.getWidth(),
                                            " h=", icon.getHeight());
                                    if (isAdded()) {
                                        albumArt.setImageBitmap(icon);
                                    }
                                }
                            }
                        }
                );
            }
        }
    }

//    public void setExtraInfo(String extraInfo) {
//        if (extraInfo == null) {
//            mExtraInfo.setVisibility(View.GONE);
//        } else {
//            mExtraInfo.setText(extraInfo);
//            mExtraInfo.setVisibility(View.VISIBLE);
//        }
//    }

    private void onPlaybackStateChanged(PlaybackStateCompat state) {
        LogHelper.d(TAG, "onPlaybackStateChanged ", state);
        if (getActivity() == null) {
            LogHelper.w(TAG, "onPlaybackStateChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (state == null) {
            return;
        }
        boolean enablePlay = false;
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_STOPPED:
                enablePlay = true;
                break;
            case PlaybackStateCompat.STATE_ERROR:
                LogHelper.e(TAG, "error playbackstate: ", state.getErrorMessage());
                Toast.makeText(getActivity(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
                break;
        }

        if (enablePlay) {
            playPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_black_24dp));
        } else {
            playNext.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_black_24dp));
        }

        // i added it
//        playNext.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) == 0
//                ? INVISIBLE : VISIBLE );
//        playPrevious.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) == 0
//                ? INVISIBLE : VISIBLE );


        // TODO: 12/02/2018 show cast messages
//        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
//        String extraInfo = null;
//        if (controller != null && controller.getExtras() != null) {
//            String castName = controller.getExtras().getString(MusicService.EXTRA_CONNECTED_CAST);
//            if (castName != null) {
//                extraInfo = getResources().getString(R.string.casting_to_device, castName);
//            }
//        }
//        setExtraInfo(extraInfo);
    }
}
