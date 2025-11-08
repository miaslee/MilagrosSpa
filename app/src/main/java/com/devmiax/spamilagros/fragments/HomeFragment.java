package com.devmiax.spamilagros.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.devmiax.spamilagros.DetalleCita;
import com.devmiax.spamilagros.R;
import com.devmiax.spamilagros.misCitas;
import com.devmiax.spamilagros.servicios;
import com.devmiax.spamilagros.PerfilActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomeFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private View cardNextAppointment;
    private TextView tvNextTitle, tvNextDetail;
    private MaterialButton btnAgendarDesdeBanner;

    // Guardar referencia a la próxima cita
    private String proximaCitaId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind de vistas

        cardNextAppointment = view.findViewById(R.id.cardNextAppointment);
        tvNextTitle = view.findViewById(R.id.tvNextTitle);
        tvNextDetail = view.findViewById(R.id.tvNextDetail);
        btnAgendarDesdeBanner = view.findViewById(R.id.btnAgendarDesdeBanner);

        // Botones
        nav(view, R.id.btnGoToAgenda, servicios.class);
        nav(view, R.id.btnGoToMyAppointments, misCitas.class);
        nav(view, R.id.btnGoToServicios, servicios.class);
        nav(view, R.id.btnGoToPerfil, PerfilActivity.class);

        btnAgendarDesdeBanner.setOnClickListener(v -> {
            // Cambiar al fragmento de Servicios
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ServiciosFragment())
                    .addToBackStack(null)
                    .commit();

            // Marcar el ítem de Servicios en el BottomNavigationView
            BottomNavigationView navView = requireActivity().findViewById(R.id.bottom_navigation);
            if (navView != null) {
                navView.setSelectedItemId(R.id.nav_servicios);
            }
        });


        // Click en el banner
        cardNextAppointment.setOnClickListener(v -> {
            if (proximaCitaId != null) {
                Intent i = new Intent(requireContext(), DetalleCita.class);
                i.putExtra("citaId", proximaCitaId);
                startActivity(i);
            }
        });

        // Cargar próxima cita
        cargarProximaCita();

        return view;
    }

    private void nav(View parent, int viewId, Class<?> target) {
        View v = parent.findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(x ->
                    startActivity(new Intent(requireContext(), target))
            );
        }
    }

    // ====================== CARGA DE PRÓXIMA CITA ======================
    private void cargarProximaCita() {
        var user = auth.getCurrentUser();
        if (user == null) {
            mostrarSinProximaCita();
            return;
        }

        Timestamp ahora = Timestamp.now();
        db.collection("citas")
                .whereEqualTo("uidCliente", user.getUid())
                .whereGreaterThanOrEqualTo("startAt", ahora)
                .orderBy("startAt", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        DocumentSnapshot d = snap.getDocuments().get(0);
                        renderBannerConCita(d);
                    } else {
                        cargarProximaCitaSinStartAt(user.getUid());
                    }
                })
                .addOnFailureListener(e -> cargarProximaCitaSinStartAt(user.getUid()));
    }

    private void cargarProximaCitaSinStartAt(String uid) {
        db.collection("citas")
                .whereEqualTo("uidCliente", uid)
                .orderBy("fecha", Query.Direction.ASCENDING)
                .orderBy("hora", Query.Direction.ASCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(snap -> {
                    DocumentSnapshot futura = null;
                    for (DocumentSnapshot d : snap) {
                        String f = d.getString("fecha");
                        String h = d.getString("hora");
                        if (compareWithNowLima(f, h) >= 0) {
                            futura = d;
                            break;
                        }
                    }
                    if (futura != null) renderBannerConCita(futura);
                    else mostrarSinProximaCita();
                })
                .addOnFailureListener(e -> mostrarSinProximaCita());
    }

    private void renderBannerConCita(DocumentSnapshot d) {
        proximaCitaId = d.getId();

        String servicioId = d.getString("servicioId");
        String staffId = d.getString("staffId");
        String fecha = d.getString("fecha");
        String hora = d.getString("hora");

        tvNextTitle.setText("Tu próxima cita");
        String fechaBonita = formatFechaHoraBonita(fecha, hora);

        final String[] nombreServicio = {null};
        final String[] nombreStaff = {null};

        List<Runnable> rendersPendientes = new ArrayList<>();

        if (servicioId != null) {
            rendersPendientes.add(() ->
                    db.collection("servicios").document(servicioId).get()
                            .addOnSuccessListener(sd -> {
                                nombreServicio[0] = sd.getString("nombre");
                                actualizarDetalle(fechaBonita, nombreServicio[0], nombreStaff[0]);
                            })
            );
        }
        if (staffId != null) {
            rendersPendientes.add(() ->
                    db.collection("staff").document(staffId).get()
                            .addOnSuccessListener(st -> {
                                nombreStaff[0] = st.getString("nombre");
                                actualizarDetalle(fechaBonita, nombreServicio[0], nombreStaff[0]);
                            })
            );
        }

        // Render inicial
        actualizarDetalle(fechaBonita, null, null);

        // Ejecutar consultas
        for (Runnable r : rendersPendientes) r.run();
    }

    private void actualizarDetalle(String fechaBonita, @Nullable String serv, @Nullable String staff) {
        StringBuilder sb = new StringBuilder();
        sb.append(fechaBonita);
        if (serv != null && !serv.isEmpty()) {
            sb.append("\n").append(serv);
            if (staff != null && !staff.isEmpty()) {
                sb.append(" • Con ").append(staff);
            }
        }
        tvNextDetail.setText(sb.toString());
    }

    private void mostrarSinProximaCita() {
        proximaCitaId = null;
        tvNextTitle.setText("Tu próxima cita");
        tvNextDetail.setText("Aún no tienes una cita programada.");
    }

    // ================= Helpers de tiempo (America/Lima) =================
    private int compareWithNowLima(String fecha, String hora) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            fmt.setTimeZone(TimeZone.getTimeZone("America/Lima"));
            Date dt = fmt.parse(fecha + " " + hora);
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));
            return dt.compareTo(now.getTime());
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatFechaHoraBonita(String fecha, String hora) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            in.setTimeZone(TimeZone.getTimeZone("America/Lima"));
            Date d = in.parse(fecha + " " + hora);
            SimpleDateFormat out = new SimpleDateFormat("EEE dd MMM • HH:mm", new Locale("es", "PE"));
            out.setTimeZone(TimeZone.getTimeZone("America/Lima"));
            return out.format(d);
        } catch (Exception e) {
            return (fecha != null ? fecha : "") + " • " + (hora != null ? hora : "");
        }
    }
}
