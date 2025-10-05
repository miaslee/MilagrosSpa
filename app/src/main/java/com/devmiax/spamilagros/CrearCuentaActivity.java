package com.devmiax.spamilagros;

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

import java.util.HashMap;
import java.util.Map;

public class CrearCuentaActivity extends AppCompatActivity {

    private EditText etName,etLastName, etPhone, etEmail, etPass, etPassConfirm;
    private Button btnRegister;
    private FirebaseAuth auth;
    private FirebaseFirestore db; // 👈 Firestore
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crear_cuenta);

    // Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Referencias UI
        etName = findViewById(R.id.Name);
        etLastName = findViewById(R.id.LastName);
        etPhone = findViewById(R.id.Phone);
        etEmail = findViewById(R.id.Email);
        etPass = findViewById(R.id.Password);
        etPassConfirm = findViewById(R.id.PasswordConfirm);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> doRegister());


    }

    private void doRegister() {
        String name = etName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass  = etPass.getText().toString().trim();
        String passConfirm  = etPassConfirm.getText().toString().trim();

        if(!pass.equals(passConfirm)){
            Toast.makeText(this, "Contraseña no coinciden", Toast.LENGTH_SHORT).show();
        }


        if (name.isEmpty() || lastName.isEmpty() || phone.isEmpty() || email.isEmpty() || pass.length() < 6) {
            Toast.makeText(this, "Completa todos los campos (contraseña mínimo 6 caracteres)", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);

        // Crear usuario en Firebase Authentication
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    btnRegister.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser u = auth.getCurrentUser();

                        if (u != null) {
                            // Guardar datos adicionales en Firestore
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("uid", u.getUid());
                            userData.put("nombre", name);
                            userData.put("lastName", lastName);
                            userData.put("telefono", phone);
                            userData.put("correo", email);
                            userData.put("rol", "cliente");

                            db.collection("users")
                                    .document(u.getUid()) // documento con el mismo UID del usuario
                                    .set(userData)
                                    .addOnSuccessListener(aVoid ->
                                            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                    )
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Error guardando datos: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                    );
                        }

                        // Ir al Home o MainActivity
                        Intent i = new Intent(this, MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    } else {
                        String msg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Error al registrar usuario";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }


}