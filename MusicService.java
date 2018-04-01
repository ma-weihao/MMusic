package club.wello.mmusic;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.ArrayList;
import java.util.List;

import club.wello.mmusic.ui.MusicProvider;
import club.wello.mmusic.util.LogHelper;

import static club.wello.mmusic.util.MediaIDHelper.MEDIA_ID_EMPTY_ROOT;
import static club.wello.mmusic.util.MediaIDHelper.MEDIA_ID_ROOT;

public class MusicService extends MediaBrowserServiceCompat {

    private static final String TAG = LogHelper.makeLogTag(MusicService.class);

    private static final String MY_MEDIA_ROOT_ID = "media_root_id";
    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";

    // Extra on MediaSession that contains the Cast device name currently connected to
    public static final String EXTRA_CONNECTED_CAST = "com.example.android.uamp.CAST_NAME";
    // The action of the incoming Intent indicating that it contains a command
    // to be executed (see {@link #onStartCommand})
    public static final String ACTION_CMD = "club.wello.mmusic.ACTION_CMD";
    // The key in the extras of the incoming Intent indicating the command that
    // should be executed (see {@link #onStartCommand})
    public static final String CMD_NAME = "CMD_NAME";
    // A value of a CMD_NAME key in the extras of the incoming Intent that
    // indicates that the music playback should be paused (see {@link #onStartCommand})
    public static final String CMD_PAUSE = "CMD_PAUSE";
    // A value of a CMD_NAME key that indicates that the music playback should switch
    // to local playback from cast playback.
    public static final String CMD_STOP_CASTING = "CMD_STOP_CASTING";
    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private PackageValidator mPackageValidator;

    private String LOG_TAG = "MMusic_MusicService";

    @Override
    public void onCreate() {
        super.onCreate();
        mPackageValidator = new PackageValidator(this);
        mediaSession = new MediaSessionCompat(this, LOG_TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        mediaSession.setCallback(new MySessionCallback());

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mediaSession.getSessionToken());

        Context context = getApplicationContext();
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 99 /*request code*/,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mediaSession.setSessionActivity(pi);

    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (intent != null) {
//            String action = intent.getAction();
//            String command = intent.getStringExtra(CMD_NAME);
//            if (ACTION_CMD.equals(action)) {
//                if (CMD_PAUSE.equals(command)) {
//                    mPlaybackManager.handlePauseRequest();
//                } else if (CMD_STOP_CASTING.equals(command)) {
//                    CastContext.getSharedInstance(this).getSessionManager().endCurrentSession(true);
//                }
//            } else {
//                // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
//                MediaButtonReceiver.handleIntent(mediaSession, intent);
//            }
//        }
//        // Reset the delay handler to enqueue a message to stop the service if
//        // nothing is playing.
//        mDelayedStopHandler.removeCallbacksAndMessages(null);
//        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
//        return START_STICKY;
//    }

    public MusicService() {
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogHelper.d(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        LogHelper.d(TAG, "OnGetRoot: clientPackageName=" + clientPackageName,
                "; clientUid=" + clientUid + " ; rootHints=", rootHints);

        // To ensure you are not allowing any arbitrary app to browse your app's contents, you
        // need to check the origin:
        if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return an empty browser root.
            // If you return null, then the media browser will not be able to connect and
            // no further calls will be made to other media browsing methods.
            LogHelper.i(TAG, "OnGetRoot: Browsing NOT ALLOWED for unknown caller. "
                    + "Returning empty browser root so all apps can use MediaController."
                    + clientPackageName);
            return new MediaBrowserServiceCompat.BrowserRoot(MEDIA_ID_EMPTY_ROOT, null);
        }

        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId,
                               @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        LogHelper.d(TAG, "OnLoadChildren: parentMediaId=", parentId);

        if (MEDIA_ID_EMPTY_ROOT.equals(parentId)) {
            result.sendResult(new ArrayList<MediaBrowserCompat.MediaItem>());
        } else if (mMusicProvider.isInitialized()) {
            // if music library is ready, return immediately
            result.sendResult(mMusicProvider.getChildren(parentMediaId, getResources()));
        } else {
            // otherwise, only return results when the music library is retrieved
            result.detach();
            mMusicProvider.retrieveMediaAsync(new MusicProvider.Callback() {
                @Override
                public void onMusicCatalogReady(boolean success) {
                    result.sendResult(mMusicProvider.getChildren(parentMediaId, getResources()));
                }
            });
        }
    }
}
