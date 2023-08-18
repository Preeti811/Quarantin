package com.example.quarantin;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import androidx.security.crypto.MasterKeys;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.andrognito.pinlockview.IndicatorDots;
import com.andrognito.pinlockview.PinLockListener;
import com.andrognito.pinlockview.PinLockView;

public class PasscodeActivity extends AppCompatActivity {
    PinLockView mPinLockView;
    IndicatorDots mIndicatorDots;

    private String currentPin = "1234"; // Set your current PIN here
    private String newPin = ""; // Store the new PIN here
    private boolean isNewPinSetting = false; // Flag to indicate if new PIN is being set

    //    private SharedPreferences sharedPreferences;
    private EncryptedSharedPreferences securePreferences;
    private static final String PREF_NAME = "pin_pref";
    private static final String KEY_CURRENT_PIN = "current_pin";
    private static final String KEY_NEW_PIN = "new_pin";
    private static final String KEY_IS_NEW_PIN_SETTING = "is_new_pin_setting";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_passcode);

        mPinLockView = findViewById(R.id.pin_lock_view);
        mIndicatorDots = findViewById(R.id.indicator_dots);

        mPinLockView.attachIndicatorDots(mIndicatorDots);
        mPinLockView.setPinLockListener(mPinLockListener);

        mPinLockView.setPinLength(4);
        mPinLockView.setTextColor(ContextCompat.getColor(this, R.color.white));

        mIndicatorDots.setIndicatorType(IndicatorDots.IndicatorType.FILL_WITH_ANIMATION);

        Context context = getApplicationContext();

        // Initialize shared preferences

        try {

            MasterKey masterKey = new MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();


            securePreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    context,
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );


        } catch (Exception e) {
            // Handle exceptions
        }

        currentPin = securePreferences.getString(KEY_CURRENT_PIN,currentPin);
        newPin = securePreferences.getString(KEY_NEW_PIN, newPin);
        isNewPinSetting = securePreferences.getBoolean(KEY_IS_NEW_PIN_SETTING, false);
    }

    private PinLockListener mPinLockListener = new PinLockListener() {
        @Override
        public void onComplete(String pin) {
            Log.d(TAG, "Pin complete: " + pin);

            if (isNewPinSetting) {
                // Set new PIN logic
                if (newPin.isEmpty()) {
                    newPin = pin;
                    Toast.makeText(PasscodeActivity.this, "Please confirm new PIN", Toast.LENGTH_SHORT).show();
                    mPinLockView.resetPinLockView();
                } else if (newPin.equals(pin)) {
                    // Store the new PIN and reset flags
                    currentPin = newPin;
//                    newPin = "";
//                    isNewPinSetting = false;
                    savePreferences(); // Save the PIN and status
                    Toast.makeText(PasscodeActivity.this, "PIN changed successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(PasscodeActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(PasscodeActivity.this, "PINs do not match. Please try again.", Toast.LENGTH_SHORT).show();
                    newPin = "";
                    mPinLockView.resetPinLockView();
                }
            } else {
                // Verify current PIN logic
                if (pin.equals(currentPin)) {
                    // Correct current PIN, allow setting new PIN
                    isNewPinSetting = true;
                    mPinLockView.resetPinLockView();
                    Toast.makeText(PasscodeActivity.this, "Enter new PIN", Toast.LENGTH_SHORT).show();
                } else {
                    // Incorrect current PIN
                    mPinLockView.resetPinLockView();
                    // Display error message to the user
                }
            }
        }

        @Override
        public void onEmpty() {
            Log.d(TAG, "Pin empty");
        }

        @Override
        public void onPinChange(int pinLength, String intermediatePin) {
            Log.d(TAG, "Pin changed, new length " + pinLength + " with intermediate pin " + intermediatePin);
        }
    };

    // Save PIN and status to shared preferences
    private void savePreferences() {
        SharedPreferences.Editor editor = securePreferences.edit();
        editor.putString(KEY_CURRENT_PIN, currentPin);
        editor.putString(KEY_NEW_PIN, newPin);
        editor.putBoolean(KEY_IS_NEW_PIN_SETTING, isNewPinSetting);
        editor.apply();
    }
}
