package ru.pekcherkin.mytaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import de.hdodenhof.circleimageview.CircleImageView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    Marker driverMarker, pickUpMarker;
    GeoQuery geoQuery;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String customerId;
    private DatabaseReference customerDatabaseRef;
    private DatabaseReference driversAvailableRef;
    private DatabaseReference driversRef;
    private DatabaseReference driversLocationRef;
    LatLng customerPosition;
    private int radius = 1;
    Boolean driverFound = false, requestType = false;
    private String driverFoundID;
    private Button customerLogoutButton, settingsButton;
    private Button callTaxiButton;
    private ValueEventListener driverLocationRefListener;
    private TextView txtName, txtPhone, txtCarName;
    private CircleImageView driverPhoto;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);

        customerLogoutButton = (Button) findViewById(R.id.customer_logout_button);
        settingsButton = (Button) findViewById(R.id.customer_settings_button);
        callTaxiButton = (Button) findViewById(R.id.customer_order_button);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        customerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customers Requests");
        driversAvailableRef = FirebaseDatabase.getInstance().getReference().child("DriverAvailable");
        driversLocationRef = FirebaseDatabase.getInstance().getReference().child("DriverWorking");

        txtName = (TextView) findViewById(R.id.driver_name);
        txtPhone = (TextView) findViewById(R.id.driver_phone_number);
        txtCarName = (TextView) findViewById(R.id.driver_car);
        driverPhoto = (CircleImageView)findViewById(R.id.driver_photo);
        relativeLayout = findViewById(R.id.rel1);

        relativeLayout.setVisibility(View.INVISIBLE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomersMapActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Customers");
                startActivity(intent);
            }
        });

        callTaxiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (requestType) {
                    requestType = false;

                    geoQuery.removeAllListeners();
                    driversLocationRef.removeEventListener(driverLocationRefListener);

                    if (driverFound != null) {
                        driversRef = FirebaseDatabase.getInstance().getReference().child("User").child("Drivers").child(driverFoundID)
                                .child("CustomerRideID");
                        driversRef.removeValue();
                        driverFoundID = null;
                    }

                    driverFound = false;
                    radius = 1;
                    GeoFire geoFire = new GeoFire(customerDatabaseRef);
                    geoFire.removeLocation(customerId);

                    if (pickUpMarker != null) {
                        pickUpMarker.remove();
                    }

                    if (driverMarker != null) {
                        driverMarker.remove();
                    }

                    callTaxiButton.setText("Вызвать такси");

                } else {
                    requestType = true;
                    GeoFire geoFire = new GeoFire(customerDatabaseRef);
                    geoFire.setLocation(customerId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                    customerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    pickUpMarker = mMap.addMarker(new MarkerOptions().position(customerPosition).title("Я здесь").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                    callTaxiButton.setText("Поиск водителя...");
                    getNearbyDrivers();
                }
            }
        });

        customerLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                logoutCustomer();
            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        googleApiClient.connect();
    }

    private void logoutCustomer() {
        Intent WelcomeIntent = new Intent(CustomersMapActivity.this, WelcomeActivity.class);
        startActivity(WelcomeIntent);
        finish();
    }

    private void getNearbyDrivers() {
        GeoFire geoFire = new GeoFire(driversAvailableRef);

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(customerPosition.latitude, customerPosition.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestType) {
                    driverFound = true;
                    driverFoundID = key;

                    driversRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideID", customerId);
                    driversRef.updateChildren(driverMap);
                    getDriverLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {
                    radius += 1;
                    getNearbyDrivers();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void getDriverLocation() {
        driverLocationRefListener = driversLocationRef.child(driverFoundID).child("l").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestType) {
                    List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    callTaxiButton.setText("Водитель найден");
                    getDriverInformation();
                    relativeLayout.setVisibility(View.VISIBLE);


                    if (driverLocationMap.get(0) != null) {
                        locationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                    }
                    if (driverLocationMap.get(1) != null) {
                        locationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);

                    if (driverMarker != null) {
                        driverMarker.remove();
                    }

                    Location location1 = new Location("");
                    location1.setLatitude(customerPosition.latitude);
                    location1.setLongitude(customerPosition.longitude);

                    Location location2 = new Location("");
                    location2.setLatitude(driverLatLng.latitude);
                    location2.setLongitude(driverLatLng.longitude);

                    float distance = location1.distanceTo(location2);
                    if (distance < 100) {
                        callTaxiButton.setText("Ваше такси подъезжает");
                    } else {
                        callTaxiButton.setText("Расстояние до такси" + String.valueOf(distance));
                    }
                    driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng)
                            .title("Ваше такси тут").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @Override
    protected void onStop() {
        super.onStop();
    }
    private void getDriverInformation(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("User").child("Drivers").child(driverFoundID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String name = snapshot.child("name").getValue().toString();
                    String phone = snapshot.child("phone").getValue().toString();
                    String car = snapshot.child("car").getValue().toString();
                    txtName.setText(name);
                    txtPhone.setText(phone);
                    txtCarName.setText(car);

                    if(snapshot.hasChild("image")) {
                        String image = snapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(driverPhoto);
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}
