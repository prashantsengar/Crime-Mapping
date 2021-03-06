package com.crime_mapping.electrothon.sos;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Date;

import static java.lang.StrictMath.abs;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final String ANONYMOUS = "anonymous";
    GoogleApiClient gac;
    String prime="";
    LocationRequest locationRequest;
    private static final int UPDATE_INTERVAL = 15 * 1000;
    private static final int FASTEST_UPDATE_INTERVAL = 2 * 1000;
    private static final int notification_request_code = 100;
    FusedLocationProviderClient mFusedLocationProviderClient;
    Location lastLocation,lastLocationGpsProvider;


    private static final String BACKGROUND_SERVICE_STATUS = "bgServiceStatus";
    SharedPreferences sharedpreferences;
    private String MyPREFERENCES="SOS_DATA";
    private boolean isServiceBackground;

    TextView tv;
    Button b;


    //Firebase Variables
//    static {
//        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
//    }
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseuser;
    FirebaseDatabase database;
    DatabaseReference myRef,myUserRef;
    private String mUsername;
    private String mPhotoUrl;
    private ChildEventListener childEventListener;
    private String mEmail;
    private String mUid;
    public String a1,a2;
    Double l1,l2,ll1,ll2;
    //Notification
    NotificationManager nm;
    String CHANNEL_ID = "my_sos_channel";// The id of the channel.
    Notification n;
    Notification.Builder nb;

    private static final String TAG = "MainActivity";

    @Override
    protected void onStart() {
        gac.connect();
        if (childEventListener != null) {
            myRef.addChildEventListener(childEventListener);
            Log.d(TAG, "onStart: ChildEventListener Attached");
        }

        stopService(new Intent(this,MyService.class));
        super.onStart();
    }

    @Override
    protected void onStop() {
        gac.disconnect();
//        if (childEventListener != null) {
//            myRef.removeEventListener(childEventListener);
//            Log.d(TAG, "onStop: ChildEventListener Removed");
//        }

        if(isServiceBackground&&FirebaseAuth.getInstance().getCurrentUser()!=null)
        {
            startService(new Intent(this,MyService.class));
            Log.d(TAG, "onStop: starting service");
        }
        super.onStop();
    }







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv=findViewById(R.id.textView);
        b=findViewById(R.id.button);

        DatabaseReference mRootRef= FirebaseDatabase.getInstance().getReference();
//        String uId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference locationRef=mRootRef.child("location").child(uId);
//        locationRef.child("lat").setValue("31.7166");
//        locationRef.child("lon").setValue("76.5333");
//        locationRef.setValue(new LatLng(Float.valueOf("31.7166"), Float.valueOf("76.5333")));


        nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        CharSequence name = "SOS ALERT";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    name,
                    importance);
            nm.createNotificationChannel(channel);
        }


        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        isServiceBackground=sharedpreferences.getBoolean(BACKGROUND_SERVICE_STATUS,true);
        if(isServiceBackground)
            b.setText("Stop background Notification");
        else
            b.setText("Start background Notification");




        /* firebase initialization */
        mUsername = ANONYMOUS;
//        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mFirebaseAuth=FirebaseAuth.getInstance();
        mFirebaseuser=mFirebaseAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Locations");
        myUserRef=database.getReference("Users");
        myRef.keepSynced(true);

        if(mFirebaseuser==null)
        {
            startActivity(new Intent(this,LoginActivity.class));
            finish();
            return;
        }
        else {

            mEmail=mFirebaseuser.getEmail();
            if(!TextUtils.isEmpty(mFirebaseuser.getDisplayName()))
                mUsername=mFirebaseuser.getDisplayName();
            else {
                mUsername = mEmail.split("@")[0];
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(mUsername).build();

                mFirebaseuser.updateProfile(profileUpdates)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "onComplete: name intialized as "+mUsername);
                                }
                            }
                        });



            }
            mUid=mFirebaseuser.getUid();
            if(mFirebaseuser.getPhotoUrl()!=null)
                mPhotoUrl=mFirebaseuser.getPhotoUrl().toString();
            Toast.makeText(this, "Welcome\n"+mUsername, Toast.LENGTH_SHORT).show();

        }

         childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());

//                FirebaseLocationData fld = dataSnapshot.getValue(FirebaseLocationData.class);
//                Toast.makeText(MainActivity.this, fld.getEmail()+" added", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());


               final FirebaseLocationData fld = dataSnapshot.getValue(FirebaseLocationData.class);
                Log.d(TAG, "onChildChanged: Creating Notification");
                myUserRef.child(fld.getUid()).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String sos_name=dataSnapshot.getValue(String.class);
                        nb= new Notification.Builder(MainActivity.this);
                        nb.setContentTitle("Emergency");
                        nb.setContentText("SOS broadcasted from "+sos_name);
                        nb.setSmallIcon(android.R.drawable.ic_dialog_alert);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            nb.setChannelId(CHANNEL_ID);
                        }
                        nb.setDefaults(Notification.DEFAULT_ALL);
                        Intent i =new Intent(MainActivity.this,MapsActivity.class);
                        Bundle b = new Bundle();
                        b.putDouble("lat", fld.getLatitude());
                        b.putDouble("long", fld.getLongitude());
                        b.putString("name", sos_name);
                        b.putString("time",fld.getSos_time());
                        i.putExtras(b);;
                        nb.setAutoCancel(false);
                        PendingIntent pi =PendingIntent.getActivity(MainActivity.this,notification_request_code,i,PendingIntent.FLAG_UPDATE_CURRENT);
                        nb.setContentIntent(pi);
                        n=nb.build();
                        nm.notify(notification_request_code,n);
                        Log.d(TAG, "onDataChange: NOTIFICATION CREATED");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });





//                Toast.makeText(MainActivity.this,fld.getEmail()+" changed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

//                String locKey = dataSnapshot.getKey();


            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());
                FirebaseLocationData fld = dataSnapshot.getValue(FirebaseLocationData.class);
                String locKey = dataSnapshot.getKey();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, ":onCancelled", databaseError.toException());
            }
        };
        if(childEventListener!=null)
            Log.d(TAG, "onCreate: childEventListenerCreated");



        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL);



        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        gac = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.firebase_menu,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.profile:
                startActivity(new Intent(MainActivity.this,ProfileActivity.class));

                return true;
            case R.id.sign_out:
                mFirebaseAuth.signOut();
                mUsername=ANONYMOUS;
                startActivity(new Intent(MainActivity.this,LoginActivity.class));
                finish();
                return  true;
            case R.id.view_location:
                startActivity(new Intent(MainActivity.this,LocationListActivity.class));
                return true;
            case R.id.info:
                startActivity(new Intent(MainActivity.this,AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public void startSos(View view) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
//        mFusedLocationProviderClient.getLastLocation()
//                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Location> task) {
//                        if (task.isSuccessful() && task.getResult() != null) {
//                            lastLocation = task.getResult();
//
////                            txtLatitude.setText(String.valueOf(lastLocation.getLatitude()));
////                            txtLongitude.setText(String.valueOf(lastLocation.getLongitude()));
//
//                        } else {
//                            Log.w(TAG, "getLastLocation:exception", task.getException());
////                            showSnackbar(getString(R.string.no_location_detected));
//                        }
//                    }
//                });

        LocationManager lm= (LocationManager) getSystemService(LOCATION_SERVICE);
        lastLocationGpsProvider=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);


        //write data to firebase

        if(lastLocationGpsProvider!=null)
            updateUserLocationToFirebase(lastLocationGpsProvider);
        else
        {
            lastLocationGpsProvider=LocationServices.FusedLocationApi.getLastLocation(gac);
            if(lastLocationGpsProvider!=null)
            updateUserLocationToFirebase(lastLocationGpsProvider);
        }



//        Toast.makeText(this, "GPS location without google client\n"+lm.getLastKnownLocation(LocationManager.GPS_PROVIDER).toString(), Toast.LENGTH_SHORT).show();



    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            return;
        }
        else{
//            Toast.makeText(this, "Permission is given", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Permission is already given");
        }

        // TODO: use fusedlocaionproviderclient
        /* mFusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations, this can be null.

                        if (location != null) {
                            // Logic to handle location object
                            lastLocation=location;
                        }
                    }
                });
        */
        Location ll = LocationServices.FusedLocationApi.getLastLocation(gac);


        Log.d(TAG, "LastLocation from Deprecated: " + (ll == null ? "NO LastLocation" : ll.toString()));
//        tv.setText("LastLocation from Deprecated: " + (ll == null ? "NO LastLocation" : ll.toString()));
//        Log.d(TAG, "LastLocation: " + (ll == null ? "NO LastLocation" : lastLocation.toString()));
         updateUI(ll);

        LocationServices.FusedLocationApi.requestLocationUpdates(gac, locationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(MainActivity.this, "onConnectionFailed: \n" + connectionResult.toString(),
                Toast.LENGTH_LONG).show();
        Log.d(TAG, connectionResult.toString());

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            updateUI(location);
        }

    }
    private void updateUI(Location loc) {
        Log.d(TAG, "updateUI");
        a1 = Double.toString(loc.getLatitude());
        l1 = loc.getLatitude();
        l2 = loc.getLongitude();
        a2 = Double.toString(loc.getLongitude());
        tv.setText(Double.toString(loc.getLatitude()) + '\n' + Double.toString(loc.getLongitude()) + '\n' + DateFormat.getTimeInstance().format(loc.getTime()));
    }
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }



    private void updateUserLocationToFirebase( Location location) {
        DatabaseReference d1 = FirebaseDatabase.getInstance().getReference().child("spots");
        d1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot dsp : dataSnapshot.getChildren()){
                    String a = dsp.child("lat").getValue().toString();

                    String b = dsp.child("lon").getValue().toString();
                    String t = dsp.child("details").getValue().toString();
//                    LatLng points = new LatLng(Double.valueOf(a),Double.valueOf(b));
//                    ll1 = Double.valueOf(a);
//                    ll2 = Double.valueOf(b);
//                    if(abs((ll1-l1))<=0.0002 && abs(ll2-l2)<0.0002)
//                    {p
                        Toast.makeText(MainActivity.this,t,Toast.LENGTH_SHORT).show();
//                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        FirebaseLocationData fld= new FirebaseLocationData(mEmail,location.getLatitude() ,location.getLongitude(),DateFormat.getTimeInstance().format(location.getTime()),DateFormat.getTimeInstance().format(new Date()));
        fld.setUid(mUid);
        myUserRef.child(mUid).child("name").setValue(mUsername);
        myRef.child(mUid).setValue(fld);
    }


    public void changeServiceState(View view) { //

       isServiceBackground = !isServiceBackground;
       SharedPreferences.Editor editor=sharedpreferences.edit();
       editor.putBoolean(BACKGROUND_SERVICE_STATUS,isServiceBackground);
       editor.apply();
//        tv.setText(String.valueOf(isServiceBackground));
       if(isServiceBackground)
       {
           b.setText("Stop background Notification");

           Toast.makeText(this, "Background Notification Started\nYou will get SOS notification even if app is closed", Toast.LENGTH_SHORT).show();

       }
       else
       {
           b.setText("Start background Notification");

           Toast.makeText(this, "Background Notification Stopped\nYou won't get notification if app is closed\nPlease Turn it back on", Toast.LENGTH_SHORT).show();
       }


    }

    public void Update_loc(View view) {
        //write code for police sections
        Toast.makeText(this,a1+" "+a2,Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this,update_spot.class);
        Bundle extras = new Bundle();
        extras.putString("latti", a1);
        extras.putString("longgi", a2);
        intent.putExtras(extras);
        startActivity(intent);

    }

    public void unsafe_nearby(View view) {
        Intent intent = new Intent(this,MapsActivity2.class);
        Bundle bundle = new Bundle();
        bundle.putString("lat",a1);
        bundle.putString("lon",a2);
        intent.putExtras(bundle);
        startActivity(intent);

    }

    public void routes(View view) {
        Intent intent = new Intent(this,MapsActivity3.class);
        startActivity(intent);
    }

    public void share_loc(View view) {
        String uId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference().child("Locations").child(uId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                prime = dataSnapshot.child("prime").getValue().toString();
                Toast.makeText(MainActivity.this,prime,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

//        Toast.makeText(this,"services started...",Toast.LENGTH_SHORT).show();
        Intent i=new Intent(this,MyService.class);
        startService(i);
    }

    public void enter_prime(View view) {
        Intent intent = new Intent(this,prime_contacts.class);
        startActivity(intent);
    }
}
