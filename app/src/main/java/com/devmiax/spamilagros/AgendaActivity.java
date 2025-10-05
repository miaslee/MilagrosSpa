package com.devmiax.spamilagros;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class AgendaActivity extends AppCompatActivity {

    private void nav(int viewId, Class<?> target) {
        View v = findViewById(viewId);
        if (v != null) v.setOnClickListener(x -> startActivity(new Intent(this, target)));
    }
    private void backOnToolbar(int toolbarId) {
        MaterialToolbar tb = findViewById(toolbarId);
        if (tb != null) tb.setNavigationOnClickListener(v -> finish());
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda);

        backOnToolbar(R.id.toolbarAgenda);
        // Confirmar cita → Confirmación
        nav(R.id.btnConfirmarCita, confirmacionCita.class);
    }
}
