package com.devmiax.spamilagros;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


public class splash extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Opcional: mostrar el splash unos segundos (por estética)
        new Handler().postDelayed(this::checkUser, 1500); // 1.5 segundos
    }

    private void checkUser() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            // No hay sesión → ir al login
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
            finish();
            return;
        }

        // Hay usuario → obtener rol desde Firestore
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String rol = document.getString("rol");

                        Intent intent;
                        if ("admin".equalsIgnoreCase(rol)) {
                            intent = new Intent(this, Admin.class);
                        } else {
                            intent = new Intent(this, MainActivity.class);
                        }

                        startActivity(intent);
                        finish();

                    } else {
                        // Si no hay datos del usuario, ir al login
                        Toast.makeText(this, "Usuario sin datos registrados.", Toast.LENGTH_SHORT).show();
                        auth.signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al obtener rol: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    auth.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
    }


}