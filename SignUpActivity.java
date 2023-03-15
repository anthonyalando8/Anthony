package com.softtronic.socisnap;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;


public class SignUpActivity extends AppCompatActivity {
    EditText email, password, repeatPassword, name;
    CheckBox agreeTerms;
    TextView linkSignIn;
    Button signUp;
    MaterialButton signIn;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_sign_up);

        name=findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password_toggle);
        repeatPassword = findViewById(R.id.password_repeat);
        agreeTerms = findViewById(R.id.t_q);
        signUp = findViewById(R.id.btnSignUp);
        linkSignIn = findViewById(R.id.linkLogin);
        signIn = findViewById(R.id.btnSign_in);

        mAuth = FirebaseAuth.getInstance();

        signUp.setOnClickListener(v ->{
            String userName=name.getText().toString().trim();
            String userEmail = email.getText().toString().trim();
            String userPassword;
            String passTry = password.getText().toString();
            String passCheck = repeatPassword.getText().toString();
            if(!passCheck.equals(passTry)){
                Toast.makeText(this, "Password not match", Toast.LENGTH_SHORT).show();
                password.setText("");
                repeatPassword.setText("");
                return;
            }else{
                userPassword = passCheck.trim();
            }

            if(!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()){
                email.setError("Invalid Email");
                email.setFocusable(true);
            }else if(!(userPassword.length() >= 5)){
                Toast.makeText(this, "Password less than 6 characters ", Toast.LENGTH_SHORT).show();
            }else{
                registerUser(userEmail,userName, userPassword);
            }
        });


        linkSignIn.setOnClickListener(v-> showLoginDialog());
        signIn.setOnClickListener(v-> showLoginDialog());
    }
    public void registerUser(String EMAIL, final String USERNAME,final String PASSWORD) {
        AlertDialog alertDialog = Progress.createAlertDialog(this, "Please Wait...");
        alertDialog.show();
        mAuth.createUserWithEmailAndPassword(EMAIL, PASSWORD).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                alertDialog.dismiss();

                //create the vulgar database after sign up is successful
                CreateDB createDB = new CreateDB(SignUpActivity.this);
                SQLiteDatabase db = createDB.getWritableDatabase();
                db.close();

                FirebaseUser user = mAuth.getCurrentUser();
                assert user != null;
                String email = user.getEmail();
                String uid = user.getUid();

                HashMap<Object, String> hashMap = new HashMap<>();
                hashMap.put("email", email);
                hashMap.put("uid", uid);
                hashMap.put("name", USERNAME);
                hashMap.put("onlineStatus", "online");
                hashMap.put("typingTo", "noOne");
                hashMap.put("image", "");
                hashMap.put("bio", "");
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference reference = database.getReference("Users");
                reference.child(uid).setValue(hashMap);
                Toast.makeText(SignUpActivity.this, "Registered " + user.getEmail(), Toast.LENGTH_LONG).show();
                Intent mainIntent = new Intent(SignUpActivity.this, EditProfilePageActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
                finish();
            } else {
                Toast.makeText(SignUpActivity.this, "Error", Toast.LENGTH_LONG).show();
                alertDialog.dismiss();
            }
        }).addOnFailureListener(e -> Toast.makeText(SignUpActivity.this, "Error Occurred", Toast.LENGTH_LONG).show());
    }
    public  void showLoginDialog(){
        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}