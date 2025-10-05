package com.devmiax.spamilagros;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class misCitas extends AppCompatActivity {

    private void backOnToolbar(int toolbarId) {
        MaterialToolbar tb = findViewById(toolbarId);
        if (tb != null) tb.setNavigationOnClickListener(v -> finish());
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_citas);

        backOnToolbar(R.id.toolbarMyAppointments);
        // La navegación a Reprogramar/Cancelar se suele hacer desde el adapter del RecyclerView.
    }
}
