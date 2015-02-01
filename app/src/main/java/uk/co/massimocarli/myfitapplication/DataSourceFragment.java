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
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.BleDevice;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.BleScanCallback;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.request.StartBleScanRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Massimo Carli on 04/11/14.
 */
public class DataSourceFragment extends ListFragment {

    /**
     * The Tag for the Log
     */
    private static final String TAG_LOG = DataSourceFragment.class.getName();

    /**
     * The Request Id for the Bluetooth activation
     */
    private static final int REQUEST_BLUETOOTH = 37;

    /**
     * The Tag for this Fragment
     */
    public static final String TAG = "DataSourceFragment";

    /**
     * The Number of sample for every time unit
     */
    public static final int SAMPLING_RATE = 10;

    /**
     * The timeout
     */
    public static final int TIMEOUT = 30;

    /**
     * The Weak Reference to the MainActivity
     */
    private WeakReference<MainActivity> mActivityRef;

    /**
     * The Adapter for the DataSource
     */
    private ArrayAdapter<DataSource> mAdapter;

    /**
     * The List we use as a Model
     */
    private List<DataSource> mModel;

    /**
     * The TextView we use to show the Location Data
     */
    private TextView mLocationTextView;


    /**
     * The Listener for the DataPoint from a DataSource
     */
    private final OnDataPointListener mOnDataPointListener = new OnDataPointListener() {

        @Override
        public void onDataPoint(DataPoint dataPoint) {
            // We get the result as a DataPoint. We get all the Field for the data
            // printing its value
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Sensor: ")
                    .append(dataPoint.getDataSource().getDevice().getManufacturer())
                    .append("\n");
            stringBuilder.append("time: ").append(dataPoint.getTimestampNanos()).append("\n");
            for (Field field : dataPoint.getDataType().getFields()) {
                // We get the specific field as Value object
                Value value = dataPoint.getValue(field);
                // We print the current value data
                stringBuilder.append(field.getName()).append(" = ").append(value).append("\n");
                Log.i(TAG_LOG, "DataPoint received: " + field.getName() + " = " + value);
            }
            mLocationTextView.setText(stringBuilder.toString());
        }
    };


    /**
     * Static Factory Method for the DataSourceFragment to manage DataSource
     *
     * @return The DataSourceFragment Fragment instance
     */
    public static final DataSourceFragment create() {
        final DataSourceFragment fragment = new DataSourceFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This has a menu
        setHasOptionsMenu(true);
        setRetainInstance(true);
        // We initialize the model
        mModel = new LinkedList<DataSource>();
        // We initialize the Adapter for the Model
        mAdapter = new ArrayAdapter<DataSource>(getActivity(), R.layout.datasource_list_item, mModel) {

            class Holder {
                /**
                 * The TextView for the Device of the DataSource
                 */
                TextView deviceTextView;

                /**
                 * The TextView for the Type of the DataSource
                 */
                TextView typeTextView;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Holder holder = null;
                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity()).inflate(R.layout.datasource_list_item, null);
                    holder = new Holder();
                    convertView.setTag(holder);
                    holder.deviceTextView = (TextView) convertView.findViewById(R.id.data_source_device);
                    holder.typeTextView = (TextView) convertView.findViewById(R.id.data_source_type);
                } else {
                    holder = (Holder) convertView.getTag();
                }
                // We get the item
                final DataSource dataSource = getItem(position);
                // We show the data
                holder.deviceTextView.setText(dataSource.getDevice().getManufacturer() + " " + dataSource.getDevice().getModel());
                holder.typeTextView.setText(dataSource.getDataType().getName());
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
        final View fragmentLayout = inflater.inflate(R.layout.fragment_datasource, null);
        // The reference to the mLocationTextView
        mLocationTextView = (TextView) fragmentLayout.findViewById(R.id.data_source_point);
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
        inflater.inflate(R.menu.menu_datasource, menu);
        inflater.inflate(R.menu.menu_ble, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater menuInflater = new MenuInflater(getActivity());
        menuInflater.inflate(R.menu.menu_datasource_context, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (R.id.action_data_subscribe == itemId) {
            // We subscribe the DataSource at given position
            subscribeDataSourceAt(info.position);
            return true;
        } else if (R.id.action_data_unsubscribe == itemId) {
            // We subscribe the DataSource at given position
            unsubscribeDataSourceAt(info.position);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.action_data_source_search == item.getItemId()) {
            searchDataSource();
            return true;
        } else if (R.id.action_data_search_ble == item.getItemId()) {
            // We start the Scan Ble
            startBleScan();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_BLUETOOTH:
                startBleScan();
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister for Context menu
        unregisterForContextMenu(getListView());
    }

    /**
     * This is the method that starts the search for the available DataSource
     */
    private void searchDataSource() {
        // Manage the Activity
        final MainActivity mainActivity = mActivityRef.get();
        if (mainActivity == null) {
            Log.w(TAG_LOG, "Lost MainActivity reference!!");
            return;
        }
        // We create the request for the Distance Delta as a raw type
        final DataSourcesRequest request = new DataSourcesRequest.Builder()
                .setDataTypes(DataType.TYPE_LOCATION_SAMPLE)
                .setDataSourceTypes(DataSource.TYPE_RAW)
                .build();
        // We start the request
        Fitness.SensorsApi.findDataSources(mainActivity.getGoogleApiClient(), request)
                .setResultCallback(new ResultCallback<DataSourcesResult>() {
                    @Override
                    public void onResult(DataSourcesResult dataSourcesResult) {
                        if (dataSourcesResult.getStatus().isSuccess()) {
                            final List<DataSource> dataSources = dataSourcesResult.getDataSources();
                            // We update the model
                            mModel.clear();
                            mModel.addAll(dataSources);
                            mAdapter.notifyDataSetChanged();
                        } else {
                            // Error. We show a Toast
                            Log.e(TAG_LOG, "Error accessing DataSource");
                        }
                    }
                });
    }

    /**
     * Utility method to subscribe the DataSource
     *
     * @param position The position of the DataSource in the model
     */
    private void subscribeDataSourceAt(final int position) {
        final MainActivity mainActivity = mActivityRef.get();
        if (mainActivity == null) {
            Log.w(TAG_LOG, "Lost MainActivity reference!!");
            return;
        }
        // We access the model to get the selected DataSource
        final DataSource dataSource = mModel.get(position);
        // We create the SensorRequest using its Builder
        final SensorRequest sensorRequest = new SensorRequest.Builder()
                .setDataSource(dataSource)
                .setDataType(DataType.TYPE_LOCATION_SAMPLE)
                .setSamplingRate(SAMPLING_RATE, TimeUnit.SECONDS)
                .setFastestRate(SAMPLING_RATE, TimeUnit.SECONDS)
                .setTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build();
        // We send the request for the Sensor data
        Fitness.SensorsApi.add(mainActivity.getGoogleApiClient(), sensorRequest, mOnDataPointListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG_LOG, "Listener subscription successful!");
                            Toast.makeText(getActivity(),
                                    R.string.action_data_source_subscribe_success,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.i(TAG_LOG, "Listener subscription error!");
                            Toast.makeText(getActivity(),
                                    R.string.action_data_source_subscribe_error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Utility method to unsubscribe the DataSource
     *
     * @param position The position of the DataSource in the model
     */
    private void unsubscribeDataSourceAt(final int position) {
        final MainActivity mainActivity = mActivityRef.get();
        if (mainActivity == null) {
            Log.w(TAG_LOG, "Lost MainActivity reference!!");
            return;
        }
        Fitness.SensorsApi.remove(
                mainActivity.getGoogleApiClient(),
                mOnDataPointListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG_LOG, "Listener removed successful!");
                            Toast.makeText(getActivity(),
                                    R.string.action_data_source_unsubscribe_success,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.i(TAG_LOG, "Listener removal error!");
                            Toast.makeText(getActivity(),
                                    R.string.action_data_source_unsubscribe_error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    /**
     * Utility method that starts the Scan for BLE
     */
    private void startBleScan() {
        final MainActivity mainActivity = mActivityRef.get();
        if (mainActivity == null) {
            Log.w(TAG_LOG, "Lost MainActivity reference!!");
            return;
        }
        // We create the callback for the scan process
        final BleScanCallback bleScanCallback = new BleScanCallback() {
            @Override
            public void onDeviceFound(final BleDevice device) {
                // Here we get the BleDevice that we want to claim as DataSource
                Fitness.BleApi.claimBleDevice(mainActivity.getGoogleApiClient(), device)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    Log.d(TAG_LOG, device + " successfully claimed!");
                                } else {
                                    Log.d(TAG_LOG, device + " claiming error!");
                                }
                            }
                        });

            }

            @Override
            public void onScanStopped() {
                // The Scan process stopped
                Log.d(TAG_LOG, "BLE Scanning stopped!");
            }
        };
        // We create the request for the scan process
        final StartBleScanRequest bleScanRequest = new StartBleScanRequest.Builder()
                .setDataTypes(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .setBleScanCallback(bleScanCallback)
                .build();
        // We start the scan
        Fitness.BleApi.startBleScan(mainActivity.getGoogleApiClient(), bleScanRequest)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            // Everything is ok so we want to connect to the device

                        } else {
                            // We check if the request failed because of the disabled BlueTooth
                            final int statusCode = status.getStatusCode();
                            if (FitnessStatusCodes.DISABLED_BLUETOOTH == statusCode) {
                                // In this case the bluetooth is disabled so we have to enable it
                                try {
                                    status.startResolutionForResult(mainActivity, REQUEST_BLUETOOTH);
                                } catch (IntentSender.SendIntentException e) {
                                    e.printStackTrace();
                                    Log.e(TAG_LOG, "Error enabling Bluetooth", e);
                                }
                            }

                        }
                    }
                });

    }

}
