package uk.co.massimocarli.myfitapplication;


import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.SessionsApi;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;
import com.google.android.gms.fitness.result.SessionReadResult;
import com.google.android.gms.fitness.result.SessionStopResult;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This is the Fragment that manages Session and Data collection
 * Created by Massimo Carli on 04/11/14.
 */
public class SessionFragment extends ListFragment {

    /**
     * The Tag for the Log
     */
    private static final String TAG_LOG = SessionFragment.class.getName();

    /**
     * The Tag for this Fragment
     */
    public static final String TAG = "SessionFragment";

    /**
     * The Format for the Date
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MM yyyy HH:mm:sss");

    /**
     * One day in milliseconds
     */
    private static final long TEN_MINUTES = 10 * 60 * 1000L;

    /**
     * The Weak Reference to the MainActivity
     */
    private WeakReference<MainActivity> mActivityRef;

    /**
     * The Adapter for the DataSource
     */
    private ArrayAdapter<Subscription> mAdapter;

    /**
     * The List we use as a Model
     */
    private List<Subscription> mModel;

    /**
     * Static Factory Method for the DataSourceFragment to manage DataSource
     *
     * @return The DataSourceFragment Fragment instance
     */
    public static final SessionFragment create() {
        final SessionFragment fragment = new SessionFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This has a menu
        setHasOptionsMenu(true);
        setRetainInstance(true);
        // We initialize the model
        mModel = new LinkedList<Subscription>();
        // We initialize the Adapter for the Model
        mAdapter = new ArrayAdapter<Subscription>(getActivity(), R.layout.subscription_list_item, mModel) {

            class Holder {
                /**
                 * The TextView for the Data Source
                 */
                TextView sourceTextView;

                /**
                 * The TextView for the Data Type
                 */
                TextView typeTextView;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Holder holder = null;
                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity()).inflate(R.layout.subscription_list_item, null);
                    holder = new Holder();
                    convertView.setTag(holder);
                    holder.sourceTextView = (TextView) convertView.findViewById(R.id.subscription_data_source);
                    holder.typeTextView = (TextView) convertView.findViewById(R.id.subscription_data_type);
                } else {
                    holder = (Holder) convertView.getTag();
                }
                // We get the item
                final Subscription subscription = getItem(position);
                final DataSource dataSource = subscription.getDataSource();
                if (dataSource != null) {
                    final Device device = dataSource.getDevice();
                    holder.sourceTextView.setText(device.getManufacturer() + " " + device.getModel());
                } else {
                    holder.sourceTextView.setText("-");
                }
                final DataType dataType = subscription.getDataType();
                if (dataType != null) {
                    holder.typeTextView.setText(dataType.getName());
                } else {
                    holder.typeTextView.setText("-");
                }

                // We return the row
                return convertView;
            }
        };
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivityRef = new WeakReference<MainActivity>((MainActivity) activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View fragmentLayout = inflater.inflate(R.layout.fragment_session, null);
        // We return the Layout
        return fragmentLayout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // We attach the adapter to the ListView
        getListView().setAdapter(mAdapter);
        // We register for contextual events
        registerForContextMenu(getListView());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_subscription, menu);
        inflater.inflate(R.menu.menu_history, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater menuInflater = new MenuInflater(getActivity());
        menuInflater.inflate(R.menu.menu_subscription_context, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (R.id.action_remove_subscription == itemId) {
            // We remove the subscription
            removeSubscription(info.position);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (R.id.action_new_subscription == itemId) {
            createSubscription();
            return true;
        } else if (R.id.action_start_session == itemId) {
            // We start the session
            startSession();
            return true;
        } else if (R.id.action_stop_session == itemId) {
            // We stop the session
            stopSession();
            return true;
        } else if (R.id.action_show_session_data == itemId) {
            // Show the session data
            showSessionData();
            return true;
        } else if (R.id.action_dump_data == itemId) {
            // Dump Data
            dumpHistoryData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        listSubscriptions();
    }

    /**
     * Utility method that gets the Subscriptions list and updates the model
     */
    private void listSubscriptions() {
        // We check the presence of the Activity
        final MainActivity mainActivity = mActivityRef.get();
        if (mainActivity == null) {
            Log.w(TAG_LOG, "Lost MainActivity reference!!");
            return;
        }
        Fitness.RecordingApi.listSubscriptions(mainActivity.getGoogleApiClient(), DataType.TYPE_LOCATION_SAMPLE)
                .setResultCallback(new ResultCallback<ListSubscriptionsResult>() {
                    @Override
                    public void onResult(ListSubscriptionsResult listSubscriptionsResult) {
                        // we update the model with the new subscriptions list
                        mModel.clear();
                        mModel.addAll(listSubscriptionsResult.getSubscriptions());
                        mAdapter.notifyDataSetChanged();
                    }
                });
    }


    /**
     * Utility method that creates a Subscription for a Position data
     */
    private void createSubscription() {
        final MainActivity mainActivity = mActivityRef.get();
        if (mainActivity == null) {
            Log.w(TAG_LOG, "Lost MainActivity reference!!");
            return;
        }
        // We create a subscription for the Location data
        Fitness.RecordingApi.subscribe(mainActivity.getGoogleApiClient(), DataType.TYPE_LOCATION_SAMPLE)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // If everything is ok we check if the subscription was already present
                        if (status.isSuccess()) {
                            final int statusCode = status.getStatusCode();
                            if (statusCode == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                // The subscription was already present
                                showToast(R.string.subscription_already_present);
                            } else {
                                // Subscription created with success
                                showToast(R.string.subscription_created_successfully);
                                // We update the data in the list
                                listSubscriptions();
                            }
                        } else {
                            // Subscription creation failed
                            showToast(R.string.subscription_creation_error);
                        }
                    }
                });
    }

    /**
     * Utility method to remove the subscription
     *
     * @param subcriptionPos The subscription
     */
    private void removeSubscription(final int subcriptionPos) {
        final MainActivity mainActivity = mActivityRef.get();
        if (mainActivity == null) {
            Log.w(TAG_LOG, "Lost MainActivity reference!!");
            return;
        }
        // We get the Subscription to remove
        final Subscription toRemove = mModel.get(subcriptionPos);
        Fitness.RecordingApi.unsubscribe(mainActivity.getGoogleApiClient(), toRemove)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            showToast(R.string.subscription_removed_successfully);
                            // We update the list
                            listSubscriptions();
                        } else {
                            showToast(R.string.subscription_removed_error);
                        }
                    }
                });

    }

    /**
     * We start a session
     */
    private void startSession() {
        final MainActivity mainActivity = mActivityRef.get();
        if (mainActivity == null) {
            Log.w(TAG_LOG, "Lost MainActivity reference!!");
            return;
        }
        // The new sessionId
        final String newSessionId = FitSessionState.get(mainActivity).startSession();
        final String newSessionName = FitSessionState.get(mainActivity).getCurrentSessionName();
        Log.d(TAG_LOG, "Try to create session with id " + newSessionId);
        final long now = System.currentTimeMillis();
        // We create the Session object
        Session session = new Session.Builder()
                .setName(newSessionName)
                .setIdentifier(newSessionId)
                .setDescription("A testing session")
                .setStartTime(now, TimeUnit.MILLISECONDS)
                .setActivity(FitnessActivities.WALKING)
                .build();
        // We use the SessionApi to create the session
        Fitness.SessionsApi.startSession(mainActivity.getGoogleApiClient(), session)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            showToast(R.string.session_start_success);
                        } else {
                            showToast(R.string.session_start_failed);
                            Log.e(TAG_LOG, "Error starting session " + status.getStatusCode());
                            if (status.hasResolution()) {
                                try {
                                    status.startResolutionForResult(mainActivity, 999);
                                } catch (IntentSender.SendIntentException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                    }
                });
    }

    /**
     * We stop the session
     */
    private void stopSession() {
        final MainActivity mainActivity = mActivityRef.get();
        if (mainActivity == null) {
            Log.w(TAG_LOG, "Lost MainActivity reference!!");
            return;
        }
        // We get the current sessionId
        final String currentSessionId = FitSessionState.get(mainActivity).getCurrentSessionId();
        // We stop the session using the identifier
        Fitness.SessionsApi.stopSession(mainActivity.getGoogleApiClient(), currentSessionId)
                .setResultCallback(new ResultCallback<SessionStopResult>() {
                    @Override
                    public void onResult(SessionStopResult sessionStopResult) {
                        final Status status = sessionStopResult.getStatus();
                        if (status.isSuccess()) {
                            showToast(R.string.session_stop_success);
                            FitSessionState.get(mainActivity).stopSession();
                        } else {
                            showToast(R.string.session_stop_failed);
                        }
                    }
                });
    }

    /**
     * Utility method that shows the session data into a different application
     */
    private void showSessionData() {
        final MainActivity mainActivity = mActivityRef.get();
        if (mainActivity == null) {
            Log.w(TAG_LOG, "Lost MainActivity reference!!");
            return;
        }
        // The time interval
        final long endTime = System.currentTimeMillis();
        final long startTime = endTime - TEN_MINUTES;
        // We create the request
        final String currentSessionId = FitSessionState.get(mainActivity).startSession();
        final SessionReadRequest sessionReadRequest = new SessionReadRequest.Builder()
                .setSessionId(currentSessionId)
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(DataType.TYPE_LOCATION_SAMPLE)
                .build();
        Fitness.SessionsApi.readSession(mainActivity.getGoogleApiClient(), sessionReadRequest)
                .setResultCallback(new ResultCallback<SessionReadResult>() {
                    @Override
                    public void onResult(SessionReadResult sessionReadResult) {
                        final Status status = sessionReadResult.getStatus();
                        if (status.isSuccess()) {
                            // We get the session object
                            final List<Session> sessions = sessionReadResult.getSessions();
                            if (sessions != null && sessions.size() > 0) {
                                final Session session = sessions.get(0);
                                Intent intent = new SessionsApi.ViewIntentBuilder(mainActivity)
                                        .setSession(session)
                                        .build();
                                startActivity(intent);
                            }
                        }
                    }
                });


    }

    /**
     * This method execute a dump of the history data
     */
    private void dumpHistoryData() {
        final MainActivity mainActivity = mActivityRef.get();
        if (mainActivity == null) {
            Log.w(TAG_LOG, "Lost MainActivity reference!!");
            return;
        }
        // We want to define the interval of the last month
        final Calendar nowCal = Calendar.getInstance();
        final Date nowDate = nowCal.getTime();
        long endTime = nowCal.getTimeInMillis();
        nowCal.add(Calendar.WEEK_OF_YEAR, -1);
        long startTime = nowCal.getTimeInMillis();
        Log.i(TAG_LOG, "Start time: " + DATE_FORMAT.format(nowDate));
        Log.i(TAG_LOG, "End time: " + DATE_FORMAT.format(nowCal.getTime()));
        // We create the Request
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .read(DataType.TYPE_LOCATION_SAMPLE)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .enableServerQueries()
                .setLimit(5)
                .build();
        // We send the Request
        Fitness.HistoryApi.readData(mainActivity.getGoogleApiClient(), readRequest)
                .setResultCallback(new ResultCallback<DataReadResult>() {
                    @Override
                    public void onResult(DataReadResult dataReadResult) {
                        final Status status = dataReadResult.getStatus();
                        if (status.isSuccess()) {
                            final List<DataSet> dataSets = dataReadResult.getDataSets();
                            for (DataSet data : dataSets) {
                                log(data);
                            }
                        } else {
                            Log.e(TAG_LOG, "Error reading DataSet");
                            showToast(R.string.data_set_error);
                        }
                    }
                });
    }

    private void showToast(final int messageId) {
        Toast.makeText(getActivity(), messageId, Toast.LENGTH_SHORT).show();
    }

    /**
     * Utility that prints the DataSet
     *
     * @param dataSet The DataSet
     */
    private void log(final DataSet dataSet) {
        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG_LOG, "Data point:");
            Log.i(TAG_LOG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG_LOG, "\tStart: " + DATE_FORMAT.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG_LOG, "\tEnd: " + DATE_FORMAT.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for (Field field : dp.getDataType().getFields()) {
                Log.i(TAG_LOG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
            }
        }
    }

}
