package ru.pekcherkin.mytaxi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerRegLoginActivity extends AppCompatActivity {

    TextView statusCustomer, signInQuestion;
    Button signApBtn, signInBtn;
    EditText driverPassword, driverEmail;
    FirebaseAuth mAuth;
    ProgressDialog progressDialog;
    DatabaseReference customerDatabaseRef;
    String onlineCustomerID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_reg_login);

        statusCustomer = findViewById(R.id.statusCustomer);
        signInQuestion = findViewById(R.id.signInQuestionCustomer);
        signApBtn = findViewById(R.id.signApCustomer);
        signInBtn = findViewById(R.id.signInCustomer);
        driverPassword = findViewById(R.id.customerPassword);
        driverEmail = findViewById(R.id.customerEmail);

        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);

        signApBtn.setVisibility(View.INVISIBLE);
        signApBtn.setEnabled(false);

        signInQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInBtn.setVisibility(View.INVISIBLE);
                signApBtn.setVisibility(View.VISIBLE);
                signInQuestion.setVisibility(View.INVISIBLE);
                signApBtn.setEnabled(true);
                statusCustomer.setText("Регистрация для клиентов");
            }
        });

        signApBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = driverEmail.getText().toString();
                String password = driverPassword.getText().toString();

                registerCustomer(email, password);
            }
        });

        signInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = driverEmail.getText().toString();
                String password = driverPassword.getText().toString();
                signInCustomer(email, password);
            }
        });
    }

    private void signInCustomer(String email, String password) {
        progressDialog.setTitle("Вход клиента");
        progressDialog.setMessage("Пожалуйста дождитесь загрузки");
        progressDialog.show();
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(CustomerRegLoginActivity.this, "Успешный вход", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    Intent customerIntent = new Intent(CustomerRegLoginActivity.this, CustomersMapActivity.class);
                    startActivity(customerIntent);
                } else {
                    Toast.makeText(CustomerRegLoginActivity.this, "Произошла ошибка попробуйте снова", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }
        });
    }

    private void registerCustomer(String email, String password) {
        progressDialog.setTitle("Регистрация клиента");
        progressDialog.setMessage("Пожалуйста дождитесь загрузки");
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    onlineCustomerID = mAuth.getCurrentUser().getUid();
                    customerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(onlineCustomerID);
                    customerDatabaseRef.setValue(true);

                    Intent customerIntent = new Intent(CustomerRegLoginActivity.this, CustomersMapActivity.class);
                    startActivity(customerIntent);

                    Toast.makeText(CustomerRegLoginActivity.this, "Регистрация прошла успешно", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                } else {
                    Toast.makeText(CustomerRegLoginActivity.this, "Ошибка", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }
        });
    }
}
