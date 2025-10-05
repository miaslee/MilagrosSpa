package com.devmiax.spamilagros;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class confirmacionCita extends AppCompatActivity {

    private void nav(int viewId, Class<?> target) {
        View v = findViewById(viewId);
        if (v != null) v.setOnClickListener(x -> startActivity(new Intent(this, target)));
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmacion_cita);

        // Ver mis citas
        nav(R.id.btnOpenMyAppointments, misCitas.class);
        // Añadir a Google Calendar (solo navegación por ahora)
        nav(R.id.btnAddToCalendar, MainActivity.class);
    }
}
