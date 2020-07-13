package com.idea.mydiary;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.navigation.NavigationView;
import com.idea.mydiary.models.DataManager;
import com.idea.mydiary.models.Note;
import com.idea.mydiary.adapters.NotesAdapter;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int MENU_ORDER_0 = 0;
    private static final int MENU_ORDER_1 = 1;
    private static final int MENU_ORDER_2 = 2;
    public static final int HEADER_VIEW_INDEX = 0;
    public static final String SELECTED_NOTE_ID = "noteID";
    public static final String NIGHT_MODE = "nightMode";
    public static final String MY_DIARY_PREFERENCES = "MyDiaryPreferences";
    private DrawerLayout mDrawer;
    private List<Note> mNotes;
    private NavigationView mNavigationView;
    private FloatingActionButton mFab;
    private View mHeaderView;
    private Toolbar mToolbar;
    private RecyclerView mNotesRecyclerView;
    private NotesAdapter mAdapter;
    private SharedPreferences.Editor mPreferenceEditor;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        restoreTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setSupportActionBar(mToolbar);

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, NewNoteActivity.class));
            }
        });

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, mToolbar, R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView.setNavigationItemSelectedListener(this);

        Switch themeSwitch =  mHeaderView.findViewById(R.id.theme_switch);

        if(AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES){
            themeSwitch.setChecked(true);
        }

        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
               toggleNightMode(isChecked);
            }
        });

        displayNotes();
        registerForContextMenu(mNotesRecyclerView);
    }

    private void restoreTheme() {
        mSharedPreferences = getSharedPreferences(MY_DIARY_PREFERENCES, MODE_PRIVATE);
        boolean isNightModeOn = mSharedPreferences.getBoolean(NIGHT_MODE, false);

        if(isNightModeOn){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void toggleNightMode(boolean isChecked) {
        mPreferenceEditor = mSharedPreferences.edit();
        if(isChecked) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            mPreferenceEditor.putBoolean(NIGHT_MODE, true);
        } else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            mPreferenceEditor.putBoolean(NIGHT_MODE, false);
        }
        recreate();
        mPreferenceEditor.apply();
    }

    private void initViews() {
        mNavigationView = findViewById(R.id.nav_view);
        mDrawer = findViewById(R.id.drawer_layout);
        mFab = findViewById(R.id.fab);
        mHeaderView = mNavigationView.getHeaderView(HEADER_VIEW_INDEX);
        mToolbar = findViewById(R.id.toolbar);
        mNotesRecyclerView = findViewById(R.id.notesRecyclerView);
    }

    private void displayNotes() {
        DataManager dataManager = DataManager.getInstance();
        mNotes = dataManager.getNotes();
        mAdapter = new NotesAdapter(this, mNotes);
        mNotesRecyclerView.setAdapter(mAdapter);
        mNotesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        mAdapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        Toast.makeText(this, String.valueOf(id), Toast.LENGTH_SHORT).show();

        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        long position = mAdapter.getAdapterPosition();

        Note note = mNotes.get((int) position);

        switch (item.getOrder()){
            case MENU_ORDER_0:
                Intent intent = new Intent(MainActivity.this, NewNoteActivity.class);
                intent.putExtra(SELECTED_NOTE_ID, 1);
                startActivity(intent);
                break;
            case MENU_ORDER_1:
                Toast.makeText(this, "Share", Toast.LENGTH_SHORT).show();
                break;
            case MENU_ORDER_2:
                Toast.makeText(this, "Delete", Toast.LENGTH_SHORT).show();
                break;

        }
        return true;
    }
}
