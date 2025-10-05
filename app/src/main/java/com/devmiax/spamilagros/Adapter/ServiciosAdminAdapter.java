package com.devmiax.spamilagros.Adapter;

import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.devmiax.spamilagros.R;
import com.devmiax.spamilagros.models.Servicio;
import com.google.android.material.chip.Chip;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServiciosAdminAdapter extends RecyclerView.Adapter<ServiciosAdminAdapter.VH> {

    public interface OnItemActionListener {
        void onEdit(Servicio s);
        void onDelete(Servicio s);
    }

    private final List<Servicio> data = new ArrayList<>();
    private final OnItemActionListener listener;

    public ServiciosAdminAdapter(OnItemActionListener l) {
        this.listener = l;
    }

    public void setItems(List<Servicio> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_servicio_admin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Servicio s = data.get(position);
        h.tvNombre.setText(s.nombre != null ? s.nombre : "");
        h.chipCategoria.setText(s.categoria != null ? s.categoria : "General");
        h.tvDuracion.setText(String.format(Locale.getDefault(), "%d min", s.duracionMin));
        h.tvPrecio.setText(String.format(Locale.getDefault(), "S/ %.2f", s.precio));
        h.tvDescripcion.setText(s.descripcion != null ? s.descripcion : "");

        h.btnMenu.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(v.getContext(), h.btnMenu);
            MenuInflater inflater = menu.getMenuInflater();
            inflater.inflate(R.menu.menu_item_admin, menu.getMenu());
            menu.setOnMenuItemClickListener((MenuItem item) -> {
                int id = item.getItemId();
                if (id == R.id.action_edit) {
                    if (listener != null) listener.onEdit(s);
                    return true;
                } else if (id == R.id.action_delete) {
                    if (listener != null) listener.onDelete(s);
                    return true;
                }
                return false;
            });
            menu.show();
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialTextView tvNombre, tvDuracion, tvPrecio, tvDescripcion;
        Chip chipCategoria;
        ImageButton btnMenu;

        VH(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreAdmin);
            chipCategoria = itemView.findViewById(R.id.chipCategoriaAdmin);
            tvDuracion = itemView.findViewById(R.id.tvDuracionAdmin);
            tvPrecio = itemView.findViewById(R.id.tvPrecioAdmin);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionAdmin);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
    }
}
