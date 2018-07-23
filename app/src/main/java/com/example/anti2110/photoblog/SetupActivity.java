package com.example.anti2110.photoblog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private String userId;

    private Toolbar toolbar;
    private CircleImageView setupImage;
    private EditText setupName;
    private Button setupBtn;

    private Uri mainImageUri = null;

    private ProgressBar setupProgressBar;
    private TextView setupProgressBarText;

    private StorageReference storageRef;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    private boolean isChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        toolbar = (Toolbar) findViewById(R.id.setup_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Account Settings");

        setupProgressBar = (ProgressBar) findViewById(R.id.setup_progressbar);
        setupProgressBarText = (TextView) findViewById(R.id.setup_progressbar_text);

        setupName = (EditText) findViewById(R.id.setup_name);
        setupBtn = (Button) findViewById(R.id.setup_btn);
        setupBtn.setEnabled(false);

        userId = mAuth.getCurrentUser().getUid();

        firestore.collection("Users")
                .document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()) {
                            if(task.getResult().exists()) {
                                Toast.makeText(SetupActivity.this, "Data exists", Toast.LENGTH_SHORT).show();

                                String name = task.getResult().getString("name");
                                String image = task.getResult().getString("image");

                                mainImageUri = Uri.parse(image);

                                setupName.setText(name);

                                RequestOptions placeholderRequest = new RequestOptions();
                                placeholderRequest.placeholder(R.drawable.avatar);

                                Glide.with(SetupActivity.this)
                                        .setDefaultRequestOptions(placeholderRequest)
                                        .load(image)
                                        .into(setupImage);

                            } else {
                                Toast.makeText(SetupActivity.this, "Data doesn't exists", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String errorMessage = task.getException().getMessage();
                            Toast.makeText(SetupActivity.this, "FireStore Retrieve Error : "+errorMessage, Toast.LENGTH_SHORT).show();
                        }
                        setupProgressBar.setVisibility(View.INVISIBLE);
                        setupProgressBarText.setVisibility(View.INVISIBLE);
                        setupBtn.setEnabled(true);
                    }
                });

        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = setupName.getText().toString();
                if (!TextUtils.isEmpty(username) && mainImageUri != null) {
                setupProgressBar.setVisibility(View.VISIBLE);
                setupProgressBarText.setVisibility(View.VISIBLE);
                if(isChanged) {


                        userId = mAuth.getCurrentUser().getUid();

                        StorageReference imagePath = storageRef.child("profile_images").child(userId + ".jpg");
                        imagePath.putFile(mainImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if (task.isSuccessful()) {

                                    storeFirestore(task, username);

                                } else {
                                    String errorMessage = task.getException().getMessage();
                                    Toast.makeText(SetupActivity.this, "Image Error : " + errorMessage, Toast.LENGTH_SHORT).show();
                                }

                                setupProgressBar.setVisibility(View.INVISIBLE);
                                setupProgressBarText.setVisibility(View.INVISIBLE);
                            }
                        });
                    } else {
                    storeFirestore(null, username);
                    }
                }
            }
        });

        setupImage = (CircleImageView) findViewById(R.id.setup_image);
        setupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(ContextCompat.checkSelfPermission(SetupActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(SetupActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                        ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    } else {
                        bringImagePicker();
                    }
                } else {
                    bringImagePicker();
                }
            }

            private void bringImagePicker() {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1, 1)
                        .start(SetupActivity.this);
            }

        });

    }

    private void storeFirestore(Task<UploadTask.TaskSnapshot> task, String username) {

        Uri download_uri;

        if(task != null) {
            download_uri = task.getResult().getDownloadUrl();
        } else {
            download_uri = mainImageUri;
        }


        Map<String, String> userMap = new HashMap<>();
        userMap.put("name", username);
        userMap.put("image", download_uri.toString());

        firestore.collection("Users").document(userId).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    Toast.makeText(SetupActivity.this, "The user settings are updated", Toast.LENGTH_SHORT).show();
                    Intent mainIntent = new Intent(SetupActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                    finish();
                } else {
                    String errorMessage = task.getException().getMessage();
                    Toast.makeText(SetupActivity.this, "FireStore Error : "+errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                mainImageUri = result.getUri();
                setupImage.setImageURI(mainImageUri);

                isChanged = true;

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }

        }

    }


}
