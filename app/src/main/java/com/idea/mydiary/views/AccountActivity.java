package com.idea.mydiary.views;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.idea.mydiary.R;
import com.idea.mydiary.Utils;
import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;
import com.idea.mydiary.viewmodels.AccountActivityViewModel;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Responsible for authenticating users using Google Sign in and backing up notes.
 */
public class AccountActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int RC_SIGN_IN = 0;
    private static final String TAG = "LoginActivity";
    private GoogleSignInClient mGoogleSignInClient;
    private Button mButtonLogin;
    private TextView mTextLogout;
    private Button mBackUp;
    private Button mRestoreBackup;
    private ProgressBar mProgressBar;
    private AccountActivityViewModel mViewModel;
    private FirebaseFirestore mFirestore;
    private String mAccountId;
    private List<Note> mNotesToBackUp;
    private Query mQuery;
    private FirebaseStorage mStorage;
    private StorageReference mStorageRef;
    private FirebaseAuth mFirebaseAuth;
    private List<Media> mMediaList;
    private File APP_FOLDER;
    private TextView mTextRestore;

    //Permissions required to backup and restore notes.
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private static final int REQUEST_PERMISSIONS_CODE = 14;
    private static final int PERMISSIONS_COUNT = PERMISSIONS.length;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        APP_FOLDER = new Utils(this).getAppFolder();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Init views and listeners
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mButtonLogin = findViewById(R.id.button_login);
        mTextLogout = findViewById(R.id.logout);
        mBackUp = findViewById(R.id.back_up);
        mRestoreBackup = findViewById(R.id.restore_backup);
        mProgressBar = findViewById(R.id.progressBar);
        mTextRestore = findViewById(R.id.text_restore);

        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        mStorageRef = mStorage.getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();

        mBackUp.setOnClickListener(this);
        mRestoreBackup.setOnClickListener(this);
        mButtonLogin.setOnClickListener(this);
        mTextLogout.setOnClickListener(this);

        mViewModel = new ViewModelProvider(this).get(AccountActivityViewModel.class);
        mViewModel.isProcessing().observe(this, isProcessing -> {
            if (isProcessing) { // Activity is busy.
                mProgressBar.setVisibility(View.VISIBLE);
            } else {
                mProgressBar.setVisibility(View.GONE);
            }
        });

        mViewModel.getBackedUpNotes(false).observe(this, noteList -> {
            if (noteList.size() < 1) {
                mBackUp.setVisibility(View.GONE);
                return;
            }
            if (mAccountId != null && !mAccountId.isEmpty()) {
                mNotesToBackUp = noteList;
            }
        });

        mViewModel.getBackedUpMedia(false).observe(
                this, items -> mMediaList = items
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            mAccountId = account.getId();
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putString("accountId", mAccountId);
            editor.apply();
        }
        updateUI(account);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!task.isSuccessful()) {
                        Toast.makeText(AccountActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                    recreate();
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_login:
                signIn();
                break;
            case R.id.logout:
                signOut();
                break;
            case R.id.back_up:
                startBackup();
                break;
            case R.id.restore_backup:
                restoreBackup();
                break;
        }
    }

    private void restoreBackup() {
        mBackUp.setVisibility(View.GONE);
        mRestoreBackup.setVisibility(View.GONE);
        mViewModel.setProcessing(true);
        restoreNoteFromFirestore();
    }

    private void startBackup() {
        if (mNotesToBackUp == null) return;
        mBackUp.setVisibility(View.GONE);
        mRestoreBackup.setVisibility(View.GONE);
        addNotesToFirestore(mNotesToBackUp);
    }

    private void addNotesToFirestore(List<Note> noteList) {
        mTextRestore.setVisibility(View.VISIBLE);
        mTextRestore.setText(R.string.note_uploading);
        CollectionReference notes = mFirestore.collection("notes");
        for (Note note : noteList) {
            note.setAccountId(mAccountId);
            notes.add(note)
                    .addOnSuccessListener(documentReference -> {
                        note.setBackedUp(true);
                        mViewModel.updateNote(note);
                    })
                    .addOnFailureListener(e -> Log.d("HRD", e.getMessage()));
        }
        uploadNoteMediaToFiresTore();
    }

    public void uploadNoteMediaToFiresTore() {
        if (mMediaList == null || mMediaList.size() == 0) return;
        for (Media media : mMediaList) {
            uploadMediaFiles(media);
        }
    }

    public void uploadMediaFiles(Media media) {
        mViewModel.setProcessing(true);
        try {
            mTextRestore.setText(R.string.media_backup);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Uri file = Uri.fromFile(new File(media.getUrl()));
        StorageReference reference = mStorageRef.child("images/" + file.getLastPathSegment());
        UploadTask uploadTask = reference.putFile(file);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            Task<Uri> url = reference.getDownloadUrl();
            url.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    if (downloadUri != null) {
                        media.setDownloadURL(downloadUri.toString());
                        uploadMediaObject(media);
                    }
                    mTextRestore.setText(R.string.all_notes_backed_up);
                } else {
                    mTextRestore.setText(R.string.some_notes_backed_up);
                }
                mViewModel.setProcessing(false);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
            Log.d("HRD", e.getMessage());
        });
    }

    public void uploadMediaObject(Media media) {
        CollectionReference mediaRef = mFirestore.collection("media");
        media.setAccountId(mAccountId);
        mediaRef.add(media)
                .addOnSuccessListener(documentReference -> {
                    media.setBackedUp(true);
                    mViewModel.updateMedia(media);
                });
    }

    public void restoreMediaFromFirestore() {
        mQuery = mFirestore.collection("media");
        mQuery = mQuery.whereEqualTo("accountId", mAccountId);
        mQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot document = task.getResult();
                if (document != null) {
                    List<Media> list = document.toObjects(Media.class);
                    for (Media media : list) {
                        media.setBackedUp(true);
                        downloadAndAttachImage(media);
                    }
                }
                mTextRestore.setText(R.string.all_notes_restored);
            } else {
                mTextRestore.setText(R.string.some_files_not_restored);
            }
            mViewModel.setProcessing(false);
        });
    }

    private void downloadAndAttachImage(Media media) {
        mViewModel.setProcessing(true);
        try {
            mTextRestore.setText(R.string.downloading_media);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String fileName = media.getUrl().split("/")[media.getUrl().split("/").length - 1];
        File destinationFile = new File(APP_FOLDER.getAbsolutePath() + "/" + fileName);
        if (destinationFile.getParentFile() != null && !destinationFile.getParentFile().exists())
            destinationFile.getParentFile().mkdirs();
        if (!destinationFile.exists()) {
            try {
                destinationFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        StorageReference httpsReference = mStorage.getReferenceFromUrl(media.getDownloadURL());
        httpsReference.getFile(destinationFile).addOnSuccessListener(taskSnapshot -> {
            media.setUrl(destinationFile.getAbsolutePath());
            mViewModel.insertMedia(media);
            mViewModel.setProcessing(false);
            try {
                mTextRestore.setText(R.string.restored);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).addOnFailureListener(exception -> {
            mViewModel.setProcessing(false);
            try {
                mTextRestore.setText(R.string.some_files_not_restored);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void restoreNoteFromFirestore() {
        mTextRestore.setVisibility(View.VISIBLE);
        mTextRestore.setText(R.string.restoring_notes);
        mQuery = mFirestore.collection("notes");
        mQuery = mQuery.whereEqualTo("accountId", mAccountId);
        mQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot document = task.getResult();
                if (document != null) {
                    List<Note> notes = document.toObjects(Note.class);
                    for (Note note : notes) {
                        note.setBackedUp(true);
                        mViewModel.insertNote(note);
                    }
                }
                restoreMediaFromFirestore();
            }
        });
    }

    private void signIn() {
        mViewModel.setProcessing(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account);
            } else {
                Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show();
            }
            updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Toast.makeText(this, "signInResult:failed code=" + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            updateUI(null);
        }
    }

    private void updateUI(GoogleSignInAccount account) {
        mViewModel.setProcessing(false);
        if (account != null) {
            mButtonLogin.setVisibility(View.GONE);
            mTextLogout.setVisibility(View.VISIBLE);
            mTextRestore.setVisibility(View.GONE);
            mBackUp.setVisibility(View.VISIBLE);
            mRestoreBackup.setVisibility(View.VISIBLE);
        } else {
            mTextLogout.setVisibility(View.GONE);
            mButtonLogin.setVisibility(View.VISIBLE);
            mTextRestore.setVisibility(View.VISIBLE);
            mBackUp.setVisibility(View.GONE);
            mRestoreBackup.setVisibility(View.GONE);
        }
    }

    private void signOut() {
        mViewModel.setProcessing(true);
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    task.addOnSuccessListener(aVoid -> {
                        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                                R.string.string_sing_out_success, Snackbar.LENGTH_LONG);
                        snackbar.show();
                        finish();
                    });
                    task.addOnFailureListener(e -> {
                        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                                R.string.string_failed, Snackbar.LENGTH_LONG);
                        snackbar.show();
                        mViewModel.setProcessing(false);
                    });
                });
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
            recreate();
        } else {
            onResume();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (permissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        }
    }
}