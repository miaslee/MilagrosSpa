package com.devmiax.spamilagros;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class DetalleCita extends AppCompatActivity {

    // UI
    private Chip chipEstado, chipCodigo;
    private MaterialTextView tvServicio, tvFechaHora, tvProfesional, tvPrecio, tvDuracion, tvUbicacion, tvNotas;
    private View rowUbicacion, btnAbrirUbicacion, btnAgregarCalendario, btnCompartir;

    // Data
    private FirebaseFirestore db;
    private String citaId;
    private String servicioId, staffId, fecha, hora, codigo, estado;
    private Double precio;
    private Integer duracionMin;
    private String nombreServicio, nombreStaff;

    private String direccion, coordena;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_cita);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbarDetalle);
        setSupportActionBar(toolbar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        // Bind views
        chipEstado       = findViewById(R.id.chipDetEstado);
        chipCodigo       = findViewById(R.id.chipDetCodigo);
        tvServicio       = findViewById(R.id.tvDetServicio);
        tvFechaHora      = findViewById(R.id.tvDetFechaHora);
        tvProfesional    = findViewById(R.id.tvDetProfesional);
        tvPrecio         = findViewById(R.id.tvDetPrecio);
        tvDuracion       = findViewById(R.id.tvDetDuracion);
        tvUbicacion      = findViewById(R.id.tvDetUbicacion);
        tvNotas          = findViewById(R.id.tvDetNotas);
        rowUbicacion     = findViewById(R.id.rowUbicacion);
        btnAbrirUbicacion= findViewById(R.id.btnAbrirUbicacion);
        btnAgregarCalendario = findViewById(R.id.btnAgregarCalendario);
        btnCompartir     = findViewById(R.id.btnCompartirDetalle);

        // Extra
        citaId = getIntent().getStringExtra("citaId");
        if (citaId == null || citaId.isEmpty()) {
            Toast.makeText(this, "No se recibió el ID de la cita.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        cargarParametrosUbicacion();   // ← carga direccion/coordena de "parametros"
        if (btnAbrirUbicacion != null) {
            btnAbrirUbicacion.setOnClickListener(v -> abrirUbicacion());
        }
        cargarCita(citaId);
    }

    private void cargarParametrosUbicacion() {
        // Si tienes un doc fijo: db.collection("parametros").document("app").get()...
        db.collection("parametros").limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot d = snap.getDocuments().get(0);

                        direccion = d.getString("direccion");
                        coordena  = d.getString("coordenada"); // p.ej. "-13.653057, -73.361240"

                        // Pintar texto de ubicación (prefiere dirección; si no, coordenada)
                        if (tvUbicacion != null) {
                            if (direccion != null && !direccion.isEmpty()) {
                                tvUbicacion.setText(direccion);
                            } else if (coordena != null && !coordena.isEmpty()) {
                                tvUbicacion.setText(coordena);
                            }
                        }

                        if (rowUbicacion != null) {
                            boolean visible = (direccion != null && !direccion.isEmpty())
                                    || (coordena  != null && !coordena.isEmpty());
                            rowUbicacion.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        Toast.makeText(this, "Parámetros cargados", Toast.LENGTH_SHORT).show();
                    } else {
                        if (rowUbicacion != null) rowUbicacion.setVisibility(View.GONE);
                        Toast.makeText(this, "No se encontraron parámetros de ubicación", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (rowUbicacion != null) rowUbicacion.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar parámetros: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    /** Abre Google Maps usando 'coordena' si existe; si no, busca por 'direccion'. */
    private void abrirUbicacion() {
        try {
            android.net.Uri uri;
            double[] ll = parseLatLng(coordena);
            if (ll != null) {
                // geo:lat,lng?q=lat,lng(Label)
                String label = (nombreServicio != null ? nombreServicio : "Ubicación");
                uri = android.net.Uri.parse("geo:" + ll[0] + "," + ll[1] +
                        "?q=" + ll[0] + "," + ll[1] + "(" + java.net.URLEncoder.encode(label, "UTF-8") + ")");
            } else if (direccion != null && !direccion.isEmpty()) {
                uri = android.net.Uri.parse("geo:0,0?q=" + java.net.URLEncoder.encode(direccion, "UTF-8"));
            } else {
                return;
            }
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(mapIntent);
        } catch (Exception ignored) {}
    }
    private void cargarCita(String id) {
        db.collection("citas").document(id).get()
                .addOnSuccessListener(this::renderCita)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "No se pudo cargar la cita.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    @Nullable
    private double[] parseLatLng(@Nullable String s) {
        if (s == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*,\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*$")
                .matcher(s);
        if (!m.find()) return null;

        double lat = Double.parseDouble(m.group(1));
        double lng = Double.parseDouble(m.group(2));
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return null;
        return new double[]{lat, lng};
    }


    private void renderCita(DocumentSnapshot d) {
        if (!d.exists()) {
            Toast.makeText(this, "La cita no existe.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Campos base
        servicioId = d.getString("servicioId");
        staffId    = d.getString("staffId");
        fecha      = d.getString("fecha");   // "yyyy-MM-dd"
        hora       = d.getString("hora");    // "HH:mm"
        precio     = d.getDouble("precio");
        estado     = d.getString("estado");
        codigo     = d.getString("codigo");
        duracionMin= d.contains("duracionMin") ? (d.getLong("duracionMin") != null ? d.getLong("duracionMin").intValue() : null) : null;

        // Estado + código
        chipEstado.setText(cap(estado != null ? estado : "confirmada"));
        chipEstado.setChecked("confirmada".equalsIgnoreCase(estado));
        chipCodigo.setText("Código: " + (nz(codigo, abrevId(d.getId()))));

        // Fecha y hora
        tvFechaHora.setText(formatFechaHoraBonita(fecha, hora));

        // Precio/duración preliminar (por si no llegan los fetch)
        if (precio != null) tvPrecio.setText("S/ " + trimZeros(precio));
        if (duracionMin != null) tvDuracion.setText(duracionMin + " min");




        if (btnAgregarCalendario != null) {
            btnAgregarCalendario.setOnClickListener(v -> agregarACalendario());
        }
        if (btnCompartir != null) {
            btnCompartir.setOnClickListener(v -> compartirResumen());
        }

        // Cargar nombres de servicio y staff
        if (servicioId != null) {
            db.collection("servicios").document(servicioId).get()
                    .addOnSuccessListener(sd -> {
                        nombreServicio = sd.getString("nombre");
                        Integer dur = sd.contains("duracionMin") && sd.getLong("duracionMin")!=null ?
                                sd.getLong("duracionMin").intValue() : null;
                        Double pr = sd.getDouble("precio");
                        if (nombreServicio != null) tvServicio.setText(nombreServicio);
                        if (dur != null) tvDuracion.setText(dur + " min");
                        if (pr != null) tvPrecio.setText("S/ " + trimZeros(pr));
                    });
        }
        if (staffId != null) {
            db.collection("users").document(staffId).get()
                    .addOnSuccessListener(st -> {
                        nombreStaff = st.getString("nombre");
                        if (nombreStaff != null) tvProfesional.setText("Con " + nombreStaff);
                    })
                    .addOnFailureListener(e -> {

                        Toast.makeText(this, "Error " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void agregarACalendario() {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            fmt.setTimeZone(TimeZone.getTimeZone("America/Lima"));
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));
            cal.setTime(fmt.parse(fecha + " " + hora));

            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, nz(nombreServicio, "Cita"))
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.getTimeInMillis())
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, cal.getTimeInMillis() + (duracionMin != null ? duracionMin : 60) * 60_000L)
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, "MilagrosSpa");

            startActivity(intent);
        } catch (Exception ignored) { }
    }

    private void compartirResumen() {
        String resumen = "Cita " + cap(nz(estado, "confirmada")) + "\n" +
                "Servicio: " + nz(nombreServicio, "—") + "\n" +
                "Fecha y hora: " + formatFechaHoraBonita(fecha, hora) + "\n" +
                "Profesional: " + (nombreStaff != null ? nombreStaff : "por asignar") + "\n" +
                "Precio: " + (precio != null ? "S/ " + trimZeros(precio) : "—") + "\n" +
                "Código: " + nz(codigo, abrevId(citaId));

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, resumen);
        startActivity(Intent.createChooser(send, "Compartir cita"));
    }

    // Utils
    private static String nz(String s, String def) { return s != null ? s : def; }
    private static String cap(String s) { return s.isEmpty()? s : s.substring(0,1).toUpperCase() + s.substring(1); }
    private static String abrevId(String id) { return id != null && id.length() > 6 ? id.substring(0,6).toUpperCase() : nz(id,"—"); }
    private static String trimZeros(Double d) { return d == null ? "" : (d % 1 == 0 ? String.valueOf(d.intValue()) : String.format(Locale.getDefault(),"%.2f", d)); }

    private String formatFechaHoraBonita(String fecha, String hora) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            in.setTimeZone(TimeZone.getTimeZone("America/Lima"));
            java.util.Date dt = in.parse(fecha + " " + hora);
            SimpleDateFormat out = new SimpleDateFormat("EEE dd MMM • HH:mm", new Locale("es","PE"));
            out.setTimeZone(TimeZone.getTimeZone("America/Lima"));
            return out.format(dt);
        } catch (Exception e) {
            return (fecha != null ? fecha : "") + " • " + (hora != null ? hora : "");
        }
    }
}
