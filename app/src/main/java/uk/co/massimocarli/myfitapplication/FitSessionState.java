package uk.co.massimocarli.myfitapplication;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Massimo Carli on 08/11/14.
 */
public final class FitSessionState {

    /**
     * The name for the SharedPreferences
     */
    private static final String PREFS_NAME = "FitSessions";

    /**
     * The key for the session Id of the current session
     */
    private static final String CURRENT_SESSION_ID = "CURRENT_SESSION_ID";

    /**
     * The key for the information about the started session
     */
    private static final String STARTED_SESSION = "STARTED_SESSION";

    /**
     * The singleton instance
     */
    private static FitSessionState sInstance;

    /**
     * The SharedPreferences
     */
    private final SharedPreferences mPrefs;

    /**
     * Private constructor for a FitSessionState
     *
     * @param context The Context
     */
    private FitSessionState(final Context context) {
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * The Static Factory method that manage the sessionId
     *
     * @param context The Context
     * @return The FitSessionState singleton instance
     */
    public synchronized static FitSessionState get(final Context context) {
        if (sInstance == null) {
            sInstance = new FitSessionState(context);
        }
        return sInstance;
    }

    /**
     * If not started creates a new SessionId
     *
     * @return The session id for the new Session
     */
    public synchronized String startSession() {
        // We check if the session has started
        final boolean sessionStarted = mPrefs.getBoolean(STARTED_SESSION, false);
        // The existing session id
        final Long sessionId = mPrefs.getLong(CURRENT_SESSION_ID, 0);
        if (sessionStarted) {
            // If started we return the same id. The session creation should fail
            // We get the current session Id
            return getSessionIdFromValue(sessionId);
        } else {
            // We create a new session id
            // We increment the id
            final Long newSessionId = System.currentTimeMillis();
            // We save the new Data
            mPrefs.edit().putLong(CURRENT_SESSION_ID, newSessionId).
                    putBoolean(STARTED_SESSION, true).commit();
            // We return the sessionId
            return getSessionIdFromValue(newSessionId);
        }
    }

    /**
     * @return The name of the session
     */
    public synchronized String getSessionName() {
        final Long sessionId = mPrefs.getLong(CURRENT_SESSION_ID, 0);
        return getSessionIdFromValue(sessionId);
    }

    /**
     * @return The current Session id
     */
    public synchronized String getCurrentSessionId() {
        final Long sessionId = mPrefs.getLong(CURRENT_SESSION_ID, 0);
        return getSessionIdFromValue(sessionId);
    }

    /**
     * @return The current Session name
     */
    public synchronized String getCurrentSessionName() {
        final Long sessionId = mPrefs.getLong(CURRENT_SESSION_ID, 0);
        return getSessionNameFromValue(sessionId);
    }

    /**
     * We just put the session as finished
     */
    public synchronized void stopSession() {
        // We just put the session as finished
        mPrefs.edit().putBoolean(STARTED_SESSION, false).commit();
    }


    /**
     * Creates the String for the string
     *
     * @param sessionId The id for the session
     * @return The sessionId as string from the number
     */
    private String getSessionIdFromValue(final long sessionId) {
        return String.valueOf(sessionId);
    }

    /**
     * Creates the String for the string
     *
     * @param sessionId The id for the session
     * @return The session name as string from the number
     */
    private String getSessionNameFromValue(final long sessionId) {
        return String.valueOf(sessionId);
    }
}

