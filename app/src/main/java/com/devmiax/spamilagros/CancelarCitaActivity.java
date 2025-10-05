package com.devmiax.spamilagros;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class CancelarCitaActivity extends AppCompatActivity {

    private void nav(int viewId, Class<?> target) {
        View v = findViewById(viewId);
        if (v != null) v.setOnClickListener(x -> startActivity(new Intent(this, target)));
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancelar_cita);

        // Confirmar cancelación → Mis citas (o Main)
        nav(R.id.btnConfirmarCancelacion, misCitas.class);
        // Cancelar (volver) → finish
        //View btnVolver = findViewById(R.id.btnVolver);
       // if (btnVolver != null) btnVolver.setOnClickListener(v -> finish());
    }
}
