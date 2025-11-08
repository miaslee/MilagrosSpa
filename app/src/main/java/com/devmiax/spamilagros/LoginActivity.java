package com.devmiax.spamilagros;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

public class LoginActivity extends AppCompatActivity {
    private EditText Email, Pass;
    private Button btnLogin, btnRegister;
    private FirebaseAuth auth;
    private View passReset;

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
        passReset = findViewById(R.id.passReset);

        btnLogin.setOnClickListener(v -> doLogin());
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, CrearCuentaActivity.class))
        );
        passReset.setOnClickListener(v ->
                startActivity(new Intent(this, PasswordReset.class))
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
                                                    intent = new Intent(this, HomeActivity.class);
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
                        String errorCode = "";
                        Exception e = task.getException();

                        if (e instanceof FirebaseAuthException) {
                            errorCode = ((FirebaseAuthException) e).getErrorCode();
                        }

                        String mensaje = traducirErrorFirebase(errorCode);
                        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();

                    }
                });
    }

    private String traducirErrorFirebase(String errorCode) {
        if (errorCode == null) return "Ocurrió un error desconocido.";

        switch (errorCode) {
            case "ERROR_INVALID_CUSTOM_TOKEN":
                return "El token personalizado tiene un formato incorrecto o no es válido.";

            case "ERROR_CUSTOM_TOKEN_MISMATCH":
                return "El token personalizado corresponde a una audiencia diferente.";

            case "ERROR_INVALID_CREDENTIAL":
                return "Las credenciales proporcionadas no son válidas o han expirado.";

            case "ERROR_INVALID_EMAIL":
                return "El formato del correo electrónico no es válido.";

            case "ERROR_WRONG_PASSWORD":
                return "La contraseña es incorrecta. Por favor inténtalo de nuevo.";

            case "ERROR_USER_MISMATCH":
                return "Las credenciales no coinciden con el usuario autenticado.";

            case "ERROR_REQUIRES_RECENT_LOGIN":
                return "Esta operación requiere que inicies sesión nuevamente.";

            case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
                return "Ya existe una cuenta con este correo, pero usa un método de acceso diferente.";

            case "ERROR_EMAIL_ALREADY_IN_USE":
                return "El correo electrónico ya está en uso por otra cuenta.";

            case "ERROR_CREDENTIAL_ALREADY_IN_USE":
                return "Estas credenciales ya están asociadas a otra cuenta de usuario.";

            case "ERROR_USER_DISABLED":
                return "La cuenta ha sido deshabilitada por un administrador.";

            case "ERROR_USER_TOKEN_EXPIRED":
                return "La sesión del usuario ha expirado. Vuelve a iniciar sesión.";

            case "ERROR_USER_NOT_FOUND":
                return "No existe una cuenta registrada con este correo.";

            case "ERROR_INVALID_USER_TOKEN":
                return "La sesión del usuario es inválida. Inicia sesión nuevamente.";

            case "ERROR_OPERATION_NOT_ALLOWED":
                return "Este método de inicio de sesión no está habilitado. Contacta con el administrador.";

            case "ERROR_WEAK_PASSWORD":
                return "La contraseña es demasiado débil. Debe tener al menos 6 caracteres.";

            case "ERROR_TOO_MANY_REQUESTS":
                return "Demasiados intentos fallidos. Intenta más tarde.";

            case "ERROR_NETWORK_REQUEST_FAILED":
                return "No se pudo conectar con el servidor. Verifica tu conexión a Internet.";

            case "ERROR_INTERNAL_ERROR":
                return "Error interno del servidor. Intenta nuevamente más tarde.";

            default:
                return "Error no identificado: " + errorCode;
        }
    }





}