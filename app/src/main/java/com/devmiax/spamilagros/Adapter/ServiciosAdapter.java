package com.devmiax.spamilagros.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.devmiax.spamilagros.AgendaActivity;
import com.devmiax.spamilagros.R;
import com.devmiax.spamilagros.models.ServicioCliente;
import com.google.android.material.chip.Chip;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServiciosAdapter extends RecyclerView.Adapter<ServiciosAdapter.VH> {

    private final Context ctx;
    private final List<ServicioCliente> fullList = new ArrayList<>();
    private final List<ServicioCliente> filtered = new ArrayList<>();

    private String query = "";
    private String category = null; // null = todas

    public ServiciosAdapter(Context ctx) { this.ctx = ctx; }

    public void setItems(List<ServicioCliente> items) {
        fullList.clear();
        if (items != null) fullList.addAll(items);
        applyFilter();
    }

    public void setQuery(String q) {
        query = q == null ? "" : q.trim().toLowerCase(Locale.getDefault());
        applyFilter();
    }

    public void setCategory(String cat) {
        category = (cat != null && cat.equalsIgnoreCase("todos")) ? null : cat;
        applyFilter();
    }

    private void applyFilter() {
        filtered.clear();
        for (ServicioCliente s : fullList) {
            if (s == null || (s.activo != null && !s.activo)) continue;

            boolean okText = true;
            if (!query.isEmpty()) {
                String blob = ((s.nombre == null ? "" : s.nombre) + " " +
                        (s.descripcion == null ? "" : s.descripcion) + " " +
                        (s.categoria == null ? "" : s.categoria)).toLowerCase(Locale.getDefault());
                okText = blob.contains(query);
            }

            boolean okCat = (category == null) ||
                    (s.categoria != null && s.categoria.equalsIgnoreCase(category));

            if (okText && okCat) filtered.add(s);
        }
        notifyDataSetChanged();
        if (onEmptyStateChanged != null) onEmptyStateChanged.onEmpty(filtered.isEmpty());
    }

    public interface OnEmptyStateChanged { void onEmpty(boolean empty); }
    private OnEmptyStateChanged onEmptyStateChanged;
    public void setOnEmptyStateChanged(OnEmptyStateChanged l) { onEmptyStateChanged = l; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_servicio, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ServicioCliente s = filtered.get(position);
        h.tvNombre.setText(s.nombre != null ? s.nombre : "");
        h.chipCategoria.setText(s.categoria != null ? s.categoria : "General");
        h.tvDuracion.setText(String.format(Locale.getDefault(), "%d min", s.duracionMin));
        h.tvPrecio.setText(String.format(Locale.getDefault(), "S/ %.2f", s.precio));
        h.tvDescripcion.setText(s.descripcion != null ? s.descripcion : "");

        h.btnAgendar.setOnClickListener(v -> {
            Intent i = new Intent(ctx, AgendaActivity.class);
            i.putExtra("servicioId", s.id);
            i.putExtra("servicioNombre", s.nombre);
            i.putExtra("precio", s.precio);
            i.putExtra("duracionMin", s.duracionMin);
            ctx.startActivity(i);
        });
    }

    @Override
    public int getItemCount() { return filtered.size(); }

    static class VH extends RecyclerView.ViewHolder {
        MaterialTextView tvNombre, tvDuracion, tvPrecio, tvDescripcion;
        Chip chipCategoria;
        Button btnAgendar;
        VH(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvServicioNombre);
            chipCategoria = itemView.findViewById(R.id.chipCategoria);
            tvDuracion = itemView.findViewById(R.id.tvServicioDuracion);
            tvPrecio = itemView.findViewById(R.id.tvServicioPrecio);
            tvDescripcion = itemView.findViewById(R.id.tvServicioDescripcion);
            btnAgendar = itemView.findViewById(R.id.btnAgendarServicio);
        }
    }
}