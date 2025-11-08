package com.devmiax.spamilagros.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.devmiax.spamilagros.Adapter.ServiciosAdapter;
import com.devmiax.spamilagros.R;
import com.devmiax.spamilagros.models.ServicioCliente;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ServiciosFragment extends Fragment {

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private androidx.recyclerview.widget.RecyclerView rv;
    private LinearProgressIndicator progress;
    private TextInputEditText editBuscar;
    private ChipGroup chipGroup;
    private MaterialTextView tvEmpty;

    private FirebaseFirestore db;
    private ListenerRegistration liveListener;

    private ServiciosAdapter adapter;
    private final List<ServicioCliente> loaded = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_servicios, container, false);

        // Inicializa Firestore
        db = FirebaseFirestore.getInstance();

        // Bind de vistas
        swipe = view.findViewById(R.id.swipeServicios);
        rv = view.findViewById(R.id.rvServicios);
        progress = view.findViewById(R.id.progressServicios);
        editBuscar = view.findViewById(R.id.editBuscar);
        chipGroup = view.findViewById(R.id.chipGroupCategorias);
        tvEmpty = view.findViewById(R.id.tvEmptyState);

        // Configuración del RecyclerView
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ServiciosAdapter(requireContext());
        rv.setAdapter(adapter);

        adapter.setOnEmptyStateChanged(empty ->
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE));

        swipe.setOnRefreshListener(this::loadOnce);

        // Búsqueda en vivo
        editBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                adapter.setQuery(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        attachRealtime(); // Carga inicial con escucha en tiempo real
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (liveListener != null) liveListener.remove();
    }

    // ------------------- FIRESTORE -------------------
    private void attachRealtime() {
        progress.setVisibility(View.VISIBLE);
        liveListener = db.collection("servicios")
                .orderBy("nombre", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    progress.setVisibility(View.GONE);
                    swipe.setRefreshing(false);
                    if (e != null || snap == null) {
                        adapter.setItems(new ArrayList<>());
                        return;
                    }
                    loaded.clear();
                    snap.getDocuments().forEach(d -> {
                        ServicioCliente s = ServicioCliente.from(d);
                        if (s.activo == null || s.activo) loaded.add(s);
                    });
                    adapter.setItems(loaded);
                    buildCategoryChips(loaded);
                });
    }

    private void loadOnce() {
        db.collection("servicios")
                .orderBy("nombre", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    loaded.clear();
                    snap.getDocuments().forEach(d -> {
                        ServicioCliente s = ServicioCliente.from(d);
                        if (s.activo == null || s.activo) loaded.add(s);
                    });
                    adapter.setItems(loaded);
                    buildCategoryChips(loaded);
                    swipe.setRefreshing(false);
                })
                .addOnFailureListener(e -> swipe.setRefreshing(false));
    }

    // ------------------- UI: Chips de categorías -------------------
    private void buildCategoryChips(List<ServicioCliente> list) {
        chipGroup.removeAllViews();

        // Chip "Todos"
        Chip chipAll = makeChip("Todos", true);
        chipAll.setOnClickListener(v -> adapter.setCategory(null));
        chipGroup.addView(chipAll);

        // Únicas categorías
        Set<String> cats = new LinkedHashSet<>();
        for (ServicioCliente s : list)
            if (s.categoria != null && !s.categoria.isEmpty())
                cats.add(s.categoria);

        for (String cat : cats) {
            Chip ch = makeChip(cat, false);
            ch.setOnClickListener(v -> adapter.setCategory(cat));
            chipGroup.addView(ch);
        }
    }

    private Chip makeChip(String text, boolean checked) {
        Chip chip = new Chip(requireContext(), null,
                com.google.android.material.R.style.Widget_Material3_Chip_Filter);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(checked);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }
}
