package com.softtronic.socisnap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {
    private CircleImageView profileImage;
    private TextView nameView, emailView, bioView;


    public ProfileFragment(){
        //constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        FloatingActionButton edit = view.findViewById(R.id.editProfile);

        Activity context;
        context = getActivity();

        edit.setOnClickListener(v ->{
            Intent intent = new Intent(context, EditProfilePageActivity.class);
            startActivity(intent);
        });


        profileImage = view.findViewById(R.id.avatarTv);
        nameView = view.findViewById(R.id.nameTv);
        emailView = view.findViewById(R.id.emailTv);
        bioView = view.findViewById(R.id.bio);
        fetchUserProfile();




        return view;
    }
    private  void fetchUserProfile(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));
        reference.get().addOnSuccessListener(dataSnapshot -> {
            UserModel user = dataSnapshot.getValue(UserModel.class);
            Objects.requireNonNull(user).setImageUri(dataSnapshot.child("image").getValue(String.class));
            setUserData(user);
        }).addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void setUserData(UserModel model){
        nameView.setText(model.getName());
        emailView.setText(model.getEmail());
        bioView.setText(model.getBio());
        try {
            Glide.with(requireContext()).load(model.getImageUri()).into(profileImage);
        }catch (Exception e){
            Picasso.get().load(R.drawable.baseline_person).into(profileImage);
        }
    }
}