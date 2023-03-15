package com.softtronic.socisnap;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    //Progress alertDialog = new Progress(this);
    EditText email, password;
    CheckBox rememberUser;
    TextView forgotPass, noAcc;
    Button signIn;
    FirebaseUser currentUser;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password_toggle);
        rememberUser = findViewById(R.id.remember_me);
        forgotPass = findViewById(R.id.forgot_password);
        signIn = findViewById(R.id.btnLogin);
        noAcc = findViewById(R.id.create_acc);

        mAuth = FirebaseAuth.getInstance();

        // checking if user is null or not
        currentUser = mAuth.getCurrentUser();

        signIn.setOnClickListener(v->{
            String userEmail = email.getText().toString();
            String userPassword = password.getText().toString();

            if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                email.setError("Invalid Email");
                email.setFocusable(true);

            } else {
                loginUser(userEmail, userPassword);

                //Create or recreate the database the database
                CreateDB createDB = new CreateDB(LoginActivity.this);
                SQLiteDatabase db = createDB.getWritableDatabase();
                db.close();
            }
        });

        noAcc.setOnClickListener(v ->{
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        forgotPass.setOnClickListener(v -> showRecoverPasswordDialog());

    }

    private void loginUser(String EMAIL, String PASSWORD) {
        AlertDialog alertDialog = Progress.createAlertDialog(this, "Please Wait...");
        alertDialog.show();
        // sign in with email and password after authenticating
        mAuth.signInWithEmailAndPassword(EMAIL, PASSWORD).addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                alertDialog.dismiss();
                if (Objects.requireNonNull(task.getResult().getAdditionalUserInfo()).isNewUser()) {
                    String email = Objects.requireNonNull(user).getEmail();
                    String uid = user.getUid();
                    HashMap<Object, String> hashMap = new HashMap<>();
                    hashMap.put("email", email);
                    hashMap.put("uid", uid);
                    hashMap.put("name", "");
                    hashMap.put("onlineStatus", "online");
                    hashMap.put("typingTo", "noOne");
                    hashMap.put("phone", "");
                    hashMap.put("image", "");
                    hashMap.put("cover", "");
                    FirebaseDatabase database = FirebaseDatabase.getInstance();

                    // store the value in Database in "Users" Node
                    DatabaseReference reference = database.getReference("Users");

                    // storing the value in Firebase
                    reference.child(uid).setValue(hashMap);
                }
                Toast.makeText(LoginActivity.this, "welcome " + Objects.requireNonNull(user).getEmail(), Toast.LENGTH_LONG).show();
                Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Login Failed", Toast.LENGTH_LONG).show();
                alertDialog.dismiss();
            }
        }).addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Error Occurred", Toast.LENGTH_LONG).show());
    }


    private void showRecoverPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recover Password");
        LinearLayout linearLayout = new LinearLayout(this);
        final EditText txtEmail = new EditText(this);//write your registered email
        txtEmail.setHint("Email Address");
        txtEmail.setMinEms(16);
        txtEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        linearLayout.addView(txtEmail);
        linearLayout.setPadding(10, 10, 10, 10);
        builder.setView(linearLayout);
        builder.setPositiveButton("Recover", (dialog, which) -> {
            String email = txtEmail.getText().toString().trim();
            beginRecovery(email);//send a mail on the mail to recover password
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void beginRecovery(String userEmail) {

        // send reset password email
        mAuth.sendPasswordResetEmail(userEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(LoginActivity.this, "Check your Email inbox", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(LoginActivity.this, "Error Occurred. Try again later", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Your Request could not be completed at the moment.", Toast.LENGTH_LONG).show());
    }
}