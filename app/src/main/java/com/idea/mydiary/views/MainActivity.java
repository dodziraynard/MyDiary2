package com.idea.mydiary.views;

import static com.idea.mydiary.adapters.NotesAdapter.MENU_EDIT;
import static com.idea.mydiary.adapters.NotesAdapter.MENU_EXPORT_PDF;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bugfender.sdk.Bugfender;
import com.bugfender.sdk.ui.FeedbackStyle;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.idea.mydiary.BuildConfig;
import com.idea.mydiary.R;
import com.idea.mydiary.adapters.NotesAdapter;
import com.idea.mydiary.databinding.ActivityMainBinding;
import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;
import com.idea.mydiary.types.MediaType;
import com.idea.mydiary.viewmodels.MainActivityViewModel;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.TextAlignment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    public static final String SELECTED_NOTE_ID = "noteID";
    public static final String NIGHT_MODE = "nightMode";
    public static final String MY_DIARY_PREFERENCES = "MyDiaryPreferences";
    public static final int HEADER_VIEW_INDEX = 0;
    public static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;
    public static final int FEEDBACK_REQUEST_CODE = 3;
    private static final int REQUEST_INVITE = 2;
    private static final int REQUEST_PERMISSIONS_CODE = 4;
    private DrawerLayout mDrawer;
    private List<Note> mNotes;
    private NavigationView mNavigationView;
    private FloatingActionButton mFab;
    private View mHeaderView;
    private Toolbar mToolbar;
    private RecyclerView mNotesRecyclerView;
    private NotesAdapter mAdapter;
    private SharedPreferences mSharedPreferences;
    private MainActivityViewModel mViewModel;
    private long itemToRemovePos = -1;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        restoreTheme();
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
        setSupportActionBar(mToolbar);
        registerForContextMenu(mNotesRecyclerView);

        // Check and handle firebase deep link
        handleDeepLink();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isProtected = pref.getBoolean("protect", false);
        if (isProtected) {
            // Let the user enter their credential.
            confirmDeviceCredentials();
        }

        // On click of the FAB, go to new note screen.
        mFab.setOnClickListener(view -> startActivity(
                new Intent(MainActivity.this, NewNoteActivity.class))
        );

        // Configure side navigation drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, mToolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();
        mNavigationView.setNavigationItemSelectedListener(this);

        // Configure the view Night/Day mode toggling
        Switch themeSwitch = mHeaderView.findViewById(R.id.theme_switch);
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            themeSwitch.setChecked(true);
        }
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleNightMode(isChecked);
        });

        // Retrieve all notes from the room db.
        mViewModel.getAllNotes().observe(this, notes -> {
            mAdapter.setNotes(notes, itemToRemovePos);
            mNotes = notes;
            itemToRemovePos = -1;
        });
        mAdapter.setOnNoteDeleteListener(note -> mViewModel.deleteNote(note));
        updateLoggedInUserInfo();
    }

    private void initView() {
        mViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        mNavigationView = findViewById(R.id.nav_view);
        mDrawer = findViewById(R.id.drawer_layout);
        mFab = findViewById(R.id.fab);
        mHeaderView = mNavigationView.getHeaderView(HEADER_VIEW_INDEX);
        mToolbar = findViewById(R.id.toolbar);
        mNotesRecyclerView = findViewById(R.id.notesRecyclerView);
        mAdapter = new NotesAdapter(this, MainActivity.this);
        mNotesRecyclerView.setAdapter(mAdapter);
        mNotesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper itemTouchHelper = new
                ItemTouchHelper(new NotesAdapter.SwipeToDeleteCallback(mAdapter));
        itemTouchHelper.attachToRecyclerView(mNotesRecyclerView);
    }

    private void updateLoggedInUserInfo() {
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(MainActivity.this);
        if (acct != null) {
            String personEmail = acct.getEmail();
            TextView emailText = mHeaderView.findViewById(R.id.user_email);
            emailText.setText(personEmail);
        }
    }

    private void openFeedBackActivity() {
        FeedbackStyle feedbackStyle = new FeedbackStyle()
                .setAppBarColors(R.color.colorPrimary,
                        android.R.color.white,
                        android.R.color.white,
                        android.R.color.white)
                .setInputColors(R.color.navBackground,
                        R.color.blackWhite,
                        R.color.lowContrastTextColor)
                .setScreenColors(R.color.background, R.color.contrastTextColor);

        Intent userFeedbackIntent = Bugfender.getUserFeedbackActivityIntent(
                this,
                getString(R.string.feed),
                getString(R.string.complete_form),
                getString(R.string.subject),
                getString(R.string.message),
                getString(R.string.send),
                feedbackStyle);
        startActivityForResult(userFeedbackIntent, FEEDBACK_REQUEST_CODE);
    }

    private void handleDeepLink() {
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, pendingDynamicLinkData -> {
                    // Get deep link from result (may be null if no link is found)
                    Uri deepLink = null;
                    if (pendingDynamicLinkData != null) {
                        deepLink = pendingDynamicLinkData.getLink();
                    }
                    if (deepLink != null) {
                        if (deepLink.toString().equals(getString(R.string.invitation_deep_link))) {
                            startActivity(new Intent(MainActivity.this, NewNoteActivity.class));
                        }
                    }
                })
                .addOnFailureListener(this, e -> Log.w(MainActivity.class.getSimpleName(), "getDynamicLink:onFailure", e));
    }

    private void confirmDeviceCredentials() {
        KeyguardManager mKeyguardManager =
                (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(
                getString(R.string.protection_header),
                getString(R.string.protected_note_info));
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        } else {
            Toast.makeText(this, R.string.no_lock_screen_info, Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreTheme() {
        mSharedPreferences = getSharedPreferences(MY_DIARY_PREFERENCES, MODE_PRIVATE);
        boolean isNightModeOn = mSharedPreferences.getBoolean(NIGHT_MODE, false);
        if (isNightModeOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void toggleNightMode(boolean isChecked) {
        SharedPreferences.Editor preferenceEditor = mSharedPreferences.edit();
        if (isChecked) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            preferenceEditor.putBoolean(NIGHT_MODE, true);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            preferenceEditor.putBoolean(NIGHT_MODE, false);
        }
        recreate();
        preferenceEditor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();

        switch (id) {
            case R.id.nav_protect:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.nav_account:
                startActivity(new Intent(MainActivity.this, AccountActivity.class));
                break;
            case R.id.nav_share:
                onInviteClicked();
                break;
            case R.id.nav_feedback:
                openFeedBackActivity();
                break;
            case R.id.nav_export:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissionsDenied()) {
                    requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS_CODE);
                }

                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                        "Processing", Snackbar.LENGTH_LONG);
                snackbar.setActionTextColor(Color.WHITE);
                snackbar.show();

                new PDFGenerationAsyncTask(MainActivity.this).execute();
                break;
        }
        return false;
    }

    private String createPDFFromNotes(long id) {
        File pdfFolder = new File(Environment.getExternalStorageDirectory().getAbsoluteFile().toString() + "/My Diary/PDF/");
        if (!pdfFolder.exists()) {
            pdfFolder.mkdirs();
        }

        //Create timestamp
        Date date = new Date();
        File pdfFile;
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
        pdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), timeStamp + ".pdf");
        OutputStream outputStream = null;
        Document document = null;

        try {
            boolean fileCreated = true;
            if (!pdfFile.exists()) {
                fileCreated = pdfFile.createNewFile();
            }
            if (fileCreated) {
                outputStream = new FileOutputStream(pdfFile);
                PdfWriter writer = new PdfWriter(outputStream);
                PdfDocument pdf = new PdfDocument(writer);
                document = new Document(pdf);
            }
        } catch (IOException e) {
            Log.d("HRD", e.getMessage() + " " + pdfFile.getAbsolutePath());
        }

        if (outputStream != null) {
            Paragraph header = new Paragraph("ENTRIES")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(30)
                    .setBold();
            document.add(header);

            for (Note note : mNotes) {
                if (id != -1) {
                    if (note.getId() != id) {
                        continue;
                    }
                }

                //Title
                Paragraph subHeader = new Paragraph(note.getTitle())
                        .setTextAlignment(TextAlignment.LEFT)
                        .setFontSize(20);
                document.add(subHeader);

                // Note text
                Paragraph text = new Paragraph(note.getText())
                        .setTextAlignment(TextAlignment.JUSTIFIED)
                        .setFontSize(15);
                document.add(text);

                List<Media> mediaList = mViewModel.getNotesMedia(note.getId());
                for (Media media : mediaList) {
                    if (!media.getMediaType().equals(MediaType.IMAGE.name())) continue;
                    try {
                        ImageData data = ImageDataFactory.create(media.getUrl());
                        Image img = new Image(data);
                        document.add(img);
                    } catch (MalformedURLException e) {
                        Log.d("HRD", e.getMessage());
                        e.printStackTrace();
                    }
                }

                document.add(new Paragraph());
                LineSeparator ls = new LineSeparator(new SolidLine());
                document.add(ls);

                // Date
                Paragraph dateText = new Paragraph("[ " + note.getFullDate() + " ]")
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFontSize(12);
                document.add(dateText);
                document.add(new Paragraph());
                document.add(new Paragraph());
                document.add(new Paragraph());
            }
            document.close();
            return pdfFile.getAbsolutePath();
        }
        return null;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        long position = mAdapter.getAdapterPosition();

        Note note = mNotes.get((int) position);

        switch (item.getOrder()) {
            case MENU_EDIT:
                Intent intent = new Intent(MainActivity.this, NewNoteActivity.class);
                intent.putExtra(SELECTED_NOTE_ID, note.getId());
                startActivity(intent);
                break;
            case MENU_EXPORT_PDF:
                if (permissionsDenied()) {
                    requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS_CODE);
                }

                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                        "Processing", Snackbar.LENGTH_LONG);
                snackbar.setActionTextColor(Color.WHITE);
                View view = snackbar.getView();
                TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
                tv.setTextColor(Color.WHITE);
                snackbar.show();

                new SinglePDFGenerationAsyncTask(MainActivity.this).execute(note.getId());
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (resultCode != RESULT_OK) {
                finish();
            }
        } else if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Get the invitation IDs of all sent messages
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                for (String id : ids) {
                    Log.d("HRD", "onActivityResult: sent invitation " + id);
                }
            } else {
                // Sending failed or it was canceled, show failure message to the user
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == FEEDBACK_REQUEST_CODE) {
            Toast.makeText(this,
                    resultCode == Activity.RESULT_OK ? "Feedback sent" : "Feedback cancelled",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void onInviteClicked() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
                .setCallToActionText("OPEN")
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    private boolean permissionsDenied() {
        for (String permission : PERMISSIONS) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionsDenied()) {
            Toast.makeText(this, "Perm Denied", Toast.LENGTH_LONG).show();
            ((ActivityManager) (this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
            finish();
        } else {
            onResume();
        }
    }

    private static class PDFGenerationAsyncTask extends AsyncTask<Void, Void, String> {
        private final WeakReference<MainActivity> mActivityReference;

        private PDFGenerationAsyncTask(MainActivity context) {
            mActivityReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(Void... params) {
            MainActivity activity = mActivityReference.get();
            return activity.createPDFFromNotes(-1);
        }

        @Override
        protected void onPostExecute(String url) {
            super.onPostExecute(url);
            MainActivity activity = mActivityReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            activity.permissionsDenied();
            if (url == null) {
                Toast.makeText(activity, R.string.couldnt_generate_pdf, Toast.LENGTH_SHORT).show();
                return;
            }

            File fileWithinMyDir = new File(url);
            Uri pdfURI = FileProvider.getUriForFile(activity,
                    BuildConfig.APPLICATION_ID + ".provider",
                    fileWithinMyDir);
            Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content),
                    "Filed saved to " + url, Snackbar.LENGTH_LONG);
            snackbar.setActionTextColor(Color.WHITE);
            View view = snackbar.getView();
            TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);

            snackbar.setAction("Open", view1 -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(pdfURI, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivity(intent);
            });
            snackbar.show();
        }
    }

    private static class SinglePDFGenerationAsyncTask extends AsyncTask<Long, Void, String> {
        private final WeakReference<MainActivity> mActivityReference;

        private SinglePDFGenerationAsyncTask(MainActivity activity) {
            mActivityReference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(Long... longs) {
            long id = longs[0];
            MainActivity activity = mActivityReference.get();
            return activity.createPDFFromNotes(id);
        }

        @Override
        protected void onPostExecute(String url) {
            super.onPostExecute(url);
            MainActivity activity = mActivityReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            activity.permissionsDenied();

            File fileWithinMyDir = new File(url);
            Uri pdfURI = FileProvider.getUriForFile(activity,
                    BuildConfig.APPLICATION_ID + ".provider",
                    fileWithinMyDir);

            Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content),
                    "Filed saved to " + url, Snackbar.LENGTH_LONG);
            snackbar.setActionTextColor(Color.WHITE);
            View view = snackbar.getView();
            TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);
            snackbar.setAction("Open", view1 -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(pdfURI, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivity(intent);
            });
            snackbar.show();
        }
    }
}
