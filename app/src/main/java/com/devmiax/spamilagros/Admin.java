package com.devmiax.spamilagros;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.devmiax.spamilagros.Adapter.ServiciosAdminAdapter;
import com.devmiax.spamilagros.R;
import com.devmiax.spamilagros.models.Servicio;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class Admin extends AppCompatActivity implements ServiciosAdminAdapter.OnItemActionListener {

    private MaterialToolbar toolbar;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private androidx.recyclerview.widget.RecyclerView rv;
    private LinearProgressIndicator progress;
    private FloatingActionButton fab;

    private FirebaseFirestore db;
    private CollectionReference colServicios;
    private ListenerRegistration liveListener;

    private ServiciosAdminAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();
        colServicios = db.collection("servicios");

        toolbar = findViewById(R.id.toolbarAdmin);
        swipe   = findViewById(R.id.swipeAdmin);
        rv      = findViewById(R.id.rvAdminServicios);
        progress= findViewById(R.id.progressAdmin);
        fab     = findViewById(R.id.fabCrearServicio);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new ServiciosAdminAdapter(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        swipe.setOnRefreshListener(this::loadOnce);
        fab.setOnClickListener(v -> showCreateEditDialog(null));

        // Listado en tiempo real
        attachRealtime();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (liveListener != null) {
            liveListener.remove();
        }
    }

    /** Listener en tiempo real: actualiza la lista cada vez que hay cambios */
    private void attachRealtime() {
        progress.setVisibility(View.VISIBLE);
        liveListener = colServicios
                .orderBy("nombre", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        progress.setVisibility(View.GONE);
                        swipe.setRefreshing(false);

                        if (e != null) {
                            Toast.makeText(Admin.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (snapshots == null) {
                            adapter.setItems(new ArrayList<>());
                            return;
                        }
                        List<Servicio> items = new ArrayList<>();
                        for (var doc : snapshots.getDocuments()) {
                            items.add(Servicio.from(doc));
                        }
                        adapter.setItems(items);
                    }
                });
    }

    /** Carga “manual” cuando haces pull-to-refresh (no necesario si usas realtime, pero útil) */
    private void loadOnce() {
        colServicios.orderBy("nombre", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Servicio> items = new ArrayList<>();
                    snapshots.forEach(d -> items.add(Servicio.from(d)));
                    adapter.setItems(items);
                    swipe.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    swipe.setRefreshing(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /** Diálogo para crear o editar */
    public void showCreateEditDialog(@Nullable Servicio s) {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_edit_service, null, false);

        TextInputLayout tilNombre = view.findViewById(R.id.tilNombre);
        TextInputLayout tilCategoria = view.findViewById(R.id.tilCategoria);
        TextInputLayout tilDuracion = view.findViewById(R.id.tilDuracion);
        TextInputLayout tilPrecio = view.findViewById(R.id.tilPrecio);
        TextInputLayout tilDescripcion = view.findViewById(R.id.tilDescripcion);

        TextInputEditText edtNombre = view.findViewById(R.id.edtNombre);
        TextInputEditText edtCategoria = view.findViewById(R.id.edtCategoria);
        TextInputEditText edtDuracion = view.findViewById(R.id.edtDuracion);
        TextInputEditText edtPrecio = view.findViewById(R.id.edtPrecio);
        TextInputEditText edtDescripcion = view.findViewById(R.id.edtDescripcion);

        if (s != null) {
            edtNombre.setText(s.nombre);
            edtCategoria.setText(s.categoria);
            edtDuracion.setText(String.valueOf(s.duracionMin));
            edtPrecio.setText(String.valueOf(s.precio));
            edtDescripcion.setText(s.descripcion);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(s == null ? "Crear servicio" : "Editar servicio")
                .setView(view)
                .setPositiveButton(s == null ? "Crear" : "Guardar", (d, which) -> {
                    String nombre = edtNombre.getText() != null ? edtNombre.getText().toString().trim() : "";
                    String cat    = edtCategoria.getText() != null ? edtCategoria.getText().toString().trim() : "";
                    String durStr = edtDuracion.getText() != null ? edtDuracion.getText().toString().trim() : "";
                    String preStr = edtPrecio.getText() != null ? edtPrecio.getText().toString().trim() : "";
                    String desc   = edtDescripcion.getText() != null ? edtDescripcion.getText().toString().trim() : "";

                    if (nombre.isEmpty() || cat.isEmpty() || durStr.isEmpty() || preStr.isEmpty()) {
                        Toast.makeText(this, "Completa los campos requeridos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int durMin;
                    double precio;
                    try {
                        durMin = Integer.parseInt(durStr);
                        precio = Double.parseDouble(preStr);
                    } catch (NumberFormatException ex) {
                        Toast.makeText(this, "Duración o precio inválidos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (s == null) {
                        crearServicio(nombre, cat, durMin, precio, desc);
                    } else {
                        actualizarServicio(s.id, nombre, cat, durMin, precio, desc);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** CREATE */
    private void crearServicio(String nombre, String cat, int durMin, double precio, String desc) {
        progress.setVisibility(View.VISIBLE);

        Servicio nuevo = new Servicio(null, nombre, cat, durMin, precio, desc, Timestamp.now());
        colServicios.add(nuevo.toMap())
                .addOnSuccessListener(ref -> {
                    // guarda el id dentro del documento (útil para futuras ediciones)
                    ref.update("id", ref.getId());
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Servicio creado", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al crear: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /** UPDATE */
    private void actualizarServicio(String id, String nombre, String cat, int durMin, double precio, String desc) {
        if (id == null || id.isEmpty()) {
            Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show();
            return;
        }
        progress.setVisibility(View.VISIBLE);
        colServicios.document(id)
                .update("nombre", nombre,
                        "categoria", cat,
                        "duracionMin", durMin,
                        "precio", precio,
                        "descripcion", desc)
                .addOnSuccessListener(aVoid -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /** DELETE (con confirmación) */
    public void eliminarServicioConfirmado(String id) {
        if (id == null || id.isEmpty()) return;
        progress.setVisibility(View.VISIBLE);
        colServicios.document(id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Servicio eliminado", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Callbacks desde el Adapter
    @Override
    public void onEdit(Servicio s) {
        showCreateEditDialog(s);
    }

    @Override
    public void onDelete(Servicio s) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar servicio")
                .setMessage("¿Deseas eliminar \"" + s.nombre + "\"?")
                .setPositiveButton("Eliminar", (d, w) -> eliminarServicioConfirmado(s.id))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}