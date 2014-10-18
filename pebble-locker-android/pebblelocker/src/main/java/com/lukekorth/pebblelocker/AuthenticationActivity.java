package com.lukekorth.pebblelocker;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lukekorth.pebblelocker.services.LockingIntentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationActivity extends Activity
        implements TextView.OnEditorActionListener, TextWatcher {

    public static final String AUTHENTICATION_TYPE_KEY = "authentication_type";
    public static final int AUTHENTICATE = 0;
    public static final int CHANGE_TO_SLIDE = 1;
    public static final int CHANGE_TO_PIN = 2;
    public static final int CHANGE_TO_PASSWORD = 3;

    private static final String TAG = "Authentication";

    private Logger mLogger;
    private SharedPreferences mPrefs;
    private int mRequestType;
    private ScreenLockType mCurrentType;
    private int mEntryNumber;
    private String mFirstEntry;

    private TextView mAuthenticationMessage;
    private EditText mPasswordInput;
    private Button mContinueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authentication);

        mAuthenticationMessage = (TextView) findViewById(R.id.authentication_message);
        mPasswordInput = (EditText) findViewById(R.id.password_input);
        mPasswordInput.setOnEditorActionListener(this);
        mPasswordInput.addTextChangedListener(this);
        mContinueButton = (Button) findViewById(R.id.continue_button);

        mLogger = LoggerFactory.getLogger(TAG);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mRequestType = getIntent().getIntExtra(AUTHENTICATION_TYPE_KEY, AUTHENTICATE);
        mCurrentType = ScreenLockType.getCurrent(this);

        mContinueButton.setEnabled(false);

        if (mRequestType == AUTHENTICATE) {
            findViewById(R.id.cancel_button).setEnabled(false);
        } else {
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        mLogger.debug("Authentication request is " + mRequestType);
        mLogger.debug("Current lock type is " + mCurrentType);

        requestCurrentAuthentication();
    }

    private void requestCurrentAuthentication() {
        if (mCurrentType == ScreenLockType.SLIDE) {
            mEntryNumber = 1;
            authenticationSuccessful();
        } else {
            if (mRequestType == AUTHENTICATE || mRequestType == CHANGE_TO_SLIDE) {
                mContinueButton.setText(R.string.finish);
                setImeActionDone();
            } else {
                setImeActionNext();
            }

            if (mCurrentType == ScreenLockType.PIN) {
                mEntryNumber = 0;
                setTitle(R.string.enter_pin);
                mPasswordInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            } else if (mCurrentType == ScreenLockType.PASSWORD) {
                mEntryNumber = 0;
                setTitle(R.string.enter_password);
                mPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
        }
    }

    public void onContinue(View v) {
        if (mContinueButton.isEnabled()) {
            String enteredText = mPasswordInput.getText().toString();
            mPasswordInput.setText("");
            if (mRequestType == AUTHENTICATE || mEntryNumber == 0) {
                if (enteredText.equals(mPrefs.getString("key_password", ""))) {
                    mEntryNumber++;
                    authenticationSuccessful();
                } else {
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.incorrect_authentication)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    requestCurrentAuthentication();
                                }
                            })
                            .show();
                }
            } else if (mEntryNumber == 1) {
                mFirstEntry = enteredText;
                mEntryNumber++;

                mContinueButton.setText(R.string.finish);
                setImeActionDone();

                if (mRequestType == CHANGE_TO_PIN) {
                    setTitle(R.string.reenter_pin);
                } else if (mRequestType == CHANGE_TO_PASSWORD) {
                    setTitle(R.string.reenter_password);
                }
            } else if (mEntryNumber == 2) {
                if (mFirstEntry.equals(enteredText)) {
                    changePassword(enteredText);
                    finishSuccessfully();
                } else {
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.passwords_did_not_match)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();

                    mEntryNumber = 1;
                    mContinueButton.setText(R.string.next);
                    if (mCurrentType == ScreenLockType.PIN) {
                        enterNewPin();
                    } else if (mCurrentType == ScreenLockType.PASSWORD) {
                        enterNewPassword();
                    }
                }
            }
        }
    }

    public void onCancel(View v) {
        onBackPressed();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
            onContinue(null);
            return true;
        }
        return false;
    }

    private void authenticationSuccessful() {
        switch (mRequestType) {
            case AUTHENTICATE:
                finishSuccessfully();
                break;
            case CHANGE_TO_SLIDE:
                changePassword("");
                finishSuccessfully();
                break;
            case CHANGE_TO_PIN:
                enterNewPin();
                break;
            case CHANGE_TO_PASSWORD:
                enterNewPassword();
                break;
        }
    }

    private void enterNewPin() {
        setTitle(R.string.choose_pin);
        mPasswordInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        setImeActionNext();
    }

    private void enterNewPassword() {
        setTitle(R.string.choose_password);
        mPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        setImeActionNext();
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mRequestType != AUTHENTICATE && mEntryNumber > 0) {
            if (s.length() > 0 && s.length() < 4) {
                if (mRequestType == CHANGE_TO_PIN) {
                    mAuthenticationMessage.setText(R.string.pin_minimum);
                } else if (mRequestType == CHANGE_TO_PASSWORD) {
                    mAuthenticationMessage.setText(R.string.password_minimum);
                }
                mContinueButton.setEnabled(false);
            } else if (s.length() > 16) {
                if (mRequestType == CHANGE_TO_PIN) {
                    mAuthenticationMessage.setText(R.string.pin_maximum);
                } else if (mRequestType == CHANGE_TO_PASSWORD) {
                    mAuthenticationMessage.setText(R.string.password_maximum);
                }
                mContinueButton.setEnabled(false);
            } else if (s.length() > 0 && mRequestType == CHANGE_TO_PIN && !s.toString().matches("[0-9]+")) {
                mAuthenticationMessage.setText(R.string.bad_pin);
                mContinueButton.setEnabled(false);
            } else {
                mAuthenticationMessage.setText("");
                mContinueButton.setEnabled(true);
            }
        } else {
            mContinueButton.setEnabled(true);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }


    private void finishSuccessfully() {
        setResult(RESULT_OK);
        finish();
    }

    private void finishCanceled() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mRequestType != AUTHENTICATE) {
            finishCanceled();
        } else {
            mLogger.debug("User pressed back or cancel while authenticating");
        }
    }

    private void changePassword(String newPassword) {
        if (alertIfMonkey()) {
            return;
        }

        switch (mRequestType) {
            case CHANGE_TO_SLIDE:
                mLogger.debug("Action was successful, removing pin/password and changing to slide");
                ScreenLockType.changeToSlide(this);
                startService(new Intent(
                        this, LockingIntentService.class).putExtra(LockingIntentService.UNLOCK, true));
                break;
            case CHANGE_TO_PIN:
                mLogger.debug("Action was successful, changing to pin");
                ScreenLockType.setCurrent(this, ScreenLockType.PIN);
                mPrefs.edit().putString("key_password", newPassword).apply();
                startService(new Intent(
                        this, LockingIntentService.class).putExtra(LockingIntentService.LOCK, true));
                break;
            case CHANGE_TO_PASSWORD:
                mLogger.debug("Action was successful, changing to password");
                ScreenLockType.setCurrent(this, ScreenLockType.PASSWORD);
                mPrefs.edit().putString("key_password", newPassword).apply();
                startService(new Intent(
                        this, LockingIntentService.class).putExtra(LockingIntentService.LOCK, true));
                break;
        }
    }

    private void setImeActionDone() {
        mPasswordInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mPasswordInput.setImeActionLabel(getString(R.string.done), EditorInfo.IME_ACTION_DONE);
    }

    private void setImeActionNext() {
        mPasswordInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mPasswordInput.setImeActionLabel(getString(R.string.next), EditorInfo.IME_ACTION_NEXT);
    }

    /**
     * If the "user" is a monkey, post an alert and notify the caller.  This prevents automated
     * test frameworks from stumbling into annoying or dangerous operations.
     */
    private boolean alertIfMonkey() {
        if (ActivityManager.isUserAMonkey()) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.monkey)
                    .show();
            return true;
        } else {
            return false;
        }
    }

}
