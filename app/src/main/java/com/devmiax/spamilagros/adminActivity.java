package com.devmiax.spamilagros;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.devmiax.spamilagros.models.CitaLite;
import com.devmiax.spamilagros.models.Reserva;
import com.devmiax.spamilagros.Adapter.ReservasAdapter;
import com.devmiax.spamilagros.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.io.Console;
import java.text.SimpleDateFormat;
import java.util.*;

public class adminActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private androidx.recyclerview.widget.RecyclerView rv;
    private View progress;
    private LinearLayout emptyContainer;
    private TextView tvEmptyTitle, tvEmptySub;

    private FirebaseFirestore db;

    private final List<Reserva> reservasPend = new ArrayList<>();
    private final List<CitaLite> citasHoy = new ArrayList<>();

    // caches de nombres para mostrar
    private final Map<String,String> serviciosNombre = new HashMap<>();
    private final Map<String,String> usersNombre     = new HashMap<>();

    private ReservasAdapter reservasAdapter;

    enum Tab { RESERVAS, HOY }
    private Tab current = Tab.RESERVAS;

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_admin2);

        toolbar        = findViewById(R.id.toolbarAdmin);
        tabLayout      = findViewById(R.id.tabLayoutAdmin);
        swipe          = findViewById(R.id.swipeAdmin);
        rv             = findViewById(R.id.rvAdmin);
        progress       = findViewById(R.id.progressAdmin);
        emptyContainer = findViewById(R.id.emptyContainer);
        tvEmptyTitle   = findViewById(R.id.tvEmptyTitle);
        tvEmptySub     = findViewById(R.id.tvEmptySub);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_servicios) {
                startActivity(new Intent(this, com.devmiax.spamilagros.Admin.class));
                return true;
            }
            return false;
        });

        tabLayout.addTab(tabLayout.newTab().setText("Reservas"));
        tabLayout.addTab(tabLayout.newTab().setText("Citas de hoy"));

        rv.setLayoutManager(new LinearLayoutManager(this));
        reservasAdapter = new ReservasAdapter(this, serviciosNombre, usersNombre);
        rv.setAdapter(reservasAdapter);

        reservasAdapter.setOnAction(new ReservasAdapter.OnAction() {
            @Override public void onAceptar(Reserva r) { aceptarReserva(r); }
            @Override public void onRechazar(Reserva r) { rechazarReserva(r); }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                current = tab.getPosition()==0 ? Tab.RESERVAS : Tab.HOY;
                bindList();
                if (current == Tab.HOY) cargarCitasHoy(); else cargarReservas();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        swipe.setOnRefreshListener(() -> {
            if (current == Tab.HOY) cargarCitasHoy(); else cargarReservas();
        });

        db = FirebaseFirestore.getInstance();
        cargarReservas(); // por defecto
    }

    // ----------------- CARGAS -----------------
    private void cargarReservas() {
        progress.setVisibility(View.VISIBLE);

        db.collection("reservas")
                .whereEqualTo("estado", "pendiente")
                .get()
                .addOnSuccessListener(snap -> {
                    reservasPend.clear();
                    for (DocumentSnapshot d : snap) reservasPend.add(Reserva.from(d));

                    // Ordena por createdAt DESC en memoria (maneja nulls)
                    java.util.Collections.sort(reservasPend, (a,b) -> {
                        com.google.firebase.Timestamp ta = a.createdAt;
                        com.google.firebase.Timestamp tb = b.createdAt;
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;   // nulls al final
                        if (tb == null) return -1;
                        return tb.compareTo(ta);    // DESC
                    });

                    prefetchNombresServiciosYClientes(() -> {
                        progress.setVisibility(View.GONE);
                        swipe.setRefreshing(false);
                        bindList();
                    });
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    swipe.setRefreshing(false);
                    // útil para depurar en Logcat
                    android.util.Log.e("Admin", "cargarReservas()", e);
                    bindList();
                });
    }


    private void cargarCitasHoy() {
        progress.setVisibility(View.VISIBLE);
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance(TimeZone.getTimeZone("America/Lima")).getTime());

        db.collection("citas")
                .whereEqualTo("fecha", hoy)
                .orderBy("hora", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    citasHoy.clear();
                    for (DocumentSnapshot d : snap) citasHoy.add(CitaLite.from(d));
                    progress.setVisibility(View.GONE);
                    swipe.setRefreshing(false);

                    bindList();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    swipe.setRefreshing(false);
                    bindList();
                    Toast.makeText(this, "error: " + e, Toast.LENGTH_SHORT).show();

                });
    }

    private void bindList() {
        if (current == Tab.RESERVAS) {
            rv.setAdapter(reservasAdapter);
            reservasAdapter.setItems(reservasPend);
            showEmpty(reservasPend.isEmpty(),
                    "No hay reservas pendientes",
                    "Cuando los clientes reserven, aparecerán aquí para revisión.");
        } else {
            // muestra citas de hoy de forma simple reutilizando el adapter de reservas (solo lectura)
            List<Reserva> mapeo = new ArrayList<>();
            for (CitaLite c : citasHoy) {
                Reserva r = new Reserva();
                r.id = c.id;
                r.uidCliente = c.uidCliente;
                r.servicioId = c.servicioId;
                r.staffId = c.staffId;
                r.fecha = c.fecha;
                r.hora = c.hora;
                r.precio = c.precio;
                r.estado = "confirmada";
                mapeo.add(r);
            }
            rv.setAdapter(reservasAdapter);
            reservasAdapter.setItems(mapeo);
            showEmpty(mapeo.isEmpty(),
                    "No hay citas para hoy",
                    "Las citas confirmadas del día aparecerán aquí.");
        }
    }

    private void showEmpty(boolean empty, String title, String sub) {
        emptyContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmptyTitle.setText(title);
        tvEmptySub.setText(sub);
    }

    // --------- Prefetch nombres de servicio y clientes ---------
    private void prefetchNombresServiciosYClientes(Runnable then) {
        // servicios
        Set<String> sIds = new HashSet<>();
        Set<String> uIds = new HashSet<>();
        for (Reserva r : reservasPend) {
            if (r.servicioId != null && !serviciosNombre.containsKey(r.servicioId)) sIds.add(r.servicioId);
            if (r.uidCliente != null && !usersNombre.containsKey(r.uidCliente)) uIds.add(r.uidCliente);
        }
        final int totalBatches = (int) Math.ceil(sIds.size() / 10.0) + (int) Math.ceil(uIds.size() / 10.0);
        if (totalBatches == 0) { then.run(); return; }
        final int[] pending = { totalBatches };

        // servicios
        List<String> sList = new ArrayList<>(sIds);
        for (int i = 0; i < sList.size(); i += 10) {
            List<String> slice = sList.subList(i, Math.min(i+10, sList.size()));
            db.collection("servicios").whereIn(FieldPath.documentId(), slice).get()
                    .addOnSuccessListener(snap -> {
                        for (DocumentSnapshot d : snap) serviciosNombre.put(d.getId(), d.getString("nombre"));
                        if (--pending[0] == 0) then.run();
                    })
                    .addOnFailureListener(e -> { if (--pending[0] == 0) then.run(); });
        }
        // users
        List<String> uList = new ArrayList<>(uIds);
        for (int i = 0; i < uList.size(); i += 10) {
            List<String> slice = uList.subList(i, Math.min(i+10, uList.size()));
            db.collection("users").whereIn(FieldPath.documentId(), slice).get()
                    .addOnSuccessListener(snap -> {
                        for (DocumentSnapshot d : snap) usersNombre.put(d.getId(), d.getString("nombre"));
                        if (--pending[0] == 0) then.run();
                    })
                    .addOnFailureListener(e -> { if (--pending[0] == 0) then.run(); });
        }
    }

    // ----------------- ACCIONES -----------------
    private void aceptarReserva(Reserva r) {
        // Transacción: verifica pendiente, slot libre, crea cita, marca reserva confirmada
        DocumentReference reservaRef = db.collection("reservas").document(r.id);

        db.runTransaction(tr -> {
            DocumentSnapshot rs = tr.get(reservaRef);
            if (!rs.exists()) throw new FirebaseFirestoreException("Reserva no existe",
                    FirebaseFirestoreException.Code.ABORTED);
            String estado = rs.getString("estado");
            if (!"pendiente".equals(estado))
                throw new FirebaseFirestoreException("La reserva ya no está pendiente",
                        FirebaseFirestoreException.Code.ABORTED);

            String servicioId = rs.getString("servicioId");
            String uidCliente = rs.getString("uidCliente");
            String staffId    = rs.getString("staffId"); // puede ser null
            String fecha      = rs.getString("fecha");
            String hora       = rs.getString("hora");
            Double precio     = rs.getDouble("precio");

            // slotId determinístico para evitar doble booking
            String citaIdRandom = db.collection("citas").document().getId();
            String staffKey = (staffId != null && !staffId.isEmpty()) ? staffId : "any";
            String lock = "_" + fecha + "_" + hora.replace(":", "-");
            String slotId = citaIdRandom  + lock;
            DocumentReference citaRef = db.collection("citas").document(slotId);




            if (tr.get(citaRef).exists())
                throw new FirebaseFirestoreException("El horario ya está ocupado",
                        FirebaseFirestoreException.Code.ABORTED);

            // startAt = Timestamp( fecha + hora en tz Lima )
            Timestamp startAt = buildStartAt(fecha, hora);

            Map<String,Object> cita = new HashMap<>();
            cita.put("uidCliente", uidCliente);
            cita.put("servicioId", servicioId);
            cita.put("staffId",    staffId);
            cita.put("fecha",      fecha);
            cita.put("hora",       hora);
            cita.put("precio",     precio);
            cita.put("reservaId",  rs.getId());
            cita.put("startAt",    startAt);
            cita.put("createdAt",  FieldValue.serverTimestamp());

            tr.set(citaRef, cita);
            tr.update(reservaRef,
                    "estado", "confirmada",
                    "citaId", citaRef.getId()
            );
            return null;
        }).addOnSuccessListener(v -> {
            toast("Reserva aceptada");
            cargarReservas();
        }).addOnFailureListener(e -> toast("No se pudo aceptar: " + e.getMessage()));
    }


    private void rechazarReserva(Reserva r) {
        db.collection("reservas").document(r.id)
                .update("estado", "rechazada")
                .addOnSuccessListener(v -> {
                    toast("Reserva rechazada");
                    cargarReservas();
                })
                .addOnFailureListener(e -> toast("No se pudo rechazar: " + e.getMessage()));
    }

    private Timestamp buildStartAt(String fecha, String hora) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            fmt.setTimeZone(TimeZone.getTimeZone("America/Lima"));
            Date d = fmt.parse(fecha + " " + hora);
            return new Timestamp(d);
        } catch (Exception e) {
            return Timestamp.now();
        }
    }

    private void toast(String s) {
        android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_LONG).show();
    }
}