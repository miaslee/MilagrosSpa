package com.devmiax.spamilagros.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.devmiax.spamilagros.Adapter.CitasAdapter;
import com.devmiax.spamilagros.DetalleCita;
import com.devmiax.spamilagros.R;
import com.devmiax.spamilagros.models.Cita;
import com.devmiax.spamilagros.servicios;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.*;

public class MisCitasFragment extends Fragment {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private androidx.recyclerview.widget.RecyclerView rv;
    private LinearLayout emptyContainer;
    private MaterialTextView tvEmptyTitle, tvEmptySub;
    private View btnIrAgendar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final List<Cita> todas = new ArrayList<>();
    private final Map<String, CitasAdapter.ServicioLite> servicios = new HashMap<>();
    private CitasAdapter adapter;

    private enum Tab { PROXIMAS, PASADAS }
    private Tab currentTab = Tab.PROXIMAS;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_mis_citas, container, false);

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // UI binds

        tabLayout      = view.findViewById(R.id.tabLayoutMisCitas);
        swipe          = view.findViewById(R.id.swipeAppointments);
        rv             = view.findViewById(R.id.rvAppointments);
        emptyContainer = view.findViewById(R.id.emptyStateContainer);
        tvEmptyTitle   = view.findViewById(R.id.tvEmptyTitle);
        tvEmptySub     = view.findViewById(R.id.tvEmptySub);
        btnIrAgendar   = view.findViewById(R.id.btnIrAgendar);

        // Configuración base
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CitasAdapter(servicios);
        rv.setAdapter(adapter);

        adapter.setOnCitaClick(c -> {
            Intent i = new Intent(requireContext(), DetalleCita.class);
            i.putExtra("citaId", c.id);
            startActivity(i);
        });

        // Tabs (Próximas / Pasadas)
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = (tab.getPosition() == 0) ? Tab.PROXIMAS : Tab.PASADAS;
                aplicarFiltroYMostrar();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Swipe refresh
        swipe.setOnRefreshListener(this::cargarCitas);

        // Botón "Ir a agendar" → abrir fragment de Servicios
        btnIrAgendar.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ServiciosFragment())
                    .addToBackStack(null)
                    .commit();

            // Marcar item en el BottomNavigationView
            com.google.android.material.bottomnavigation.BottomNavigationView navView =
                    requireActivity().findViewById(R.id.bottom_navigation);
            if (navView != null) navView.setSelectedItemId(R.id.nav_servicios);
        });

        // Cargar citas
        cargarCitas();

        return view;
    }

    private void cargarCitas() {
        swipe.setRefreshing(true);
        var user = auth.getCurrentUser();
        if (user == null) {
            swipe.setRefreshing(false);
            mostrarVacio(true);
            return;
        }

        db.collection("citas")
                .whereEqualTo("uidCliente", user.getUid())
                .orderBy("fecha", Query.Direction.ASCENDING)
                .orderBy("hora", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    todas.clear();
                    snap.getDocuments().forEach(d -> todas.add(Cita.from(d)));

                    prefetchServicios(() -> {
                        swipe.setRefreshing(false);
                        aplicarFiltroYMostrar();
                    });
                })
                .addOnFailureListener(e -> {
                    swipe.setRefreshing(false);
                    mostrarVacio(true);
                    Log.e("spa", "Error cargando citas", e);
                });
    }

    private void aplicarFiltroYMostrar() {
        List<Cita> lista = new ArrayList<>();
        for (Cita c : todas) {
            int cmp = compareWithNow(c.fecha, c.hora);
            if (currentTab == Tab.PROXIMAS && cmp >= 0) lista.add(c);
            if (currentTab == Tab.PASADAS && cmp < 0) lista.add(c);
        }
        adapter.setItems(lista);
        mostrarVacio(lista.isEmpty());
    }

    private void mostrarVacio(boolean empty) {
        emptyContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (currentTab == Tab.PROXIMAS) {
            tvEmptyTitle.setText("No tienes citas próximas");
            tvEmptySub.setText("Cuando reserves, aparecerán aquí.");
        } else {
            tvEmptyTitle.setText("No tienes citas pasadas");
            tvEmptySub.setText("Tus citas completadas o antiguas aparecerán aquí.");
        }
    }

    private void prefetchServicios(Runnable then) {
        Set<String> ids = new HashSet<>();
        for (Cita c : todas)
            if (c.servicioId != null && !servicios.containsKey(c.servicioId))
                ids.add(c.servicioId);

        if (ids.isEmpty()) {
            then.run();
            return;
        }

        List<String> list = new ArrayList<>(ids);
        final int[] pending = {(int) Math.ceil(list.size() / 10.0)};
        if (pending[0] == 0) pending[0] = 1;

        for (int i = 0; i < list.size(); i += 10) {
            List<String> slice = list.subList(i, Math.min(i + 10, list.size()));
            db.collection("servicios")
                    .whereIn(FieldPath.documentId(), slice)
                    .get()
                    .addOnSuccessListener(snap -> {
                        snap.getDocuments().forEach(d -> {
                            String id = d.getId();
                            String nombre = d.getString("nombre");
                            Integer dur = d.getLong("duracionMin") != null
                                    ? d.getLong("duracionMin").intValue() : null;
                            Double precio = d.getDouble("precio");
                            servicios.put(id, new CitasAdapter.ServicioLite(nombre, dur, precio));
                        });
                        if (--pending[0] == 0) then.run();
                    })
                    .addOnFailureListener(e -> {
                        if (--pending[0] == 0) then.run();
                    });
        }
    }

    private int compareWithNow(String fecha, String hora) {
        try {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            fmt.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
            java.util.Date dt = fmt.parse(fecha + " " + hora);
            Calendar now = Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Lima"));
            return dt.compareTo(now.getTime());
        } catch (Exception e) {
            return 0;
        }
    }
}
