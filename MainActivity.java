package com.softtronic.socisnap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private long pressedTime;
    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    NavigationView navigationView;
    FirebaseAuth firebaseAuth;
    FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        navigationView = findViewById(R.id.drawer_navigation);

        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        checkOnlineStatus("online");
        drawerLayout = findViewById(R.id.my_drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);

        // pass the Open and Close toggle for the drawer layout listener
        // to toggle the button
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // to make the Navigation drawer icon always appear on the action bar
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        ChatListFragment fragment = new ChatListFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content, fragment, "");
        fragmentTransaction.commit();

        navigationView.setNavigationItemSelectedListener(item -> {
            if(item.getItemId() == R.id.nav_account){
                openFragment(new ProfileFragment(), "Profile");
            } else if (item.getItemId() == R.id.nav_chats) {
                openFragment(new ChatListFragment(), "SociSnap");
            } else if (item.getItemId() == R.id.nav_user) {
                openFragment(new UsersFragment(), "Users");
            } else if (item.getItemId() == R.id.nav_logout) {

                if(currentUser != null){
                    firebaseAuth.signOut();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);//makes sure user cant go back
                    startActivity(intent);
                }
            }else if (item.getItemId() == R.id.nav_refresh) {
                this.finish();
                this.startActivity(this.getIntent());
            }else if (item.getItemId() == R.id.nav_group ) {
                //Nothing
            } else if (item.getItemId() == R.id.nav_post ) {
                openFragment(new PostFragment(), "Post");
            } else if (item.getItemId() == R.id.nav_settings ) {
                //Nothing
            } else if (item.getItemId() == R.id.nav_help ) {
                String url = "https://anthonyalando8.github.io/softtronic-east-africa.github.io";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }else{
                //Nothing
            }
            drawerLayout.closeDrawer(GravityCompat.START, false);
            return false;

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar_nav, menu);

        // Get the action bar and set the app title
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setTitle(R.string.app_name);
        MenuItem btnUsers = menu.findItem(R.id.action_users);
        MenuItem btnPost = menu.findItem(R.id.action_posts);
        MenuItem btnAcc = menu.findItem(R.id.action_account);

        btnUsers.setOnMenuItemClickListener(menuItem -> {
            openFragment(new UsersFragment(), "Users");
            return true;
        });
        btnPost.setOnMenuItemClickListener(menuItem -> {
            openFragment(new PostFragment(), "Post");
            return true;
        });
        btnAcc.setOnMenuItemClickListener(menuItem -> {
            openFragment(new ProfileFragment(), "Profile");
            return true;
        });
        return super.onCreateOptionsMenu(menu);
    }

    // override the onOptionsItemSelected()
    // function to implement
    // the item click listener callback
    // to open and close the navigation
    // drawer when the icon is clicked
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if(currentUser != null){
            checkOnlineStatus(String.valueOf(System.currentTimeMillis()));
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        if(currentUser != null){
            checkOnlineStatus("online");
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if(currentUser != null){
            checkOnlineStatus(String.valueOf(System.currentTimeMillis()));
        }
        super.onPause();
    }
    private void checkOnlineStatus(String status) {
        // check online status
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);
        dbRef.updateChildren(hashMap);
    }
    public void openFragment(Fragment fragment, String title) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.content, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setTitle(title);
    }
}