package uk.co.massimocarli.myfitapplication;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;


public class MainActivity extends Activity {

    /**
     * The Tag for the Log
     */
    private static final String TAG_LOG = MainActivity.class.getName();

    /**
     * The Id for the authentication request
     */
    private static final int REQUEST_OAUTH = 1;

    /**
     * We use this variable to test if the auth process is pending or not
     */
    private static final String AUTH_PENDING = "auth_state_pending";

    /**
     * If true the authorization process is in progress
     */
    private boolean authInProgress = false;

    /**
     * The GoogleApiClient to access Google Play Services
     */
    private GoogleApiClient mGoogleApiClient = null;

    /**
     * The Current Fragment
     */
    private Fragment mCurrentFragment;

    /**
     * The ConnectionCallback implementation to manage connection with GoogleApiClient
     */
    private final GoogleApiClient.ConnectionCallbacks mConnectionCallbacks =
            new GoogleApiClient.ConnectionCallbacks() {

                @Override
                public void onConnected(Bundle bundle) {
                    // The Fitness API are connected so we can go ahead
                    goWithFitnessApi();
                }

                @Override
                public void onConnectionSuspended(int cause) {
                    // Connection has been suspended so we show an error message
                    Log.w(TAG_LOG, "Connection Suspended!");
                }
            };

    /**
     * The Callback to manage errors
     */
    private final GoogleApiClient.OnConnectionFailedListener mOnConnectionFailedListener
            = new GoogleApiClient.OnConnectionFailedListener() {

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (!connectionResult.hasResolution()) {
                // If a solution is available we show the related Dialog
                GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(),
                        MainActivity.this, 0).show();
                return;
            }
            // We manage the error only if the authorization is not pending
            if (!authInProgress) {
                try {
                    authInProgress = true;
                    connectionResult.startResolutionForResult(MainActivity.this, REQUEST_OAUTH);
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG_LOG, "Error managing Google Play Service", e);
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // We test if the authInProgress is true to avoid duplication
        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }
        // We initialize GoogleApiClient object
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.API)
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mOnConnectionFailedListener)
                .build();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_session_data) {
            showSessionFragment();
            return true;
        } else if (id == R.id.action_list_data_sources) {
            showDataSourceFragment();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect to the Fitness API
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }
        // We have to delegate the method to the current Fragment
        if (mCurrentFragment != null) {
            mCurrentFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }

    /**
     * This method contains the logic for FitnessApi of our application
     */
    private void goWithFitnessApi() {
        // We add the Fragment for the DataSources if not already there
        showDataSourceFragment();
    }


    /**
     * Action that shows the Fragment for session management
     */
    private void showSessionFragment() {
        final SessionFragment sessionFragment = SessionFragment.create();
        mCurrentFragment = sessionFragment;
        getFragmentManager().beginTransaction()
                .replace(R.id.anchor_point, sessionFragment, SessionFragment.TAG)
                .addToBackStack(SessionFragment.TAG)
                .commit();
    }

    /**
     * Action that shows the Fragment for session management
     */
    private void showDataSourceFragment() {
        final DataSourceFragment dataSourceFragment = DataSourceFragment.create();
        mCurrentFragment = dataSourceFragment;
        getFragmentManager().beginTransaction()
                .replace(R.id.anchor_point, dataSourceFragment, DataSourceFragment.TAG)
                .addToBackStack(DataSourceFragment.TAG)
                .commit();
    }


    /**
     * @return The GoogleApiClient object
     */
    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }
}
