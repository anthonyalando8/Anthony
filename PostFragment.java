package com.softtronic.socisnap;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.HashMap;


public class PostFragment extends Fragment {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private ArrayList<PostModel> arrayList;
    private SwipeRefreshLayout swipeRefreshLayout;
    FirebaseUser firebaseUser;
    FirebaseAuth firebaseAuth;
    StorageReference storageReference;
    DatabaseReference reference;
    String storagePath = "PostImages/";
    String photo = "image";
    private Uri imageUri;
    private PostAdapter adapter;
    private PermissionListener cameraPermissionListener;
    private PermissionListener galleryPermissionListener;
    FloatingActionButton addPost;
    RecyclerView recyclerView;
    ProgressBar progressBar;

    public PostFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        cameraPermissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse response) {
                // Permission is granted for camera
                pickFromCamera();
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse response) {
                // Permission is denied
                Toast.makeText(getActivity(), "Camera permission is required", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                // Show permission rationale
                token.continuePermissionRequest();
            }
        };
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        galleryPermissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse response) {
                // Permission is granted for gallery
                pickFromGallery();
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse response) {
                // Permission is denied
                Toast.makeText(getActivity(), "Gallery permission is required", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                // Show permission rationale
                token.continuePermissionRequest();
            }
        };

        View view = inflater.inflate(R.layout.fragment_post, container, false);
        arrayList = new ArrayList<>();
        recyclerView = view.findViewById(R.id.recycleViewPost);
        progressBar = view.findViewById(R.id.pbLoadingPost);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchAllPosts();
                swipeRefreshLayout.setRefreshing(false);
            }
        });


        fetchAllPosts();

        addPost = view.findViewById(R.id.addPost);
        addPost.setOnClickListener(v->{
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Create New Post");
            builder.setCancelable(true);

            LayoutInflater inflateAlert = getLayoutInflater();
            View dialogView = inflateAlert.inflate(R.layout.add_new_post, null);
            final FloatingActionButton imagePicker = dialogView.findViewById(R.id.addPic);
            final ImageView imageView = dialogView.findViewById(R.id.ivImage);
            final EditText editText = dialogView.findViewById(R.id.etPost);
            final MaterialButton materialButton = dialogView.findViewById(R.id.uploadPost);
            builder.setView(dialogView);
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            imagePicker.setOnClickListener(view1-> showImagePicDialog());
            AlertDialog dialog = builder.create();
            materialButton.setOnClickListener(view2->{
                String message = editText.getText().toString();

                //Add post Picture
                if(imageUri != null && !message.isEmpty()){
                    AlertDialog alertDialog = Progress.createAlertDialog(requireContext(), "Posting...");
                    alertDialog.show();
                    storageReference = FirebaseStorage.getInstance().getReference("UserPost");
                    String timePost = String.valueOf(System.currentTimeMillis());
                    String filePathName = storagePath + "" + photo + "_" + timePost;
                    StorageReference storageReference1 = storageReference.child(filePathName);
                    storageReference1.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful()) ;

                        // We will get the url of our image using uri task
                        final Uri downloadUri = uriTask.getResult();
                        if (uriTask.isSuccessful()) {
                            // updating our image url into the realtime database
                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Posts");
                            String timestamp = String.valueOf(System.currentTimeMillis());
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("postId", databaseReference.push().getKey());
                            hashMap.put("postImageUri", downloadUri.toString());
                            hashMap.put("postDescription", message);
                            hashMap.put("postTime", timestamp);
                            hashMap.put("userId", firebaseUser.getUid());
                            databaseReference.push().setValue(hashMap).addOnSuccessListener(unused -> {
                                alertDialog.dismiss();
                                dialog.dismiss();
                                Toast.makeText(getContext(), "Successful", Toast.LENGTH_SHORT).show();
                            }).addOnFailureListener(e -> {
                                alertDialog.dismiss();
                                dialog.dismiss();
                                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            Toast.makeText(getContext(), "Error", Toast.LENGTH_LONG).show();
                        }
                    }).addOnFailureListener(e -> {
                        alertDialog.dismiss();
                        dialog.dismiss();
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }else
                    Toast.makeText(getContext(), "Try again later", Toast.LENGTH_SHORT).show();

            });
            dialog.show();
        });

        return view;
    }
    private void showImagePicDialog() {
        String [] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Image from");
        builder.setItems(options, (dialog, which) -> {
            // if access is not given then we will request for permission
            if (which == 0) {
                Dexter.withContext(getContext())
                        .withPermission(Manifest.permission.CAMERA)
                        .withListener(cameraPermissionListener)
                        .check();
            } else if (which == 1) {
                Dexter.withContext(getContext())
                        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(galleryPermissionListener)
                        .check();
            }
        });
        builder.create().show();
    }
    private void pickFromCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "post");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "SociSnap Post");
        imageUri = getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
    }

    // We will select an image from gallery
    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, REQUEST_IMAGE_PICK);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if(data != null){
                if (requestCode == REQUEST_IMAGE_PICK) {
                    imageUri = data.getData();
                }
                if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    imageUri = data.getData();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    public void fetchAllPosts(){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter = new PostAdapter(getContext(), arrayList);
                    adapter.clear();
                    adapter.addAll(arrayList);
                    for (DataSnapshot dataSnapshot1 : snapshot.getChildren()) {
                        PostModel postModel = dataSnapshot1.getValue(PostModel.class);
                        String uid = postModel.getUserId();
                        arrayList.add(postModel);
                        fetchUserDetails(uid);
                    }
                    adapter = new PostAdapter(getContext(), arrayList);
                    recyclerView.setAdapter(adapter);
                }else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "No posts", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    public void fetchUserDetails(String uid){
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot : snapshot.getChildren()){
                    String name, image;
                    if(dataSnapshot.getKey().equals(uid)){
                        UserModel model = dataSnapshot.getValue(UserModel.class);
                        model.setName(dataSnapshot.child("name").getValue(String.class));
                        model.setImageUri(dataSnapshot.child("image").getValue(String.class));
                        name = model.getName();
                        image = model.getImageUri();
                        adapter.setUserNameMap(uid, name);
                        adapter.setImageLinkMap(uid, image);
                        adapter.notifyDataSetChanged();
                    }
                }


            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                error.getMessage();
            }
        });
    }
}