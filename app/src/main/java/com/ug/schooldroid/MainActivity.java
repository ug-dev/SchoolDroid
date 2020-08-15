package com.ug.schooldroid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ImageView mainImage, headerLogo;
    private LinearLayout phoneNumberInput;
    private Button phoneNumberNextButton;
    private EditText phoneInput;
    private TextView titleText1, titleText2;
    private ProgressBar loginProgressBar;
    private ConstraintLayout LoginConstraintLayout;

    private float actualHeight;
    private long backPressedTime;
    private Toast backToast;

    private String CodeSent;
    private String UserPhoneNumber;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LoginConstraintLayout = findViewById(R.id.LoginConstrainLayout);
        mainImage = findViewById(R.id.main_image);
        headerLogo = findViewById(R.id.header_logo);
        phoneNumberInput = findViewById(R.id.phone_number_input);
        phoneNumberNextButton = findViewById(R.id.phone_number_next_button);
        phoneInput = findViewById(R.id.phoneInputBox);
        titleText1 = findViewById(R.id.main_title_text_1);
        titleText2 = findViewById(R.id.main_title_text_2);
        loginProgressBar = findViewById(R.id.login_progressBar);

        phoneInput.requestFocus();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float height = displayMetrics.heightPixels;
        actualHeight = (float) (height / 2.5);

        phoneNumberInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAnimation();
                showSoftKeyboard(phoneInput);
            }
        });

        phoneInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAnimation();
            }
        });

        phoneInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                endAnimation();
                return false;
            }
        });

        phoneNumberNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endAnimation();
                hideSoftKeyboard(v);
                startHoverAnimation(v);
                endHoverAnimation(v);

                //Sending SMS...
                if (TextUtils.isEmpty(phoneInput.getText().toString().trim())) {
                    Snackbar.make(LoginConstraintLayout, "Please enter your phone number", Snackbar.LENGTH_LONG)
                            .setBackgroundTint(getResources().getColor(R.color.white))
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setTextColor(getResources().getColor(R.color.black))
                            .show();
                } else {
                    if (phoneInput.getText().toString().trim().length() >= 10) {
                        if (isNetworkAvailable()) {
                            phoneNumberNextButton.setVisibility(View.GONE);
                            loginProgressBar.setVisibility(View.VISIBLE);
                            sendVerificationCode();
                        } else {
                            Snackbar.make(LoginConstraintLayout, "Check Your Internet", Snackbar.LENGTH_LONG)
                                    .setBackgroundTint(getResources().getColor(R.color.white))
                                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                                    .setTextColor(getResources().getColor(R.color.black))
                                    .show();
                        }
                    } else {
                        Snackbar.make(LoginConstraintLayout, "Check your phone number", Snackbar.LENGTH_LONG)
                                .setBackgroundTint(getResources().getColor(R.color.white))
                                .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                                .setTextColor(getResources().getColor(R.color.black))
                                .show();
                    }
                }
            }
        });

        phoneNumberNextButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startHoverAnimation(v);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    endHoverAnimation(v);
                }
                return false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getIntent().getBooleanExtra("edit", false)) {
            String msg = getIntent().getStringExtra("PhoneNumber");
            phoneInput.setText(msg);
        }
    }

    private void sendVerificationCode() {
        UserPhoneNumber = phoneInput.getText().toString().trim();
        String phoneNum = "+91" + phoneInput.getText().toString().trim();

        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.
                OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) { }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                phoneNumberNextButton.setVisibility(View.VISIBLE);
                loginProgressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, ""+e.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.
                    ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);

                CodeSent = s;
                phoneNumberNextButton.setVisibility(View.VISIBLE);
                loginProgressBar.setVisibility(View.GONE);

                startActivity(new Intent(MainActivity.this, OTPLoginScreen.class)
                        .putExtra("PhoneNumber", UserPhoneNumber)
                        .putExtra("CodeSent", CodeSent));
            }
        };

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNum,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
    }

    private void endHoverAnimation(View v) {
        ObjectAnimator animLogo = ObjectAnimator
                .ofFloat(v, "scaleY", 1f);

        ObjectAnimator animLogo2= ObjectAnimator
                .ofFloat(v, "scaleX", 1f);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(animLogo, animLogo2);
        animSet.setDuration(100);
        animSet.start();
    }

    private void startHoverAnimation(View v) {
        ObjectAnimator animLogo = ObjectAnimator
                .ofFloat(v, "scaleY", 0.95f);

        ObjectAnimator animLogo2= ObjectAnimator
                .ofFloat(v, "scaleX", 0.95f);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(animLogo, animLogo2);
        animSet.setDuration(100);
        animSet.start();
    }

    private void hideSoftKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void showSoftKeyboard(View v) {
        if (v.requestFocus()) {
            InputMethodManager imm =(InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(v,InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void endAnimation() {
        ObjectAnimator animMain = ObjectAnimator
                .ofFloat(mainImage, "translationY", 0f)
                .setDuration(300);

        ObjectAnimator animText1 = ObjectAnimator
                .ofFloat(titleText1, "translationY", 0f)
                .setDuration(300);

        ObjectAnimator animText2 = ObjectAnimator
                .ofFloat(titleText2, "translationY", 0f)
                .setDuration(300);

        ObjectAnimator animCard = ObjectAnimator
                .ofFloat(phoneNumberInput, "translationY", 0f)
                .setDuration(300);

        ObjectAnimator animButton = ObjectAnimator
                .ofFloat(phoneNumberNextButton, "translationY", 0f)
                .setDuration(300);

        ObjectAnimator animLogo = ObjectAnimator
                .ofFloat(headerLogo, "scaleY", 1f)
                .setDuration(400);

        ObjectAnimator animLogo2 = ObjectAnimator
                .ofFloat(headerLogo, "scaleX", 1f)
                .setDuration(400);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(animMain, animLogo, animLogo2, animText1, animText2,
                animCard, animButton);
        animSet.start();
    }

    private void startAnimation() {
        ObjectAnimator animMain = ObjectAnimator
                .ofFloat(mainImage, "translationY", -actualHeight)
                .setDuration(300);

        ObjectAnimator animText1 = ObjectAnimator
                .ofFloat(titleText1, "translationY", -actualHeight)
                .setDuration(300);

        ObjectAnimator animCard = ObjectAnimator
                .ofFloat(phoneNumberInput, "translationY", -actualHeight)
                .setDuration(300);

        ObjectAnimator animButton = ObjectAnimator
                .ofFloat(phoneNumberNextButton, "translationY", -actualHeight)
                .setDuration(300);

        ObjectAnimator animText2 = ObjectAnimator
                .ofFloat(titleText2, "translationY", -actualHeight)
                .setDuration(300);

        ObjectAnimator animLogo = ObjectAnimator
                .ofFloat(headerLogo, "scaleY", 0f)
                .setDuration(200);

        ObjectAnimator animLogo2 = ObjectAnimator
                .ofFloat(headerLogo, "scaleX", 0f)
                .setDuration(200);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(animMain, animLogo, animLogo2, animText1, animText2,
                animCard, animButton);
        animSet.start();
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel();
            super.onBackPressed();
            return;
        } else {
            backToast = Toast.makeText(this,
                    "Press back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
        }

        backPressedTime = System.currentTimeMillis();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}