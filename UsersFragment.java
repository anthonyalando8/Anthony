package com.softtronic.socisnap;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.softtronic.socisnap.adapter.UserAdapter;

import java.util.ArrayList;


public class UsersFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ArrayList<UserModel> userArrayList;
    private UserAdapter adapter;
    public UsersFragment(){
        //Const
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users, container, false);
        recyclerView = view.findViewById(R.id.recycleUsers);
        progressBar = view.findViewById(R.id.pbLoading);

        userArrayList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserAdapter(userArrayList, getActivity().getApplicationContext());
        recyclerView.setAdapter(adapter);

        fetchAllUsers();
        return view;
    }
    public void fetchAllUsers(){
        AlertDialog alertDialog = Progress.createAlertDialog(getContext(), "Please Wait...");
        alertDialog.show();
        Query query = FirebaseDatabase.getInstance().getReference("Users");
        query.get().addOnSuccessListener(dataSnapshot -> {
            if(dataSnapshot.exists()){
                alertDialog.dismiss();
                recyclerView.setVisibility(View.VISIBLE);
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    if(!snapshot.getKey().equals(FirebaseAuth.getInstance().getUid())){
                        UserModel user = snapshot.getValue(UserModel.class);
                        user.setImageUri(snapshot.child("image").getValue(String.class));
                        user.setId(snapshot.getKey());
                        userArrayList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
            }else{
                Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });

    }
}
