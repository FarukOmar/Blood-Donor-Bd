package com.example.blooddonorbd.Activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blooddonorbd.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.concurrent.TimeUnit;

public class PhoneNumberConfirmationActivity extends AppCompatActivity {
    EditText codeEt;
    private String verificationId;
    ProgressDialog progressBar;
    ProgressDialog progressDialog;
    TextView phoneNumberTv,detailsPhnNumTv;
    FirebaseAuth auth;
    ConstraintLayout constraintLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number_confirmation);

        auth = FirebaseAuth.getInstance();
        codeEt = findViewById(R.id.codeEt);
        constraintLayout = findViewById(R.id.phoneNumberConfActivity);

        phoneNumberTv = findViewById(R.id.textView3);
        detailsPhnNumTv = findViewById(R.id.textView4);

        progressDialog = new ProgressDialog(this,R.style.AppCompatAlertDialogStyle);
        progressDialog.setMessage("Please wait");

        String phoneNumber = getIntent().getStringExtra("phoneNumber");
        phoneNumberTv.setText("Veriy "+phoneNumber);
        detailsPhnNumTv.setText("Waiting to detect an verification code from "+phoneNumber+" phone number.");
        sendVerificationCode(phoneNumber);

    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallback = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
            String code = phoneAuthCredential.getSmsCode();//getting sms code
            if (code != null){
                codeEt.setText(code);
                verifyVerificationCode(code);
                progressBar.dismiss();
            }else {
                signInWithPhoneAuthCredential(phoneAuthCredential);
                //progressBar.dismiss();
            }
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            Toast.makeText(PhoneNumberConfirmationActivity.this, "Verification  try again", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(PhoneNumberConfirmationActivity.this,VerifyPhoneNumberActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
            startActivity(intent);
            //Log.d("exception",e.toString());
            progressBar.dismiss();
        }

        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            verificationId = s;
            progressBar.dismiss();
        }
    };
    public void sendVerificationCode(String phoneNumber){
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                mCallback
        );
        progressBar = new ProgressDialog(this,R.style.AppCompatAlertDialogStyle);
        progressBar.setMessage("Sending verification code");

        progressBar.show();
    }

    private void verifyVerificationCode(String code){
        //creating credential
        PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(verificationId,code);
        signInWithPhoneAuthCredential(phoneAuthCredential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential phoneAuthCredential) {
        auth.signInWithCredential(phoneAuthCredential)
                .addOnCompleteListener(PhoneNumberConfirmationActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull final Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("User").child(task.getResult().getUser().getPhoneNumber());
                            //reading database value
                            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    int count = (int) dataSnapshot.getChildrenCount();
                                    if (count>=8 && !isLocationEnabled() && constraintLayout.getVisibility() == View.VISIBLE){//if database value count > than 1 then this condition will be work
                                        Intent intent = new Intent(PhoneNumberConfirmationActivity.this,DiscoverableActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        progressDialog.dismiss();
                                        finish();
                                        startActivity(intent);
                                    }else if (count>=8 && isLocationEnabled() && constraintLayout.getVisibility() == View.VISIBLE){
                                        Intent intent = new Intent(PhoneNumberConfirmationActivity.this,HomeActivity.class);
                                        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                                            @Override
                                            public void onSuccess(InstanceIdResult instanceIdResult) {
                                                databaseReference.child("Token id").setValue(instanceIdResult.getToken());
                                            }
                                        });
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        finish();
                                        progressDialog.dismiss();
                                        startActivity(intent);
                                    }
                                    else{
                                        Intent intent = new Intent(PhoneNumberConfirmationActivity.this, SetupProfileActivity.class);
                                        databaseReference.child("User id").setValue(task.getResult().getUser().getUid());
                                        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                                            @Override
                                            public void onSuccess(InstanceIdResult instanceIdResult) {
                                                databaseReference.child("Token id").setValue(instanceIdResult.getToken());
                                            }
                                        });
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        finish();
                                        progressDialog.dismiss();
                                        startActivity(intent);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                            Toast.makeText(PhoneNumberConfirmationActivity.this, "Phone number confirmation successful", Toast.LENGTH_SHORT).show();
                        }else {
                            task.getException();
                        }
                    }
                });
        progressDialog.show();
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    public void nextBtOnPhoneVerifyActivity(View view) {
        String code = codeEt.getText().toString();
        verifyVerificationCode(code);
    }

}
