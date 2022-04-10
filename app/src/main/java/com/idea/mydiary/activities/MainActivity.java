package com.idea.mydiary.activities;

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
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Observer;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.idea.mydiary.BuildConfig;
import com.idea.mydiary.R;
import com.idea.mydiary.adapters.NotesAdapter;
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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final int HEADER_VIEW_INDEX = 0;
    public static final String SELECTED_NOTE_ID = "noteID";
    public static final String NIGHT_MODE = "nightMode";
    public static final String MY_DIARY_PREFERENCES = "MyDiaryPreferences";
    public static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;
    public static final int FEEDBACK_REQUEST_CODE = 3;
    private static final String KEY_NAME = "my_key";
    private static final byte[] SECRET_BYTE_ARRAY = new byte[]{1, 2, 3, 4, 5, 6};
    private static final int AUTHENTICATION_DURATION_SECONDS = 30;
    private static final int REQUEST_INVITE = 2;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    private static final int REQUEST_PERMISSIONS_CODE = 4;
    private static final int PERMISSIONS_COUNT = PERMISSIONS.length;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        restoreTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        handleDeepLink();
        setSupportActionBar(mToolbar);
        registerForContextMenu(mNotesRecyclerView);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isProtected = pref.getBoolean("protect", false);
        if (isProtected) {
            createKey();
            tryEncrypt();
        }

        mFab.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, NewNoteActivity.class)));

        // Navigation drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, mToolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();
        mNavigationView.setNavigationItemSelectedListener(this);

        // Night/Day mode toggling
        Switch themeSwitch = mHeaderView.findViewById(R.id.theme_switch);
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            themeSwitch.setChecked(true);
        }
        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleNightMode(isChecked);
            }
        });

        mViewModel.getAllNotes().observe(this, new Observer<List<Note>>() {
            @Override
            public void onChanged(@Nullable final List<Note> notes) {
                mAdapter.setNotes(notes, itemToRemovePos);
                mNotes = notes;
                itemToRemovePos = -1;
            }
        });

        mAdapter.setOnNoteDeleteListener(note -> mViewModel.deleteNote(note));
        updateLoggedInUserInfo();
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
                .setAppBarColors(R.color.colorPrimary, android.R.color.white, android.R.color.white, android.R.color.white)
                .setInputColors(R.color.navBackground, R.color.blackWhite, R.color.lowContrastTextColor)
                .setScreenColors(R.color.background, R.color.contrastTextColor);

        Intent userFeedbackIntent = Bugfender.getUserFeedbackActivityIntent(
                this,
                "Feed",
                "Kindly complete the form below.",
                "Subject",
                "Message",
                "Send",
                feedbackStyle);
        startActivityForResult(userFeedbackIntent, FEEDBACK_REQUEST_CODE);
    }

    private void handleDeepLink() {
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
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
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("HRD", "getDynamicLink:onFailure", e);
                    }
                });
    }

    private boolean checkAuthentication() {
        KeyguardManager mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        }
        return true;
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

    private void init() {
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
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
        File pdfFile = new File(pdfFolder.getAbsoluteFile() + "/" + timeStamp + ".pdf");
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
            } else{
                Log.d("HRD", "Can't create file." + pdfFile.getAbsolutePath());
            }

        } catch (IOException e) {
//            Toast.makeText(this, "Can't save note", Toast.LENGTH_SHORT).show();
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissionsDenied()) {
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
            // Challenge completed, proceed with using cipher
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
                // ...
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
//                .setCustomImage(Uri.parse(getString(R.string.invitation_custom_image)))
                .setCallToActionText("OPEN")
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    private boolean tryEncrypt() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_NAME, null);
            Cipher cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);

            // Try encrypting something, it will only work if the user authenticated within
            // the last AUTHENTICATION_DURATION_SECONDS seconds.
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            cipher.doFinal(SECRET_BYTE_ARRAY);

            return true;
        } catch (UserNotAuthenticatedException e) {
            // User is not authenticated, let's authenticate with device credentials.
            checkAuthentication();
            return false;
        } catch (KeyPermanentlyInvalidatedException e) {
            // This happens if the lock screen has been disabled or reset after the key was
            // generated after the key was generated.
            Toast.makeText(this, "Keys are invalidated after created. Retry the purchase\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return false;
        } catch (BadPaddingException | IllegalBlockSizeException | KeyStoreException |
                CertificateException | UnrecoverableKeyException | IOException
                | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {

            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.string_set_lock_screen, Snackbar.LENGTH_LONG);
            snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorAccent));
            snackbar.show();
            return false;
        }
    }

    private void createKey() {
        // Generate a key to decrypt payment credentials, tokens, etc.
        // This will most likely be a registration step for the user when they are setting up your app.
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    // Require that the user has unlocked in the last 30 seconds
                    .setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException
                | InvalidAlgorithmParameterException | KeyStoreException
                | CertificateException | IOException e) {

            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.string_key_creation_failed, Snackbar.LENGTH_LONG);
            snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorAccent));
            snackbar.show();
        }
    }

    private boolean permissionsDenied() {
        for (int i = 0; i < PERMISSIONS_COUNT; i++) {
            if (checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
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
            snackbar.setAction("Open", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(pdfURI, "application/pdf");
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    activity.startActivity(intent);
                }
            });
            snackbar.show();
        }
    }

    private static class SinglePDFGenerationAsyncTask extends AsyncTask<Long, Void, String> {
        private final WeakReference<MainActivity> mActivityReference;

        private SinglePDFGenerationAsyncTask(MainActivity context) {
            mActivityReference = new WeakReference<>(context);
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
            snackbar.setAction("Open", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(pdfURI, "application/pdf");
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    activity.startActivity(intent);
                }
            });
            snackbar.show();
        }
    }
}
