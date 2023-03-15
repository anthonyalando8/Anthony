package com.softtronic.socisnap;


import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.MyHolder> {
    Context context;
    ArrayList<PostModel> postModel;
    private final HashMap<String, String> userName;
    private final HashMap<String, String> userImageLink;

    public PostAdapter(Context context, ArrayList<PostModel> postModel) {
        this.context = context;
        this.postModel = postModel;
        userName = new HashMap<>();
        userImageLink = new HashMap<>();
    }
    public void clear() {
        postModel.clear();
        notifyDataSetChanged();
    }

    public void addAll(ArrayList<PostModel> newData) {
        postModel.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.post_view, parent,false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        PostModel model = postModel.get(position);
        holder.txtPost.setText(model.getPostDescription());
        String time = model.getPostTime();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(time));
        String timeDate = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();
        holder.txtTime.setText(timeDate);
        holder.txtName.setText(userName.get(model.getUserId()));
        try{
            Picasso.get().load(model.getPostImageUri()).into(holder.postImage);
        }catch (Exception e){
            e.printStackTrace();
        }
        try{
            Glide.with(context).load(userImageLink.get(model.getUserId())).into(holder.userProfile);
        }catch (Exception e){
            e.printStackTrace();
        }
        int off = R.drawable.baseline_thumb_up_off;
        int on = R.drawable.baseline_thumb_up_on;
        final boolean[] isOff = {true};
        holder.btnLike.setIconResource(off);
        holder.btnLike.setOnClickListener(view -> {
            if (isOff[0]) {
                holder.btnLike.setIconResource(on);
                isOff[0] = false;
            } else {
                holder.btnLike.setIconResource(off);
                isOff[0] = true;
            }
        });
    }
    public void setUserNameMap(String userId, String name) {
        userName.put(userId, name);
    }
    public void setImageLinkMap(String userId, String link){
        userImageLink.put(userId, link);
    }
    @Override
    public int getItemCount() {
        return postModel.size();
    }

    public static class MyHolder extends RecyclerView.ViewHolder{
        ImageView more, postImage;
        CircleImageView userProfile;
        TextView txtPost, txtName, txtTime;
        MaterialButton btnLike, btnComment;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            more = itemView.findViewById(R.id.more);
            postImage = itemView.findViewById(R.id.postImage);
            userProfile = itemView.findViewById(R.id.profileCV);
            txtPost = itemView.findViewById(R.id.postDesc);
            txtName = itemView.findViewById(R.id.txtName);
            txtTime = itemView.findViewById(R.id.timeTV);
            btnLike = itemView.findViewById(R.id.like);
            btnComment = itemView.findViewById(R.id.comment);
        }
    }
}
