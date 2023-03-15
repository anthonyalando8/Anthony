package com.softtronic.socisnap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class ChatActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    ImageView profile, block;
    TextView name, userStatus;
    EditText msg;
    ImageButton send, attach;
    FirebaseAuth firebaseAuth;
    String uid, myUid, image;
    List<ModelChat> chatList;
    AdapterChat adapterChat;

    private static final int IMAGE_PICK_GALLERY_REQUEST = 300;
    private static final int IMAGE_PICK_CAMERA_REQUEST = 400;
    private static final int CAMERA_REQUEST = 100;
    private static final int STORAGE_REQUEST = 200;
    String[] cameraPermission;
    String[] storagePermission;
    Uri imageUri = null;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference users;
    FirebaseUser currentUser;
    boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_chat);
        firebaseAuth = FirebaseAuth.getInstance();

        // initialise the text views and layouts
        profile = findViewById(R.id.profiletv);
        name = findViewById(R.id.nameptv);
        userStatus = findViewById(R.id.onlinetv);
        msg = findViewById(R.id.messaget);
        send = findViewById(R.id.sendmsg);
        attach = findViewById(R.id.attachbtn);
        block = findViewById(R.id.block);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView = findViewById(R.id.chatrecycle);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        uid = getIntent().getStringExtra("uid");



        // getting uid of another user using intent
        firebaseDatabase = FirebaseDatabase.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        myUid = Objects.requireNonNull(currentUser).getUid();
        // initialising permissions
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        checkUserStatus();
        checkOnlineStatus("online");

        msg.setOnFocusChangeListener((view, b) -> {
            if(b){
                checkTypingStatus(uid);
            }
            else {
                checkTypingStatus("noOne");
            }
        });
        RelativeLayout parent = findViewById(R.id.rootLayout);
        parent.setOnTouchListener((view, motionEvent) -> {
            msg.clearFocus();
            return false;
        });
        users = firebaseDatabase.getReference("Users");
        attach.setOnClickListener(v ->{
            showImagePicDialog();
            msg.clearFocus();
        });
        send.setOnClickListener(v -> {
            msg.clearFocus();
            notify = true;
            String message = msg.getText().toString().trim();
            if (TextUtils.isEmpty(message)) {//if empty
                Toast.makeText(ChatActivity.this, "Message body can't be empty!", Toast.LENGTH_LONG).show();
            } else {
                Context context = getApplicationContext();
                boolean hasVulgarWords = WordChecker.hasVulgarWords(context, message);
                if (hasVulgarWords) {
                    AlertDialog customAlert = AlertMsg.createAlertDialog(this, "Vulgar detected!");
                    customAlert.show();
                    
                } else {

                    sendMessage(message);
                }

            }
            msg.setText("");
        });

        Query userQuery = users.orderByChild("uid").equalTo(uid);
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // retrieve user data
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    String nameString = dataSnapshot1.child("name").getValue(String.class);
                    image = "" + dataSnapshot1.child("image").getValue(String.class);
                    String onlineStatus = dataSnapshot1.child("onlineStatus").getValue(String.class);
                    String typingTo = dataSnapshot1.child("typingTo").getValue(String.class);
                    if (typingTo.equals(myUid)) {// if user is typing to my chat
                        userStatus.setText("typing...");// type status as typing
                    } else {
                        if (Objects.requireNonNull(onlineStatus).equals("online")) {
                            userStatus.setText(onlineStatus);
                        } else {
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeInMillis(Long.parseLong(onlineStatus));
                            String timeDate = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();
                            userStatus.setText("Last Seen:".concat(timeDate));
                        }
                    }
                    name.setText(nameString);
                    try {
                        Glide.with(ChatActivity.this).load(image).placeholder(R.drawable.baseline_person).into(profile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        readMessages();

    }

    @Override
    protected void onPause() {
        super.onPause();
        //checkOnlineStatus("online");
        checkTypingStatus("noOne");
    }

    @Override
    protected void onResume() {
        checkOnlineStatus("online");
        super.onResume();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void checkOnlineStatus(String status) {
        // check online status
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);
        dbRef.updateChildren(hashMap);
    }

    private void checkTypingStatus(String typing) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("typingTo", typing);
        dbRef.updateChildren(hashMap);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        checkOnlineStatus("online");
        super.onStart();
    }

    private void readMessages() {
        // show message after retrieving data
        chatList = new ArrayList<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                chatList.clear();
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    ModelChat modelChat = dataSnapshot1.getValue(ModelChat.class);
                    if (Objects.requireNonNull(modelChat).getSender().equals(myUid) &&
                            modelChat.getReceiver().equals(uid) ||
                            modelChat.getReceiver().equals(myUid)
                                    && modelChat.getSender().equals(uid)) {
                        chatList.add(modelChat); // add the chat in chat list
                    }
                    adapterChat = new AdapterChat(ChatActivity.this, chatList, image);
                    adapterChat.notifyDataSetChanged();
                    recyclerView.setAdapter(adapterChat);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showImagePicDialog() {
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
        builder.setTitle("Pick Image From");
        builder.setItems(options, (dialog, which) -> {

            if (which == 0) {
                if (!checkCameraPermission()) { // if permission is not given
                    requestCameraPermission(); // request for permission
                } else {
                    pickFromCamera(); // if already access granted then click
                }
            } else if (which == 1) {
                if (!checkStoragePermission()) { // if permission is not given
                    requestStoragePermission(); // request for permission
                } else {
                    pickFromGallery(); // if already access granted then pick
                }
            }
        });
        builder.create().show();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // request for permission if not given
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST: {
                if (grantResults.length > 0) {
                    boolean camera_accepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (camera_accepted && writeStorageAccepted) {
                        pickFromCamera(); // if access granted then click
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
                        pickFromGallery(); // if access granted then pick
                    } else {
                        Toast.makeText(this, "Please Enable Storage Permissions", Toast.LENGTH_LONG).show();
                    }
                }
            }
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_REQUEST) {
                imageUri = Objects.requireNonNull(data).getData(); // get image data to upload
                try {
                    sendImageMessage(imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (requestCode == IMAGE_PICK_CAMERA_REQUEST) {
                imageUri = Objects.requireNonNull(data).getData(); // get image data to upload
                try {
                    sendImageMessage(imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendImageMessage(Uri uri) throws IOException {
        notify = true;
        AlertDialog alertDialog = Progress.createAlertDialog(this, "Sending...");
        alertDialog.show();

        // If we are sending image as a message
        // then we need to find the url of
        // image after uploading the
        // image in firebase storage
        final String timestamp = "" + System.currentTimeMillis();
        String filePathName = "ChatImages/" + "post" + timestamp; // filename
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, arrayOutputStream); // compressing the image using bitmap
        final byte[] data = arrayOutputStream.toByteArray();
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathName);
        ref.putBytes(data).addOnSuccessListener(taskSnapshot -> {

            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful()) ;
            String downloadUri = uriTask.getResult().toString(); // getting url if task is successful

            if (uriTask.isSuccessful()) {
                alertDialog.dismiss();
                DatabaseReference re = FirebaseDatabase.getInstance().getReference();
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("sender", myUid);
                hashMap.put("receiver", uid);
                hashMap.put("message", downloadUri);
                hashMap.put("timestamp", timestamp);
                hashMap.put("dilihat", false);
                hashMap.put("type", "images");
                re.child("Chats").push().setValue(hashMap); // push in firebase using unique id
                final DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference("ChatList").child(uid).child(myUid);
                ref1.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            ref1.child("id").setValue(myUid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                final DatabaseReference ref2 = FirebaseDatabase.getInstance().getReference("ChatList").child(myUid).child(uid);
                ref2.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            ref2.child("id").setValue(uid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }).addOnFailureListener(e -> {

        });
    }

    private Boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission() {
        requestPermissions(cameraPermission, CAMERA_REQUEST);
    }

    private void pickFromCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_pic");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");
        imageUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_REQUEST);
    }

    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_REQUEST);
    }

    private Boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission() {
        requestPermissions(storagePermission, STORAGE_REQUEST);
    }

    private void sendMessage(final String message) {
        // creating a reference to store data in firebase
        // We will be storing data using current time in "Chat list"
        // and we are pushing data using unique id in "Chats"
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        String timestamp = String.valueOf(System.currentTimeMillis());
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", uid);
        hashMap.put("message", message);
        hashMap.put("timestamp", timestamp);
        hashMap.put("dilihat", false);
        hashMap.put("type", "text");
        databaseReference.child("Chats").push().setValue(hashMap);
        final DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference("ChatList").child(uid).child(myUid);
        ref1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    ref1.child("id").setValue(myUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        final DatabaseReference ref2 = FirebaseDatabase.getInstance().getReference("ChatList").child(myUid).child(uid);
        ref2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (!dataSnapshot.exists()) {
                    ref2.child("id").setValue(uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            myUid = user.getUid();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ChatActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        super.onBackPressed();
    }
}
