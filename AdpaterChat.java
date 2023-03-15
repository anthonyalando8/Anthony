package com.softtronic.socisnap;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.format.DateFormat;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

class AdapterChat extends RecyclerView.Adapter<AdapterChat.MyHolder> {
    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;
    Context context;
    List<ModelChat> list;
    String imageUrl;
    FirebaseUser firebaseUser;

    public AdapterChat(Context context, List<ModelChat> list, String imageUrl) {
        this.context = context;
        this.list = list;
        this.imageUrl = imageUrl;
    }
    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == MSG_TYPE_LEFT) {
            view = LayoutInflater.from(context).inflate(R.layout.received_msg_layout, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.send_msg_layout, parent, false);
        }
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position) {
        String message = list.get(position).getMessage();
        String timeStamp = list.get(position).getTimestamp();
        String type = list.get(position).getType();
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(Long.parseLong(timeStamp));
        String timeDate = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();
        holder.message.setText(message);
        holder.time.setText(timeDate);
        try {
            Glide.with(context).load(imageUrl).into(holder.image);
        } catch (Exception e) {
            Glide.with(context).load(R.drawable.baseline_person).into(holder.image);
        }
        if (type.equals("text")) {
            holder.message.setVisibility(View.VISIBLE);
            holder.msgImage.setVisibility(View.GONE);
            holder.message.setText(message);
        } else {
            holder.message.setVisibility(View.GONE);
            holder.msgImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(message).into(holder.msgImage);
        }

        holder.msgLayPut.setOnLongClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Message Selected");
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            final TextView copyMsg = new TextView(context);
            copyMsg.setText("Copy");
            final TextView delete = new TextView(context);
            delete.setText("Delete");
            final TextView info = new TextView(context);
            info.setText("Info");
            copyMsg.setPadding(25,5,5,5);
            delete.setPadding(25,5,5,5);
            info.setPadding(25,5,5,5);
            linearLayout.addView(copyMsg);
            linearLayout.addView(delete);
            linearLayout.addView(info);
            linearLayout.setPadding(5, 5, 5, 5);
            copyMsg.setOnClickListener(v->{
                ClipboardManager clipboardManager = getSystemService(context, ClipboardManager.class);
                ClipData clipData = ClipData.newPlainText("text", message);
                Objects.requireNonNull(clipboardManager).setPrimaryClip(clipData);
                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show();
            });
            delete.setOnClickListener(v->{
                AlertDialog.Builder deleteBuilder = new AlertDialog.Builder(context);
                deleteBuilder.setTitle("Delete Message");
                deleteBuilder.setMessage("Are You Sure To Delete This Message");
                deleteBuilder.setPositiveButton("Delete", (dialog, which) -> deleteMsg(holder.getAdapterPosition()));
                deleteBuilder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                deleteBuilder.create().show();
            });
            builder.setView(linearLayout);
            builder.create().show();
            return true;
        });
    }
    private void deleteMsg(int position) {
        final String myUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        String msgTimeStamp = list.get(position).getTimestamp();
        DatabaseReference chats = FirebaseDatabase.getInstance().getReference().child("Chats");
        Query query = chats.orderByChild("timestamp").equalTo(msgTimeStamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    if (Objects.requireNonNull(dataSnapshot1.child("sender").getValue()).equals(myUid)) {
                        // any two of below can be used
                        dataSnapshot1.getRef().removeValue();
					/* HashMap<String, Object> hashMap = new HashMap<>();
						hashMap.put("message", "This Message Was Deleted");
						dataSnapshot1.getRef().updateChildren(hashMap);
						Toast.makeText(context,"Message Deleted.....",Toast.LENGTH_LONG).show();
*/
                    } else {
                        Toast.makeText(context, "you can delete only your msg....", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public int getItemViewType(int position) {
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (list.get(position).getSender().equals(firebaseUser.getUid())) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }


    class MyHolder extends RecyclerView.ViewHolder {

        CircleImageView image;
        ImageView msgImage;
        TextView message, time, isSee;
        CardView msgLayPut;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.profilec);
            message = itemView.findViewById(R.id.msgc);
            time = itemView.findViewById(R.id.timetv);
            isSee = itemView.findViewById(R.id.isSeen);
            msgLayPut = itemView.findViewById(R.id.msglayout);
            msgImage = itemView.findViewById(R.id.images);
        }
    }
}
