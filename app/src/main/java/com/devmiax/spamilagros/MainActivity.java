package com.devmiax.spamilagros;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class MainActivity extends AppCompatActivity {

    private void nav(int viewId, Class<?> target) {
        View v = findViewById(viewId);
        if (v != null) v.setOnClickListener(x -> startActivity(new Intent(this, target)));
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        nav(R.id.btnGoToAgenda,         AgendaActivity.class);
        nav(R.id.btnGoToMyAppointments, misCitas.class);
        nav(R.id.btnGoToServicios,      servicios.class);
        nav(R.id.btnGoToPerfil,         PerfilActivity.class);
        nav(R.id.btnAgendarDesdeBanner, AgendaActivity.class); // banner opcional
    }
}
