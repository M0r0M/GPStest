package com.example.gpstest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

public class MainActivity extends FragmentActivity implements
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener, 
LocationListener {
	
	TextView GPSlongitude;
	TextView GPSlatitude;
	TextView GPSaltitude;
	TextView GPSaccuracy;
	Timer updateTimer;
	Button reportButton;
	
	// date
	String dateFormat = "yyyy-MM-dd\tHH:mm:ss.SSS";
	SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
	
	// report
	String resultFile = "GeolocData.txt";
	
	// la carte
	GoogleMap map;
	
	 /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	
	// Define an object that holds accuracy and frequency parameters
    LocationRequest mLocationRequest;
    private static final int FASTEST_INTERVAL = 1000;
    public static final int UPDATE_INTERVAL = 5000;
    LocationClient mLocationClient;
    boolean mUpdatesRequested;
    
    // Global variable to hold the current location
    Location mCurrentLocation;
    
    // preferences
    SharedPreferences mPrefs;
    SharedPreferences.Editor mEditor;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		GPSlongitude = (TextView)findViewById(R.id.LongitudeValue);
		GPSlatitude = (TextView)findViewById(R.id.LatitudeValue);
		GPSaltitude = (TextView)findViewById(R.id.GPSaltitudeValue);
		GPSaccuracy = (TextView)findViewById(R.id.GPSaccuracyValue);
		reportButton = (Button)findViewById(R.id.SendReportButton);
		reportButton.setClickable(false);
		
		// Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        
        // Open the shared preferences
        mPrefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        // Get a SharedPreferences editor
        mEditor = mPrefs.edit();
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
        // Start with updates turned off
        mUpdatesRequested = false;
        
        
        // init la map
        
	}
	
	@Override
    protected void onPause() {
        // Save the current setting for updates
        mEditor.putBoolean("KEY_UPDATES_ON", mUpdatesRequested);
        mEditor.commit();
        super.onPause();
    }
	
	@Override
    protected void onStart() {
		super.onStart();
        mLocationClient.connect();
    }
	
	@Override
    protected void onResume() {
        /*
         * Get any previous setting for location updates
         * Gets "false" if an error occurs
         */
		super.onResume();
        if (mPrefs.contains("KEY_UPDATES_ON")) {
            mUpdatesRequested =
                    mPrefs.getBoolean("KEY_UPDATES_ON", false);

        // Otherwise, turn off location updates
        } else {
            mEditor.putBoolean("KEY_UPDATES_ON", false);
            mEditor.commit();
        }
    }
	
	/*
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     */
    @Override
    protected void onStop() {
        // If the client is connected
        if (mLocationClient.isConnected()) {
            /*
             * Remove location updates for a listener.
             * The current Activity is the listener, so
             * the argument is "this".
             */
            //removeLocationUpdates(this);
        }
        /*
         * After disconnect() is called, the client is
         * considered "dead".
         */
        mLocationClient.disconnect();
        super.onStop();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void startStopGPS(View v) {
		updateUI();
		updateFile(resultFile, mCurrentLocation);		
	}
	
	public void updateUI() {
				mCurrentLocation = mLocationClient.getLastLocation();
				GPSlongitude.setText(String.format("%s",mCurrentLocation.getLongitude()));
				GPSlatitude.setText(String.format("%s", mCurrentLocation.getLatitude()));
				GPSaltitude.setText(String.format("%s", mCurrentLocation.getAltitude()));
				GPSaccuracy.setText(String.format("%s", mCurrentLocation.getAccuracy()));
	}
	
	
	/*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        // If already requested, start periodic updates
        if (mUpdatesRequested) {
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
        }
    }
    
    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }
    
    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            //showErrorDialog(connectionResult.getErrorCode());
        }
    }
    
    // Define the callback method that receives location updates
    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        updateUI();
        updateFile(resultFile, location);
    }	
    
    public void updateFile(String FILE_NAME, Location location) {
    	String dataToWrite = String.format("%s\t%s\t%s\t%s\t%s\n",
    			sdf.format(new Date()),
    			location.getLatitude(),
    			location.getLongitude(),
    			location.getAltitude(),
    			location.getAccuracy());
    	try {
			FileOutputStream outputStream = openFileOutput(FILE_NAME, Context.MODE_APPEND);
			outputStream.write(dataToWrite.getBytes());
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	reportButton.setClickable(true);
    }
    
    public void sendReport(View v) {
		
		String aEmailList[] = { "matthieu.moro@me.com" };
		
		String resultsString = "";
		
		InputStream inputStream;
		try {
			inputStream = openFileInput(resultFile);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			// READ STRING OF UNKNOWN LENGTH
			StringBuilder sb = new StringBuilder();
		    char[] inputBuffer = new char[2048];
		    int l;
		    // FILL BUFFER WITH DATA
		    while ((l = inputStreamReader.read(inputBuffer)) != -1) {
		        sb.append(inputBuffer, 0, l);
		    }
		    resultsString = sb.toString();
		    inputStream.close();
		} catch (Exception e) {}
		
		// send the mail
				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.setType("message/rfc822");
				sendIntent.putExtra(Intent.EXTRA_EMAIL, aEmailList);
				sendIntent.putExtra(Intent.EXTRA_SUBJECT, String.format("Data recording %s", sdf.format(new Date())));
				sendIntent.putExtra(Intent.EXTRA_TEXT, resultsString);
						
				startActivity(Intent.createChooser(sendIntent, "Send mail..."));
    }
    
    public void deleteRecording(View view) {
		File results = new File(getApplicationContext().getFilesDir() + "/" + resultFile);
		if (results.exists()) {
			Toast.makeText(getApplicationContext(), "Data File Removed", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getApplicationContext(), "No Data File To Remove", Toast.LENGTH_SHORT).show();
		}
		results.delete();
	}
    
    public void showOnMap(View v) {
    	
    }
}
