package ru.pekcherkin.mytaxi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverRegLoginActivity extends AppCompatActivity {
    TextView statusDriver, signInQuestion;
    Button signApDriver, signInDriver;
    EditText driverPassword, driverEmail;
    FirebaseAuth mAuth;
    DatabaseReference driverDatabaseRef;
    String onlineDriverID;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_reg_login);

        statusDriver = findViewById(R.id.statusDriver);
        signInQuestion = findViewById(R.id.signInQuestion);
        signApDriver = findViewById(R.id.signApDriver);
        signInDriver = findViewById(R.id.signInDriver);
        driverPassword = findViewById(R.id.driverPassword);
        driverEmail = findViewById(R.id.driverEmail);
        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);


        signApDriver.setVisibility(View.INVISIBLE);
        signApDriver.setEnabled(false);

        signInQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInDriver.setVisibility(View.INVISIBLE);
                signApDriver.setVisibility(View.VISIBLE);
                signInQuestion.setVisibility(View.INVISIBLE);
                signApDriver.setEnabled(true);
                statusDriver.setText("Регистрация для водителей");
            }
        });

        signApDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = driverEmail.getText().toString();
                String password = driverPassword.getText().toString();

                registerDriver(email, password);
            }
        });

        signInDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = driverEmail.getText().toString();
                String password = driverPassword.getText().toString();
                signInDriver(email, password);
            }
        });
    }

    private void signInDriver(String email, String password) {
        progressDialog.setTitle("Вход водителя");
        progressDialog.setMessage("Пожалуйста дождитесь загрузки");
        progressDialog.show();
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(DriverRegLoginActivity.this, "Успешный вход", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();

                    Intent driverIntent = new Intent(DriverRegLoginActivity.this, DriversMapActivity.class);
                    startActivity(driverIntent);
                } else {
                    Toast.makeText(DriverRegLoginActivity.this, "Произошла ошибка попробуйте снова", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }
        });
    }

    private void registerDriver(String email, String password) {
        progressDialog.setTitle("Регистрация водителя");
        progressDialog.setMessage("Пожалуйста дождитесь загрузки");
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    onlineDriverID = mAuth.getCurrentUser().getUid();
                    driverDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(onlineDriverID);
                    driverDatabaseRef.setValue(true);

                    Intent driverIntent = new Intent(DriverRegLoginActivity.this, DriversMapActivity.class);
                    startActivity(driverIntent);

                    Toast.makeText(DriverRegLoginActivity.this, "Регистрация прошла успешно", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();

                } else {
                    Toast.makeText(DriverRegLoginActivity.this, "Ошибка", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }
        });
    }
}
