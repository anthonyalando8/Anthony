package com.softtronic.socisnap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public class AdapterChatList extends RecyclerView.Adapter<AdapterChatList.MyHolder> {

    Context context;
    FirebaseAuth firebaseAuth;
    String userId, imageUri;
    Set<UserModel> usersList;
    private final HashMap<String, String> lastMessageMap;
    public AdapterChatList(Context context, Set<UserModel> usersList) {
        this.context = context;
        this.usersList = usersList;
        lastMessageMap = new HashMap<>();
        firebaseAuth = FirebaseAuth.getInstance();
        userId = firebaseAuth.getUid();
    }

    public void clear() {
        usersList.clear();
        notifyDataSetChanged();
    }

    public void addAll(Set<UserModel> newData) {
        usersList.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_list_view, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position) {
        UserModel[]arr = usersList.toArray(new UserModel[0]);

        final String UserId = arr[position].getId();
        String imageUri = arr[position].getImageUri();
        String username = arr[position].getName();
        String lastMsg = lastMessageMap.get(UserId);
        holder.name.setText(username);
        if(!arr[position].getOnlineStatus().equals("online")){
            holder.status.setVisibility(View.GONE);
        }
        //holder.block.setImageResource(R.drawable.ic_unblock);

        // if no last message then Hide the layout
        if (lastMsg == null || lastMsg.equals("default")) {
            holder.lastMessageTxt.setVisibility(View.GONE);
        }
        else {
            holder.lastMessageTxt.setVisibility(View.VISIBLE);
            holder.lastMessageTxt.setText(lastMsg);
        }
        holder.lastMessageTxt.setText(lastMsg);
        try {
            // loading profile pic of user
            Glide.with(context).load(imageUri).into(holder.profile);
        } catch (Exception e) {
            Glide.with(context).load(R.drawable.no_user).into(holder.profile);
        }
        holder.profile.setOnClickListener(view -> {
            AlertDialog alertDialog = ViewProfilePic.createAlertDialog(view.getContext(), username, imageUri);
            alertDialog.show();
        });

        // redirecting to chat activity on item click
        holder.nameMsgLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);

            // putting uid of user in extras
            intent.putExtra("uid", UserId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        });
        holder.nameMsgLayout.setOnLongClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle("Select");
            LinearLayout layout = new LinearLayout(builder.getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            final TextView delete = new TextView(view.getContext());
            delete.setText("Delete");
            delete.setPadding(5,5,5,5);
            delete.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
            delete.setOnClickListener(view1 -> {
                DatabaseReference chats = FirebaseDatabase.getInstance().getReference().child("ChatList")
                        .child(userId);
                chats.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot dataSnapshot : snapshot.getChildren()){
                            if(Objects.requireNonNull(dataSnapshot.getKey()).equals(UserId)){
                                dataSnapshot.getRef().removeValue().addOnSuccessListener(unused -> Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            });
            builder.setNegativeButton("Cancel",(dialog,which)-> dialog.dismiss());
            layout.addView(delete);
            builder.setView(layout);
            builder.create().show();
            return false;
        });

    }

    // setting last message sent by users.
    public void setLastMessageMap(String userId, String lastMessage) {
        lastMessageMap.put(userId, lastMessage);
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        ImageView profile, status;
        TextView name, lastMessageTxt;
        LinearLayout nameMsgLayout;
        CardView cardView;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            profile = itemView.findViewById(R.id.profileimage);
            status = itemView.findViewById(R.id.onlineStatus);
            name = itemView.findViewById(R.id.nameonline);
            lastMessageTxt = itemView.findViewById(R.id.lastmessge);
            nameMsgLayout = itemView.findViewById(R.id.nameMsg);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}
