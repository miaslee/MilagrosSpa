package com.devmiax.spamilagros;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private EditText Email, Pass;
    private Button btnLogin, btnRegister;
    private FirebaseAuth auth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        //enlazar view
        auth = FirebaseAuth.getInstance();
        Email = findViewById(R.id.mail);
        Pass  = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> doLogin());
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, CrearCuentaActivity.class))
        );
    }

    private void doLogin() {
        String email = Email.getText().toString().trim();
        String pass  = Pass.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Completa email y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Usuario autenticado
                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {
                            // Obtener el documento del usuario en Firestore
                            FirebaseFirestore db = FirebaseFirestore.getInstance();

                            db.collection("users")
                                    .document(user.getUid())
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String rol = documentSnapshot.getString("rol");

                                            if (rol != null) {
                                                Intent intent;
                                                if (rol.equalsIgnoreCase("admin")) {
                                                    intent = new Intent(this, Admin.class);
                                                } else {
                                                    intent = new Intent(this, MainActivity.class);
                                                }

                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                            } else {
                                                Toast.makeText(this, "El campo 'rol' no está definido.", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(this, "No se encontraron datos del usuario.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Error al obtener datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }

                    } else {
                        // Falló la autenticación
                        String msg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Error al iniciar sesión";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

}