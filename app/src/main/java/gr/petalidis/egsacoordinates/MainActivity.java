package gr.petalidis.egsacoordinates;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Locale;

import gr.petalidis.egsacoordinates.utilities.WGS84Converter;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_GPS_REQUEST_CODE = 2;


    private Location gpsLocation = null;

    private Activity mContext;

    private boolean requestLocationUpdates = true;

    private LocationCallback mLocationCallback;

    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mContext = this;


        if (savedInstanceState != null) {
            requestLocationUpdates = savedInstanceState.getBoolean("requestLocationUpdates");
        }

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    setGpsLocation(location);
                }
            }
        };
        // recovering the instance state
        checkLocationPermission();

       // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("requestLocationUpdates", requestLocationUpdates);
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }

    public void gpsToggle(View view) {
        Button button = findViewById(R.id.gpsButton);

        if (requestLocationUpdates) {
            stopLocationUpdates();
            requestLocationUpdates = false;
            button.setText("Έναρξη ενημέρωσης συν/γμένων");
        } else {
            requestLocationUpdates = true;
            checkLocationPermission();
            button.setText("Παύση ενημέρωσης συν/γμένων");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void setGpsLocation(Location location) {

        if (location != null &&
                (gpsLocation == null || location.distanceTo(gpsLocation) > 1
                        || location.getAccuracy() < gpsLocation.getAccuracy())) {
            gpsLocation = location;

            double[] coordinates = WGS84Converter.toGGRS87(gpsLocation.getLatitude(), gpsLocation.getLongitude());

            TextView xCoordinate =  findViewById(R.id.xCoordinate);
            TextView yCoordinate = findViewById(R.id.yCoordinate);
            TextView precision =  findViewById(R.id.precision);

            float accuracy = location.getAccuracy();

            xCoordinate.setText(String.format(Locale.forLanguageTag("el"), "%.2f", coordinates[0]));
            yCoordinate.setText(String.format(Locale.forLanguageTag("el"), "%.2f", coordinates[1]));
            precision.setText(String.format(Locale.forLanguageTag("el"), "%.2f", accuracy) + "μ");
        }
    }

    public boolean checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // TODO: Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new android.support.v7.app.AlertDialog.Builder(this)
                        .setTitle("Αίτημα χρήσης υπηρεσιών τοποθεσίας")
                        .setMessage("Η τοποθεσία χρησιμοποιείται για την τοποθέτηση του στάβλου όταν καταχωρείτε έλεγχο")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(mContext,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_GPS_REQUEST_CODE);
                            }
                        })
                        .create()
                        .show();


            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_GPS_REQUEST_CODE);
            }
            return false;
        } else {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            setGpsLocation(location);
                        }
                    });
            if (requestLocationUpdates) {
                LocationRequest currentLocationRequest = new LocationRequest();
                currentLocationRequest.setInterval(500)
                        .setFastestInterval(0)
                        .setMaxWaitTime(0)
                        .setSmallestDisplacement(0)
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                if (mFusedLocationClient != null) {
                    mFusedLocationClient.requestLocationUpdates(currentLocationRequest, mLocationCallback, null);
                }
            } else {
                stopLocationUpdates();
            }
            return true;
        }
    }

    private void showGpsFailure() {
        AlertDialog.Builder gpsAlertDialog = new AlertDialog.Builder(this);
        String msg = "Δεν ήταν δυνατή η ανάγνωση της τοποθεσίας. Δοκιμάσετε αργότερα";
        gpsAlertDialog.setTitle("Αδυναμία πρόσβασης στις υπηρεσίες τοποθεσίας").setMessage(msg)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        gpsAlertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_GPS_REQUEST_CODE:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    showGpsFailure();
                } else {
                    checkLocationPermission();
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationPermission();
    }
}
