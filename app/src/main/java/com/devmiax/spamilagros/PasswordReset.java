package com.devmiax.spamilagros;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class PasswordReset extends AppCompatActivity {

    private TextInputEditText editEmailReset;
    private MaterialButton btnEnviarReset, btnVolverLogin;
    private LinearProgressIndicator progressReset;
    private TextView textStatusReset;

    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_password_reset);

        // Inicializar vistas
        editEmailReset = findViewById(R.id.editEmailReset);
        btnEnviarReset = findViewById(R.id.btnEnviarReset);
        btnVolverLogin = findViewById(R.id.btnVolverLogin);
        progressReset = findViewById(R.id.progressReset);
        textStatusReset = findViewById(R.id.textStatusReset);

        auth = FirebaseAuth.getInstance();

        btnEnviarReset.setOnClickListener(v -> enviarEnlaceReset());
        btnVolverLogin.setOnClickListener(v -> finish()); // vuelve al login
    }


    private void enviarEnlaceReset() {
        String email = editEmailReset.getText() != null ? editEmailReset.getText().toString().trim() : "";

        if (email.isEmpty()) {
            editEmailReset.setError("Ingresa tu correo electrónico");
            return;
        }

        progressReset.setVisibility(View.VISIBLE);
        btnEnviarReset.setEnabled(false);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressReset.setVisibility(View.GONE);
                    btnEnviarReset.setEnabled(true);

                    if (task.isSuccessful()) {
                        textStatusReset.setTextColor(getColor(android.R.color.holo_green_dark));
                        textStatusReset.setText("Te enviamos un enlace a tu correo para restablecer la contraseña.");
                        Toast.makeText(this, "Correo de restablecimiento enviado", Toast.LENGTH_LONG).show();
                    } else {
                        String errorCode = "";
                        Exception e = task.getException();

                        if (e instanceof FirebaseAuthException) {
                            errorCode = ((FirebaseAuthException) e).getErrorCode();
                        }

                        String mensaje = traducirErrorResetFirebase(errorCode);
                        textStatusReset.setTextColor(getColor(android.R.color.holo_red_dark));
                        textStatusReset.setText(mensaje);
                        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Método para traducir los códigos de error
    private String traducirErrorResetFirebase(String errorCode) {
        if (errorCode == null) return "Ocurrió un error desconocido.";

        switch (errorCode) {
            case "ERROR_INVALID_EMAIL":
                return "El formato del correo electrónico no es válido.";

            case "ERROR_USER_NOT_FOUND":
                return "No se encontró ninguna cuenta con este correo.";

            case "ERROR_USER_DISABLED":
                return "La cuenta ha sido deshabilitada por un administrador.";

            case "ERROR_NETWORK_REQUEST_FAILED":
                return "Error de red. Verifica tu conexión a Internet.";

            case "ERROR_TOO_MANY_REQUESTS":
                return "Demasiados intentos fallidos. Intenta más tarde.";

            case "ERROR_INTERNAL_ERROR":
                return "Error interno del servidor. Intenta nuevamente más tarde.";

            default:
                return "Error al enviar enlace: " + errorCode;
        }
    }
}