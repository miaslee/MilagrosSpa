package com.devmiax.spamilagros;

import android.content.Intent;
import android.os.Bundle;
import android.widget.AutoCompleteTextView;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.RecyclerView; // por si luego agregas lista

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Agenda: genera y muestra horarios disponibles (versión con tipos explícitos). */
public class AgendaActivity extends AppCompatActivity {

    // UI (coinciden con activity_agenda.xml)
    private MaterialToolbar toolbarAgenda;
    private SwipeRefreshLayout swipeRefresh;
    private CalendarView calendarView;
    private ChipGroup chipGroupSlots;
    private LinearProgressIndicator progressSlots;
    private MaterialTextView tvFechaSeleccionada;
    private MaterialTextView tvLastUpdate;
    private MaterialTextView tvEstadoAgenda;
    private AutoCompleteTextView actvServicio;
    private AutoCompleteTextView actvProfesional;
    private MaterialButton btnConfirmarCita;

    // Datos del servicio (llegan por Intent)
    private String servicioId, servicioNombre;
    private int duracionMin;
    private double precio;

    // Selección actual
    private String fechaSeleccionada; // "YYYY-MM-DD"
    private String horaSeleccionada;  // "HH:mm"
    private String staffIdSeleccionado; // opcional
    private String staffNombreSeleccionado;

    // Utils
    private final SimpleDateFormat sdfReq  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat sdfShow = new SimpleDateFormat("EEE d MMM", new Locale("es", "PE"));
    private final SimpleDateFormat sdfHour = new SimpleDateFormat("HH:mm", Locale.getDefault());

    // Firebase
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda); // usa tu XML existente

        // --- bind UI (TIPOS explícitos) ---
        toolbarAgenda      = findViewById(R.id.toolbarAgenda);
        swipeRefresh       = findViewById(R.id.swipeRefresh);
        calendarView       = findViewById(R.id.calendarView);
        chipGroupSlots     = findViewById(R.id.chipGroupSlots);
        progressSlots      = findViewById(R.id.progressSlots);
        tvFechaSeleccionada= findViewById(R.id.tvFechaSeleccionada);
        tvLastUpdate       = findViewById(R.id.tvLastUpdate);
        tvEstadoAgenda     = findViewById(R.id.tvEstadoAgenda);
        actvServicio       = findViewById(R.id.actvServicio);
        actvProfesional    = findViewById(R.id.actvProfesional);
        btnConfirmarCita   = findViewById(R.id.btnConfirmarCita);

        setSupportActionBar(toolbarAgenda);
        toolbarAgenda.setNavigationOnClickListener(v -> finish());

        // --- extras del servicio ---
        Intent in = getIntent();
        servicioId     = in.getStringExtra("servicioId");
        servicioNombre = in.getStringExtra("servicioNombre");
        duracionMin    = in.getIntExtra("duracionMin", 60);
        precio         = in.getDoubleExtra("precio", 0);
        actvServicio.setText(servicioNombre, false);
        actvServicio.setEnabled(false);

        // --- dropdown de profesional (placeholder) ---
        ArrayAdapter<String> staffAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        staffAdapter.add("Cualquiera");
        actvProfesional.setAdapter(staffAdapter);
        actvProfesional.setOnItemClickListener((parent, view, position, id) -> {
            String elegido = staffAdapter.getItem(position);
            if ("Cualquiera".equals(elegido)) {
                staffIdSeleccionado = null;
                staffNombreSeleccionado = "Cualquiera";
            } else {
                // aquí asignarías el id real del staff
                staffIdSeleccionado = "STAFF_ID_DEMO";
                staffNombreSeleccionado = elegido;
            }
            if (fechaSeleccionada != null) cargarDisponibilidad();
        });

        // --- fecha por defecto = hoy ---
        Calendar cal = Calendar.getInstance();
        fechaSeleccionada = sdfReq.format(cal.getTime());
        tvFechaSeleccionada.setText("Horarios disponibles — " + sdfShow.format(cal.getTime()));

        // listener del calendario (TIPO correcto)
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Calendar c = Calendar.getInstance();
                c.set(year, month, dayOfMonth, 0, 0, 0);
                fechaSeleccionada = sdfReq.format(c.getTime());
                tvFechaSeleccionada.setText("Horarios disponibles — " + sdfShow.format(c.getTime()));
                cargarDisponibilidad();
            }
        });

        // pull-to-refresh (TIPO correcto)
        swipeRefresh.setOnRefreshListener(() -> {
            cargarDisponibilidad();
            swipeRefresh.setRefreshing(false);
        });

        // botón confirmar
        btnConfirmarCita.setEnabled(false);
        btnConfirmarCita.setOnClickListener(v -> {
            if (horaSeleccionada == null) return;
            Intent next = new Intent(this, confirmacionCita.class); // cambia si tu clase se llama distinto
            next.putExtra("servicioId", servicioId);
            next.putExtra("servicioNombre", servicioNombre);
            next.putExtra("precio", precio);
            next.putExtra("duracionMin", duracionMin);
            next.putExtra("fecha", fechaSeleccionada);
            next.putExtra("hora", horaSeleccionada);
            next.putExtra("staffId", staffIdSeleccionado);
            next.putExtra("staffNombre", staffNombreSeleccionado);
            startActivity(next);
        });

        db = FirebaseFirestore.getInstance();
        cargarDisponibilidad(); // primera carga
    }

    // ================== DISPONIBILIDAD (demo local 09:00–18:00 sin bloqueos) ==================
    private void cargarDisponibilidad() {
        progressSlots.setVisibility(View.VISIBLE);
        chipGroupSlots.removeAllViews();
        tvEstadoAgenda.setText("");
        horaSeleccionada = null;
        btnConfirmarCita.setEnabled(false);

        // Demo: genera slots cada 30 min dentro de 09:00–18:00 y marca algunos ocupados de ejemplo
        List<String> ocupados = new ArrayList<>(); // aquí mezclarías citas + reservas del día
        // TODO: reemplaza por consulta real a Firestore:
        // db.collection("citas").whereEqualTo("fecha", fechaSeleccionada) ... etc.

        List<String> slots = generarSlots("09:00", "18:00", 30, duracionMin, ocupados);

        progressSlots.setVisibility(View.GONE);
        tvLastUpdate.setText("Actualizado " + sdfHour.format(Calendar.getInstance().getTime()));

        if (slots.isEmpty()) {
            tvEstadoAgenda.setText("No hay horarios disponibles para esta fecha");
            return;
        }

        for (String h : slots) {
            Chip c = new Chip(this, null, com.google.android.material.R.style.Widget_Material3_Chip_Assist_Elevated);
            c.setText(h);
            c.setCheckable(true);
            c.setOnClickListener(v -> {
                // desmarcar otros
                for (int i = 0; i < chipGroupSlots.getChildCount(); i++) {
                    View child = chipGroupSlots.getChildAt(i);
                    if (child instanceof Chip && child != v) ((Chip) child).setChecked(false);
                }
                horaSeleccionada = h;
                btnConfirmarCita.setEnabled(true);
            });
            chipGroupSlots.addView(c);
        }
    }

    /** Genera horas de inicio posibles dentro de [start,end], paso=stepMin, evitando horas ocupadas. */
    private List<String> generarSlots(String start, String end, int stepMin, int duracionMin, List<String> ocupados) {
        List<String> list = new ArrayList<>();
        int s = toMin(start);
        int e = toMin(end);
        for (int t = s; t + duracionMin <= e; t += stepMin) {
            String h = fromMin(t);
            if (!ocupados.contains(h)) list.add(h);
        }
        return list;
    }

    private static int toMin(String hhmm) {
        String[] sp = hhmm.split(":");
        return Integer.parseInt(sp[0]) * 60 + Integer.parseInt(sp[1]);
    }
    private static String fromMin(int m) {
        int h = m / 60, mi = m % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", h, mi);
    }
}