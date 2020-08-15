package com.ug.schooldroid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OTPLoginScreen extends AppCompatActivity implements View.OnFocusChangeListener, TextWatcher {
    private Handler mHandler = new Handler();
    private boolean ResendOtpFlag = false;

    private EditText otp_input_1, otp_input_2, otp_input_3,
            otp_input_4, otp_input_5, otp_input_6;

    private ConstraintLayout OTPConstraintLayout;
    private TextView OtpSentSms;

    private CardView layoutOne, layoutTwo;
    private String OTPMessage;
    private FirebaseAuth mAuth;
    private Button otpSubmitButton;
    private String CodeSent;
    private String UserPhoneNumber;
    private ProgressBar otpProgressBar;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_login_screen);

        mHandler.postDelayed(mRunnable, 30000);
        mAuth = FirebaseAuth.getInstance();

        CodeSent = getIntent().getStringExtra("CodeSent");
        UserPhoneNumber = getIntent().getStringExtra("PhoneNumber");

        OTPConstraintLayout = findViewById(R.id.OTPConstrainLayout);
        otp_input_1 = findViewById(R.id.otp_number_1);
        otp_input_2 = findViewById(R.id.otp_number_2);
        otp_input_3 = findViewById(R.id.otp_number_3);
        otp_input_4 = findViewById(R.id.otp_number_4);
        otp_input_5 = findViewById(R.id.otp_number_5);
        otp_input_6 = findViewById(R.id.otp_number_6);
        otpProgressBar = findViewById(R.id.otp_progressBar);
        OtpSentSms = findViewById(R.id.otp_sent_sms);

        otp_input_1.requestFocus();
        isFocusAvailable(otp_input_1, true);

        String sms = getResources().getString(R.string.otp_sent_sms) + UserPhoneNumber;
        OtpSentSms.setText(sms);

        layoutOne = findViewById(R.id.layoutOne);
        layoutTwo = findViewById(R.id.layoutTwo);

        layoutOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard(v);
                if (ResendOtpFlag) {
                    otpProgressBar.setVisibility(View.VISIBLE);
                    otpSubmitButton.setVisibility(View.INVISIBLE);
                    sendVerificationCode();
                } else {
                    Snackbar.make(OTPConstraintLayout, "Try again in 60 Seconds", Snackbar.LENGTH_LONG)
                            .setBackgroundTint(getResources().getColor(R.color.white))
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setTextColor(getResources().getColor(R.color.black))
                            .show();
                }
            }
        });

        layoutTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard(v);
                startActivity(new Intent(OTPLoginScreen.this, MainActivity.class)
                        .putExtra("PhoneNumber", UserPhoneNumber)
                        .putExtra("edit", true));
                finish();
            }
        });

        otpSubmitButton = findViewById(R.id.otp_submit_button);
        otpSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startHoverAnimation(v);
                hideSoftKeyboard(v);

                if (checkAllInputs()) {
                    if (isNetworkAvailable()) {
                        verifySignCode();
                    } else {
                        Snackbar.make(OTPConstraintLayout, "Check Your Internet", Snackbar.LENGTH_LONG)
                                .setBackgroundTint(getResources().getColor(R.color.white))
                                .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                                .setTextColor(getResources().getColor(R.color.black))
                                .show();
                    }
                } else {
                    Snackbar.make(OTPConstraintLayout, "Please enter OTP code", Snackbar.LENGTH_LONG)
                            .setBackgroundTint(getResources().getColor(R.color.white))
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setTextColor(getResources().getColor(R.color.black))
                            .show();
                }
                endHoverAnimation(v);
            }
        });

        otpSubmitButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (v == otpSubmitButton) {
                        startHoverAnimation(v);
                    }

                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (v == otpSubmitButton) {
                        endHoverAnimation(v);
                    }
                }

                return false;
            }
        });

        otp_input_6.setOnFocusChangeListener(this);
        otp_input_1.setOnFocusChangeListener(this);
        otp_input_2.setOnFocusChangeListener(this);
        otp_input_3.setOnFocusChangeListener(this);
        otp_input_4.setOnFocusChangeListener(this);
        otp_input_5.setOnFocusChangeListener(this);

        otp_input_6.addTextChangedListener(this);
        otp_input_1.addTextChangedListener(this);
        otp_input_2.addTextChangedListener(this);
        otp_input_3.addTextChangedListener(this);
        otp_input_4.addTextChangedListener(this);
        otp_input_5.addTextChangedListener(this);
    }

    private void defaultInputBoxColor() {
        otp_input_1.setTextColor(getResources().getColor(R.color.inputTextColor));
        otp_input_2.setTextColor(getResources().getColor(R.color.inputTextColor));
        otp_input_3.setTextColor(getResources().getColor(R.color.inputTextColor));
        otp_input_4.setTextColor(getResources().getColor(R.color.inputTextColor));
        otp_input_5.setTextColor(getResources().getColor(R.color.inputTextColor));
        otp_input_6.setTextColor(getResources().getColor(R.color.inputTextColor));
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            ResendOtpFlag = true;
        }
    };

    private Runnable mRunnableWrongOtp = new Runnable() {
        @Override
        public void run() {
            defaultInputBoxColor();
        }
    };

    private void sendVerificationCode() {
        String phoneNum = "+91" + UserPhoneNumber;

        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.
                OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                Toast.makeText(OTPLoginScreen.this, "Complete", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
                Toast.makeText(OTPLoginScreen.this, "okay"+s, Toast.LENGTH_LONG).show();
                super.onCodeAutoRetrievalTimeOut(s);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                otpProgressBar.setVisibility(View.GONE);
                otpSubmitButton.setVisibility(View.VISIBLE);
                Toast.makeText(OTPLoginScreen.this, ""+e.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.
                    ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);

                CodeSent = s;
                otpProgressBar.setVisibility(View.GONE);
                otpSubmitButton.setVisibility(View.VISIBLE);
                ResendOtpFlag = false;
                mHandler.postDelayed(mRunnable, 60000);
            }
        };

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNum,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
    }

    private void verifySignCode() {
        otpSubmitButton.setVisibility(View.INVISIBLE);
        otpProgressBar.setVisibility(View.VISIBLE);
        String userCode = OTPMessage;

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(CodeSent, userCode);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(OTPLoginScreen.this, "Logged In.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(OTPLoginScreen.this, "Login Failure",
                                    Toast.LENGTH_SHORT).show();
                            changeInputBoxColor();
                            mHandler.postDelayed(mRunnableWrongOtp, 3000);
                        }
                        otpSubmitButton.setVisibility(View.VISIBLE);
                        otpProgressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void changeInputBoxColor() {
        otp_input_1.setTextColor(getResources().getColor(R.color.red));
        otp_input_2.setTextColor(getResources().getColor(R.color.red));
        otp_input_3.setTextColor(getResources().getColor(R.color.red));
        otp_input_4.setTextColor(getResources().getColor(R.color.red));
        otp_input_5.setTextColor(getResources().getColor(R.color.red));
        otp_input_6.setTextColor(getResources().getColor(R.color.red));
    }

    private boolean checkAllInputs() {
        String msg = "";

        if (!TextUtils.isEmpty(otp_input_1.getText().toString().trim())) {
            msg = msg + otp_input_1.getText().toString().trim();

            if (!TextUtils.isEmpty(otp_input_2.getText().toString().trim())) {
                msg = msg + otp_input_2.getText().toString().trim();

                if (!TextUtils.isEmpty(otp_input_3.getText().toString().trim())) {
                    msg = msg + otp_input_3.getText().toString().trim();

                    if (!TextUtils.isEmpty(otp_input_4.getText().toString().trim())) {
                        msg = msg + otp_input_4.getText().toString().trim();

                        if (!TextUtils.isEmpty(otp_input_5.getText().toString().trim())) {
                            msg = msg + otp_input_5.getText().toString().trim();

                            if (!TextUtils.isEmpty(otp_input_6.getText().toString().trim())) {
                                msg = msg + otp_input_6.getText().toString().trim();

                                OTPMessage = msg;
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
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

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v == otp_input_1) {
            isFocusAvailable(otp_input_1, hasFocus);
        } else if (v == otp_input_2) {
            isFocusAvailable(otp_input_2, hasFocus);
        } else if (v == otp_input_3) {
            isFocusAvailable(otp_input_3, hasFocus);
        } else if (v == otp_input_4) {
            isFocusAvailable(otp_input_4, hasFocus);
        } else if (v == otp_input_5) {
            isFocusAvailable(otp_input_5, hasFocus);
        } else if (v == otp_input_6) {
            isFocusAvailable(otp_input_6, hasFocus);
        }
    }

    private void isFocusAvailable(EditText editText, boolean hasFocus) {
        if (hasFocus) {
            editText.setBackgroundTintList(ColorStateList.valueOf(
                    getResources().getColor(R.color.gray)));
        } else {
            editText.setBackgroundTintList(ColorStateList.valueOf(
                    getResources().getColor(R.color.white)));
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void hideSoftKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_DEL == keyCode) {
            if (otp_input_6.isFocused()) {
                otp_input_5.requestFocus();
            } else if (otp_input_5.isFocused()) {
                otp_input_4.requestFocus();
            } else if (otp_input_4.isFocused()) {
                otp_input_3.requestFocus();
            } else if (otp_input_3.isFocused()) {
                otp_input_2.requestFocus();
            } else if (otp_input_2.isFocused()) {
                otp_input_1.requestFocus();
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (checkAllInputs()) {
            if (otp_input_6.isFocused()) {
                if (isNetworkAvailable()) {
                    verifySignCode();
                } else {
                    Snackbar.make(OTPConstraintLayout, "Check Your Internet", Snackbar.LENGTH_LONG)
                            .setBackgroundTint(getResources().getColor(R.color.white))
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setTextColor(getResources().getColor(R.color.black))
                            .show();
                }
            }
        }

        if (!TextUtils.isEmpty(s.toString().trim())) {
            if (otp_input_1.isFocused()) {
                otp_input_2.requestFocus();
            } else if (otp_input_2.isFocused()) {
                otp_input_3.requestFocus();
            } else if (otp_input_3.isFocused()) {
                otp_input_4.requestFocus();
            } else if (otp_input_4.isFocused()) {
                otp_input_5.requestFocus();
            } else if (otp_input_5.isFocused()) {
                otp_input_6.requestFocus();
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) { }
}