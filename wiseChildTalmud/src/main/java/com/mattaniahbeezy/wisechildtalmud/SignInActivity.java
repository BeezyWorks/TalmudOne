package com.mattaniahbeezy.wisechildtalmud;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.parse.LogInCallback;
import com.parse.LogOutCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by Mattaniah on 6/28/2015.
 */
public class SignInActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {
    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;
    /* Is there a ConnectionResult resolution in progress? */
    private boolean mIsResolving = false;

    /* Should we automatically resolve ConnectionResults when possible? */
    private boolean mShouldResolve = false;

    private static final String TAG = "SignIn Activity";

    View signInButton;
    View signOutButton;

    ImageView profileImage;
    TextView profileName;
    AppBarLayout appBarLayout;

    TextView statBox;

    private final String faceFileName = "userface.png";
    private final String backgroundFileName = "userbackground.png";

    private final int REQUEST_CODE_ASK_PERMISSIONS = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin_layout);

        signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(this);
        signOutButton = findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(this);

        profileImage = (ImageView) findViewById(R.id.profilePicture);
        profileName = (TextView) findViewById(R.id.accountName);

        appBarLayout = (AppBarLayout) findViewById(R.id.appbarlayout);

        statBox = (TextView) findViewById(R.id.statBox);
        statBox.setText(new TimeLearnedUtil(this).getTotalTimeAsString());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Build GoogleApiClient with access to basic profile
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addScope(new Scope(Scopes.PROFILE))
                .build();

    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        // onConnected indicates that an account was selected on the device, that the selected
        // account has granted any requested permissions to our app and that we were able to
        // establish a service connection to Google Play services.
        Log.d(TAG, "onConnected:" + bundle);
        mShouldResolve = false;

        // Show the signed-in UI
        showSignedInUI();
        signInWithParse();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                onSignInClicked();
                break;
            case R.id.sign_out_button:
                onSignOutClicked();
                break;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Could not connect to Google Play Services.  The user needs to select an account,
        // grant permissions or resolve an error in order to sign in. Refer to the javadoc for
        // ConnectionResult to see possible error codes.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);

        if (!mIsResolving && mShouldResolve) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(this, RC_SIGN_IN);
                    mIsResolving = true;
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Could not resolve ConnectionResult.", e);
                    mIsResolving = false;
                    mGoogleApiClient.connect();
                }
            } else {
                // Could not resolve the connection result, show the user an
                // error dialog.
                showErrorDialog(connectionResult);
            }
        } else {
            // Show the signed-out UI
            showSignedOutUI();
        }
    }

    private void showErrorDialog(ConnectionResult connectionResult) {
        new AlertDialog.Builder(this)
                .setMessage(connectionResult.toString()).create().show();
    }

    private void onSignInClicked() {
        // User clicked the sign-in button, so begin the sign-in process and automatically
        // attempt to resolve any errors that occur.
        mShouldResolve = true;
        mGoogleApiClient.connect();

        // Show a message to the user that we are signing in.
        profileName.setText("Signing in...");
    }

    private void onSignOutClicked() {
        // Clear the default account so that GoogleApiClient will not automatically
        // connect in the future.
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }

        showSignedOutUI();
        if (ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            ParseUser.logOutInBackground(new LogOutCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null)
                        Log.d(TAG, "Parse Logged Out");
                    else
                        Log.d(TAG, e.toString());
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);

        if (requestCode == RC_SIGN_IN) {
            // If the error resolution was not successful we should not resolve further.
            if (resultCode != RESULT_OK) {
                mShouldResolve = false;
            }

            mIsResolving = false;
            mGoogleApiClient.connect();
        }
    }

    private void showSignedOutUI() {
        profileImage.setImageResource(R.drawable.ic_account);
        profileName.setText("Signed Out");
        clearAccountInformation();
        signOutButton.setVisibility(View.GONE);
        signInButton.setVisibility(View.VISIBLE);
        appBarLayout.setBackgroundResource(0);
        appBarLayout.setBackgroundColor(getResources().getColor(R.color.primary));
    }

    private Drawable getSavedImage(String fileName) throws FileNotFoundException {
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        File file = new File(extStorageDirectory, fileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        return Drawable.createFromStream(fileInputStream, fileName);
    }

    private void saveImageFile(Bitmap bm, String fileName) throws IOException {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
            File file = new File(extStorageDirectory, fileName);
            FileOutputStream outStream = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                } else {
                    // Permission Denied
                    Toast.makeText(this, "WRITE_EXTERNAL Denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void signInWithParse() {
        if (Plus.AccountApi.getAccountName(mGoogleApiClient) == null)
            return;
        final Person signedInUser = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        ParseUser.logInInBackground(signedInUser.getId(), "password", new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if (!mGoogleApiClient.isConnected())
                    return;
                if (user != null && e == null) {
                    Log.d("Sign In", "Success");
                    user.setEmail(Plus.AccountApi.getAccountName(mGoogleApiClient));
                    if (signedInUser.hasDisplayName())
                        user.put("Display Name", signedInUser.getDisplayName());
                    if (signedInUser.hasGender())
                        user.put("Gender", signedInUser.getGender());
                    if (signedInUser.hasAgeRange())
                        user.put("Age Range", signedInUser.getAgeRange());
                    user.saveInBackground(null);
                } else {
                    Log.d("Sign In", "Creating New User");
                    ParseUser.getCurrentUser().setUsername(signedInUser.getId());
                    ParseUser.getCurrentUser().setEmail(Plus.AccountApi.getAccountName(mGoogleApiClient));
                    ParseUser.getCurrentUser().setPassword("password");
                    ParseUser.getCurrentUser().signUpInBackground(new SignUpCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null)
                                Log.d("Signed UP", "Success");
                            else
                                Log.d("Signed UP", e.toString());
                        }
                    });
                }
            }

        });
    }

    private void showSignedInUI() {
        final Person signedInUser = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);

        String accountName = signedInUser.getDisplayName();
        profileName.setText(accountName);

        class BackgroundImage extends AsyncTask {
            Drawable drawable;

            @Override
            protected Object doInBackground(Object[] params) {
                try {
                    drawable = Drawable.createFromStream((InputStream) new URL(signedInUser.getCover().getCoverPhoto().getUrl()).getContent(), "Cover Photo");
                    saveImageFile(ViewFactory.drawableToBitmap(drawable), backgroundFileName);
                } catch (NullPointerException | IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                if (drawable != null)
                    appBarLayout.setBackground(drawable);
            }
        }
        new BackgroundImage().execute();
        new ViewFactory.URLToImageView(profileImage, getImageURL(signedInUser), (int) getResources().getDimension(R.dimen.profileImageDiameter_big)).execute();

        try {

            appBarLayout.setBackground(getSavedImage(backgroundFileName));
        } catch (Exception e) {
        }

        saveAccountInforamtion(signedInUser);
        signInButton.setVisibility(View.GONE);
        signOutButton.setVisibility(View.VISIBLE);
    }


    private String getImageURL(Person user) {
        String imageUrl = user.getImage().getUrl();
        return imageUrl.replace("sz=50", "sz=250");
    }

    private void saveAccountInforamtion(Person user) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(getString(R.string.profileImageURLKey), getImageURL(user));
        editor.putString(getString(R.string.profileNameKey), user.getDisplayName());
        editor.putString(getString(R.string.profileEmailKey), Plus.AccountApi.getAccountName(mGoogleApiClient));
        editor.apply();
    }

    private void clearAccountInformation() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(getString(R.string.profileImageURLKey), null);
        editor.putString(getString(R.string.profileNameKey), null);
        editor.putString(getString(R.string.profileEmailKey), null);
        editor.apply();
    }
}
