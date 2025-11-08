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
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CrearCuentaActivity extends AppCompatActivity {

    private EditText etName, etPhone, etEmail, etPass, etPassConfirm;
    private Button btnRegister,btnGoToLogin;
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

        etPhone = findViewById(R.id.Phone);
        etEmail = findViewById(R.id.Email);
        etPass = findViewById(R.id.Password);
        etPassConfirm = findViewById(R.id.PasswordConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoToLogin= findViewById(R.id.btnGoToLogin);


        btnRegister.setOnClickListener(v -> doRegister());
        btnGoToLogin.setOnClickListener(v -> finish()); // vuelve al login

    }



    private void doRegister() {
        String name = etName.getText().toString().trim();

        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass  = etPass.getText().toString().trim();
        String passConfirm  = etPassConfirm.getText().toString().trim();

        if(!pass.equals(passConfirm)){
            Toast.makeText(this, "Contraseña no coinciden", Toast.LENGTH_SHORT).show();
        }


        if (name.isEmpty()  || phone.isEmpty() || email.isEmpty() || pass.length() < 6) {
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
                        Intent i = new Intent(this, HomeActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    } else {
                        String errorCode = "";
                        Exception e = task.getException();

                        if (e instanceof FirebaseAuthException) {
                            errorCode = ((FirebaseAuthException) e).getErrorCode();
                        }

                        String mensaje = traducirErrorRegistroFirebase(errorCode);
                        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
                    }
                });
    }


    private String traducirErrorRegistroFirebase(String errorCode) {
        if (errorCode == null) return "Ocurrió un error desconocido.";

        switch (errorCode) {
            case "ERROR_INVALID_EMAIL":
                return "El formato del correo electrónico no es válido.";

            case "ERROR_EMAIL_ALREADY_IN_USE":
                return "El correo electrónico ya está registrado. Usa otro o inicia sesión.";

            case "ERROR_WEAK_PASSWORD":
                return "La contraseña es demasiado débil. Debe tener al menos 6 caracteres.";

            case "ERROR_OPERATION_NOT_ALLOWED":
                return "El método de registro con correo y contraseña no está habilitado.";

            case "ERROR_NETWORK_REQUEST_FAILED":
                return "Error de red. Verifica tu conexión a Internet.";

            case "ERROR_INVALID_CREDENTIAL":
                return "Las credenciales proporcionadas no son válidas o han expirado.";

            case "ERROR_TOO_MANY_REQUESTS":
                return "Demasiados intentos fallidos. Intenta más tarde.";

            case "ERROR_INTERNAL_ERROR":
                return "Error interno del servidor. Intenta nuevamente más tarde.";

            case "ERROR_USER_DISABLED":
                return "La cuenta ha sido deshabilitada por un administrador.";

            default:
                return "Error al registrar usuario: " + errorCode;
        }
    }


}