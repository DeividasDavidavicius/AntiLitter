package com.example.siukslesv1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import com.example.siukslesv1.ml.Modelis;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;



public class CameraActivity extends AppCompatActivity {

    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    private static final int CAMERA_PERMISSION_CODE = 103;
    public static final int GALLERY_REQUEST_CODE = 105;
    public static final int LOCATION_REQUEST_CODE = 44;
    ImageView selectedImage;
    Button cameraBtn;
    Button galleryBtn;
    Button submitBtn;
    Button settingsButton;
    Button profileButton;
    String currentPhotoPath;
    String imageUri;
    StorageReference storageReference;
    TextInputEditText postName;
    RadioGroup radioGroup;
    RadioButton radioButton;
    int dumpSize;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private static final String POSTS = "posts";
    private FirebaseAuth mAuth;
    private String email;
    private Post post;
    private String locationInfo;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient FusedLocationClient;
    private String realPhotoPath;
    private String realPhotoPathCamera;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);




        //kvieciant locationa submite nespeja requestai suvaiksciot.
        FusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getCurrentLocation();
        setContentView(R.layout.activity_camera);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mAuth.getCurrentUser();
        email = mUser.getEmail();

        selectedImage = findViewById(R.id.displayImageView);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        submitBtn = findViewById(R.id.submitBtn);
        postName = findViewById(R.id.postName);
        radioGroup = findViewById(R.id.radioGroup);
        dumpSize = -1;

        storageReference = FirebaseStorage.getInstance().getReference();

        database = FirebaseDatabase.getInstance("https://siuksliu-programele-default-rtdb.europe-west1.firebasedatabase.app/");
        databaseReference = database.getReference(POSTS);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(200);

        cameraBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                askCameraPermissions();
            }
        });

        galleryBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = postName.getText().toString();

                try {
                    Modelis tflite = Modelis.newInstance(getApplicationContext());
                    Bitmap bitmap;
                    if (realPhotoPathCamera != null) {
                        bitmap = BitmapFactory.decodeFile(realPhotoPathCamera);
                    } else {
                        bitmap = BitmapFactory.decodeFile(realPhotoPath);
                    }

                    Bitmap resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
                    int[] pixels = new int[224 * 224];
                    resized.getPixels(pixels, 0, resized.getWidth(), 0, 0, resized.getWidth(), resized.getHeight());

                    float[] input = new float[224 * 224 * 3];
                    for (int i = 0; i < pixels.length; ++i) {
                        final int val = pixels[i];
                        input[i * 3 + 0] = (float) ((val & 0xFF) / 255.0);
                        input[i * 3 + 1] = (float) (((val >> 8) & 0xFF) / 255.0);
                        input[i * 3 + 2] = (float) (((val >> 16) & 0xFF) / 255.0);
                    }

                    TensorBuffer inputTensor = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
                    inputTensor.loadArray(input);

                    Modelis.Outputs outputs = tflite.process(inputTensor);
                    TensorBuffer outputTensor = outputs.getOutputFeature0AsTensorBuffer();
                    //TensorBuffer outputTensor = outputs.getOutputFeature0().getOutputAsTensorBuffer();

                    String label = (outputTensor.getFloatValue(0) > 0.85) ? "tvarkoma" : "netvarkoma";
                    if (label == "netvarkoma")
                    {
                        Toast toast = Toast.makeText(getApplicationContext(), "We didn't detect any trash in the picture you uploaded. To ensure that the contents of your photo are clear and visible, try adjusting the lighting or angle of your shot.", Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }

                    tflite.close();
                } catch (Exception e) {
                    // TODO Handle the exception
                }



                if(name.matches(""))
                {
                    Toast.makeText(CameraActivity.this, "Post must have a name!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(imageUri == null)
                {
                    Toast.makeText(CameraActivity.this, "You must select an image before submitting!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(dumpSize == -1)
                {
                    Toast.makeText(CameraActivity.this, "You must select dump size!" + dumpSize, Toast.LENGTH_SHORT).show();
                    return;
                }

                if(realPhotoPath!=null)
                {
                    locationInfo=getLocationFromImage(realPhotoPath);
                }


                //Creating unique ID for post
                UUID postID = generateKey();
                String id = postID.toString();

                Calendar calendar = Calendar.getInstance();
                long currentTime = calendar.getTimeInMillis();

                post = new Post(email, name, locationInfo, imageUri, dumpSize, currentTime);

                String keyID = databaseReference.push().getKey();
                databaseReference.child(keyID).setValue(post);
                Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
                startActivity(intent);
            }
        });
        // Initialize and assign variable
        BottomNavigationView bottomNavigationView=findViewById(R.id.bottom_navigation);

        // Set Home selected
        bottomNavigationView.setSelectedItemId(R.id.camera);

        // Perform item selected listener
        bottomNavigationView.setOnItemSelectedListener(item -> {

            switch(item.getItemId())
            {
                case R.id.home:
                    startActivity(new Intent(getApplicationContext(),MainActivity.class));
                    overridePendingTransition(0,0);
                    return true;
                case R.id.camera:
                    return true;
                case R.id.map:
                    startActivity(new Intent(getApplicationContext(),mapActivity.class));
                    overridePendingTransition(0,0);
                    return true;
            }
            return false;
        });
        settingsButton = findViewById(R.id.Settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToSettings();
            }
        });
        profileButton = findViewById(R.id.Profile);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToProfile();
            }
        });
    }

    private String getLocationFromImage(String path) {
        Float Latitude= Float.valueOf(0);
        Float Longitude=Float.valueOf(0);
        try {
            ExifInterface exif = new ExifInterface(path);
            String LATITUDE = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String LATITUDE_REF = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String LONGITUDE = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String LONGITUDE_REF = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);


            if((LATITUDE !=null)
                    && (LATITUDE_REF !=null)
                    && (LONGITUDE != null)
                    && (LONGITUDE_REF !=null))
            {

                if(LATITUDE_REF.equals("N")){
                    Latitude = convertToDegree(LATITUDE);
                }
                else{
                    Latitude = 0 - convertToDegree(LATITUDE);
                }

                if(LONGITUDE_REF.equals("E")){
                    Longitude = convertToDegree(LONGITUDE);
                }
                else{
                    Longitude = 0 - convertToDegree(LONGITUDE);
                }

            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return String.format("%f %f",Latitude,Longitude);
    }

    private Float convertToDegree(String stringDMS){
        Float result = null;
        String[] DMS = stringDMS.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        Double D0 = new Double(stringD[0]);
        Double D1 = new Double(stringD[1]);
        Double FloatD = D0/D1;

        String[] stringM = DMS[1].split("/", 2);
        Double M0 = new Double(stringM[0]);
        Double M1 = new Double(stringM[1]);
        Double FloatM = M0/M1;

        String[] stringS = DMS[2].split("/", 2);
        Double S0 = new Double(stringS[0]);
        Double S1 = new Double(stringS[1]);
        Double FloatS = S0/S1;

        result = new Float(FloatD + (FloatM/60) + (FloatS/3600));

        return result;
    }


    private void switchToSettings() {
        Intent switchActivityIntent = new Intent(this, settingsActivity.class);
        startActivity(switchActivityIntent);
    }
    private void switchToProfile() {
        Intent switchActivityIntent = new Intent(this, profileActivity.class);
        startActivity(switchActivityIntent);
    }

    private void askCameraPermissions() {
        if ((ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) &&
                ((ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED))) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_CODE);
        }
        else
        {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera Permission is Required to Use camera.", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CAMERA_REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK) {
                realPhotoPathCamera= currentPhotoPath;
                File f = new File(currentPhotoPath);
                //selectedImage.setImageURI(Uri.fromFile(f));
                Log.d("tag", "Absolute Url of Image is " + Uri.fromFile(f));

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);

                uploadImageToFirebase(f.getName(), contentUri);
            }
        }

        if(requestCode == GALLERY_REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK) {
                Uri contentUri = data.getData();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "." + getFileExt(contentUri);
                Log.d("tag", "onActivityResult: Gallery Image Uri: " + imageFileName);
                //selectedImage.setImageURI(contentUri);
                realPhotoPath= getRealPathFromURI(contentUri);
                uploadImageToFirebase(imageFileName, contentUri);
            }
        }
    }
    public String getRealPathFromURI(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        @SuppressWarnings("deprecation")
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }


    private void uploadImageToFirebase(String name, Uri contentUri) {
        final StorageReference image = storageReference.child("images/" + name);
        image.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d("tag", "onSuccess: Uploaded Image URI is " + uri.toString());
                        imageUri = uri.toString();
                        Picasso.get().load(imageUri).into(selectedImage);
                    }
                });
                Toast.makeText(CameraActivity.this, "Image Is Uploaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CameraActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getFileExt(Uri contentUri) {
        ContentResolver c = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));
    }

    @NonNull
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        //File storageDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir

        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //IFAS SUGADINA MAN VISKA HELP
        //if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {

            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        //}
    }

    public void checkSizeButton(View v)
    {

        int radioId = radioGroup.getCheckedRadioButtonId();
        radioButton = findViewById(radioId);

        if(radioButton.getText().toString().matches("Small")) {
            dumpSize = 1;
        }
        if(radioButton.getText().toString().matches("Medium")) {
            dumpSize = 2;
        }
        if(radioButton.getText().toString().matches("Large")) {
            dumpSize = 3;
        }

        //Toast.makeText(CameraActivity.this, "Selected Radio Button: " + radioButton.getText(), Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {

        if (checkPermissions()) {

            if (isLocationEnabled()) {
                FusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            locationInfo = String.format("%f %f",location.getLatitude(),location.getLongitude());
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {


        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5);
        locationRequest.setFastestInterval(0);
        locationRequest.setNumUpdates(1);


        FusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        FusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private LocationCallback locationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location LastLocation = locationResult.getLastLocation();
            locationInfo = String.format("%f %f",LastLocation.getLatitude(),LastLocation.getLongitude());
        }
    };

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getCurrentLocation();
        }
    }
    private UUID generateKey()
    {
        return UUID.randomUUID();
    }
}

