package club.wello.mmusic.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by maweihao on 12/02/2018.
 */

public class NetworkHelper {

    /**
     * @param context to use to check for network connectivity.
     * @return true if connected, false otherwise.
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}
