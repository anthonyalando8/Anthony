package com.softtronic.socisnap;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Objects;

public class EditProfilePageActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    StorageReference storageReference;
    String storagePath = "PostImages/";
    ImageView set;
    TextView profilePic, editName, editPassword, editBio;
    private static final int CAMERA_REQUEST = 100;
    private static final int STORAGE_REQUEST = 200;
    private static final int IMAGE_PICK_GALLERY_REQUEST = 300;
    private static final int IMAGE_PICK_CAMERA_REQUEST = 400;
    String [] cameraPermission;
    String [] storagePermission;
    Uri imageUri;
    String profileOrCoverPhoto;
    MaterialButton finishEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile_page);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Edit Profile");
        profilePic = findViewById(R.id.profilepic);
        editName = findViewById(R.id.editname);
        set = findViewById(R.id.setting_profile_image);
        editPassword = findViewById(R.id.changepassword);
        editBio = findViewById(R.id.editBio);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("UserProfile");
        databaseReference = firebaseDatabase.getReference("Users");
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        Query query = databaseReference.orderByChild("email").equalTo(firebaseUser.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {

                    String image = "" + dataSnapshot1.child("image").getValue();

                    try {
                        Glide.with(EditProfilePageActivity.this).load(image).into(set);
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        finishEdit = findViewById(R.id.openMain);
        finishEdit.setOnClickListener(v->{
            Intent intent = new Intent(EditProfilePageActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        editPassword.setOnClickListener(v -> showPasswordChangeDialog());

        profilePic.setOnClickListener(v -> {
            profileOrCoverPhoto = "image";
            showImagePicDialog();
        });

        editName.setOnClickListener(v -> showNamePhoneUpdate());
        editBio.setOnClickListener(v->{
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LinearLayout linearLayout = new LinearLayout(this);
            builder.setTitle("Update Bio");
            linearLayout.setPadding(10,10,10,10);
            final EditText bio = new EditText(this);
            bio.setHint("Update Bio");
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.addView(bio);
            builder.setPositiveButton("Update", (dialog, which) -> {
                final String value = bio.getText().toString().trim();
                if (!TextUtils.isEmpty(value)) {
                    AlertDialog alertDialog = Progress.createAlertDialog(this, "Updating...");
                    alertDialog.show();
                    // Here we are updating the new name
                    HashMap<String, Object> result = new HashMap<>();
                    result.put("bio", value);
                    databaseReference.child(firebaseUser.getUid()).updateChildren(result).addOnSuccessListener(aVoid -> {
                        alertDialog.dismiss();
                        Toast.makeText(EditProfilePageActivity.this, " Updated ", Toast.LENGTH_LONG).show();
                    }).addOnFailureListener(e -> {
                        alertDialog.dismiss();
                        Toast.makeText(EditProfilePageActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which)->dialog.dismiss());
            builder.setView(linearLayout);
            builder.create().show();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Query query = databaseReference.orderByChild("email").equalTo(firebaseUser.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {

                    String image = "" + dataSnapshot1.child("image").getValue();

                    try {
                        Glide.with(EditProfilePageActivity.this).load(image).into(set);
                    } catch (Exception e) {
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        editPassword.setOnClickListener(v -> showPasswordChangeDialog());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Query query = databaseReference.orderByChild("email").equalTo(firebaseUser.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {

                    String image = "" + dataSnapshot1.child("image").getValue();
                    try {
                        Glide.with(EditProfilePageActivity.this).load(image).into(set);
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        editPassword.setOnClickListener(v -> showPasswordChangeDialog());
    }

    // checking storage permission ,if given then we can add something in our storage
    private Boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    // requesting for storage permission
    private void requestStoragePermission() {
        requestPermissions(storagePermission, STORAGE_REQUEST);
    }

    // checking camera permission ,if given then we can click image using our camera
    private Boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    // requesting for camera permission if not given
    private void requestCameraPermission() {
        requestPermissions(cameraPermission, CAMERA_REQUEST);
    }

    // We will show an alert box where we will write our old and new password
    private void showPasswordChangeDialog() {

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_update_password, null);
        final EditText oldPassword = view.findViewById(R.id.oldpasslog);
        final EditText newPassword = view.findViewById(R.id.newpasslog);
        Button editPassword = view.findViewById(R.id.updatepass);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        dialog.show();
        editPassword.setOnClickListener(v -> {
            String oldPassString = oldPassword.getText().toString().trim();
            String newPasswordString = newPassword.getText().toString().trim();
            if (TextUtils.isEmpty(oldPassString)) {
                Toast.makeText(EditProfilePageActivity.this, "Current Password cant be empty", Toast.LENGTH_LONG).show();
                return;
            }
            if (TextUtils.isEmpty(newPasswordString)) {
                Toast.makeText(EditProfilePageActivity.this, "New Password cant be empty", Toast.LENGTH_LONG).show();
                return;
            }
            dialog.dismiss();
            updatePassword(oldPassString, newPasswordString);
        });
    }

    // Now we will check that if old password was authenticated
    // correctly then we will update the new password
    private void updatePassword(String oldPass, final String newPass) {
        AlertDialog alertDialog = Progress.createAlertDialog(this, "Working...");
        alertDialog.show();
        final FirebaseUser user = firebaseAuth.getCurrentUser();
        AuthCredential authCredential = null;
        if (user != null) {
            authCredential = EmailAuthProvider.getCredential(Objects.requireNonNull(user.getEmail()), oldPass);
        }
        Objects.requireNonNull(user).reauthenticate(authCredential)
                .addOnSuccessListener(aVoid -> user.updatePassword(newPass)
                        .addOnSuccessListener(aVoid1 -> {
                            alertDialog.dismiss();
                            Toast.makeText(EditProfilePageActivity.this, "Successful", Toast.LENGTH_LONG).show();
                        }).addOnFailureListener(e -> {
                            alertDialog.dismiss();
                            Toast.makeText(EditProfilePageActivity.this, "Failed", Toast.LENGTH_LONG).show();
                        })).addOnFailureListener(e -> {
                    alertDialog.dismiss();
                    Toast.makeText(EditProfilePageActivity.this, "Failed", Toast.LENGTH_LONG).show();
                });
    }

    // Updating name
    private void showNamePhoneUpdate() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update " + "name");

        // creating a layout to write the new name
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10, 10, 10, 10);
        final EditText editText = new EditText(this);
        editText.setHint("Enter " + "name");
        layout.addView(editText);
        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            final String value = editText.getText().toString().trim();
            if (!TextUtils.isEmpty(value)) {
                AlertDialog alertDialog = Progress.createAlertDialog(this, "Working...");
                alertDialog.show();
                // Here we are updating the new name
                HashMap<String, Object> result = new HashMap<>();
                result.put("name", value);
                databaseReference.child(firebaseUser.getUid()).updateChildren(result).addOnSuccessListener(aVoid -> {
                    alertDialog.dismiss();
                    Toast.makeText(EditProfilePageActivity.this, " Updated ", Toast.LENGTH_LONG).show();
                }).addOnFailureListener(e -> {
                    alertDialog.dismiss();
                    Toast.makeText(EditProfilePageActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    // Here we are showing image pic dialog where we will select
    // and image either from camera or gallery
    private void showImagePicDialog() {
        String [] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image from");
        builder.setItems(options, (dialog, which) -> {
            // if access is not given then we will request for permission
            if (which == 0) {
                if (!checkCameraPermission()) {
                    requestCameraPermission();
                } else {
                    pickFromCamera();
                }
            } else if (which == 1) {
                if (!checkStoragePermission()) {
                    requestStoragePermission();
                } else {
                    pickFromGallery();
                }
            }
        });
        builder.create().show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if(data != null){
                if (requestCode == IMAGE_PICK_GALLERY_REQUEST) {
                    imageUri = data.getData();
                    uploadProfileCoverPhoto(imageUri);
                }
                if (requestCode == IMAGE_PICK_CAMERA_REQUEST) {
                    imageUri = data.getData();
                    uploadProfileCoverPhoto(imageUri);
                }
                Picasso.get().load(imageUri).into(set);
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST: {
                if (grantResults.length > 0) {
                    boolean camera_accepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (camera_accepted && writeStorageAccepted) {
                        pickFromCamera();
                    } else {
                        Toast.makeText(this, "Please Enable Camera and Storage Permissions", Toast.LENGTH_LONG).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST: {
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        pickFromGallery();
                    } else {
                        Toast.makeText(this, "Please Enable Storage Permissions", Toast.LENGTH_LONG).show();
                    }
                }
            }
            break;
        }
    }

    // Here we will click a photo and then go to startActivityForResult for updating data
    private void pickFromCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "SociSnapProfile");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "SociSnap profile");
        imageUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_REQUEST);
    }

    // We will select an image from gallery
    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_REQUEST);
    }

    // We will upload the image from here.
    private void uploadProfileCoverPhoto(final Uri uri) {
        AlertDialog alertDialog = Progress.createAlertDialog(this, "Working...");
        alertDialog.show();

        // We are taking the filepath as storage path + firebase auth.getUid()+".png"
        String filePathName = storagePath + "" + profileOrCoverPhoto + "_" + firebaseUser.getUid();
        StorageReference storageReference1 = storageReference.child(filePathName);
        storageReference1.putFile(uri).addOnSuccessListener(taskSnapshot -> {
            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful()) ;

            // We will get the url of our image using uri task
            final Uri downloadUri = uriTask.getResult();
            if (uriTask.isSuccessful()) {
                // updating our image url into the realtime database
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put(profileOrCoverPhoto, downloadUri.toString());
                databaseReference.child(firebaseUser.getUid()).updateChildren(hashMap).addOnSuccessListener(aVoid -> {
                    alertDialog.dismiss();
                    Toast.makeText(EditProfilePageActivity.this, "Updated", Toast.LENGTH_LONG).show();
                }).addOnFailureListener(e -> {
                    alertDialog.dismiss();
                    Toast.makeText(EditProfilePageActivity.this, "Error Updating ", Toast.LENGTH_LONG).show();
                });
            } else {
                alertDialog.dismiss();
                Toast.makeText(EditProfilePageActivity.this, "Error", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            alertDialog.dismiss();
            Toast.makeText(EditProfilePageActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(EditProfilePageActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
