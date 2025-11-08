package com.devmiax.spamilagros.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.textview.MaterialTextView;
import com.devmiax.spamilagros.R;
import com.devmiax.spamilagros.models.Cita;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CitasAdapter extends RecyclerView.Adapter<CitasAdapter.VH> {

    public interface OnCitaClick { void onClick(Cita c); }

    private final List<Cita> data = new ArrayList<>();
    private final Map<String, ServicioLite> servicios; // id -> {nombre, duracionMin}
    private OnCitaClick listener;
    private FirebaseFirestore db;

    public CitasAdapter(Map<String, ServicioLite> servicios) {
        this.servicios = servicios;
    }

    public void setOnCitaClick(OnCitaClick l) { this.listener = l; }

    public void setItems(List<Cita> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cita, parent, false); // :contentReference[oaicite:3]{index=3}
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Cita c = data.get(pos);

        ServicioLite s = servicios != null ? servicios.get(c.servicioId) : null;
        h.tvServicio.setText(s != null && s.nombre != null ? s.nombre : "Servicio");

        // Estado (si guardas estado en citas; si no, deja “Confirmada” por defecto)
        String est = (c.estado != null) ? c.estado : "Confirmada";
        h.chipEstado.setText(cap(est));
        if(db == null){
            db = FirebaseFirestore.getInstance();
        }
        if (c.staffId  != null) {
            db.collection("users").document(c.staffId).get()
                    .addOnSuccessListener(st -> {
                        String nombreStaff = st.getString("nombre");
                        if (nombreStaff != null) h.tvProfesional.setText("Con " + nombreStaff);
                    })
                    .addOnFailureListener(e -> {

                        h.tvProfesional.setText("por asignar");
                    });
        }else{
            h.tvProfesional.setText("por asignar");
        }

        //ubicaión
        db.collection("parametros").limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot d = snap.getDocuments().get(0);

                        String direccion = d.getString("direccion");


                        // Pintar texto de ubicación (prefiere dirección; si no, coordenada)
                        if (h.tvDireccion != null) {
                            if (direccion != null && !direccion.isEmpty()) {
                                h.tvDireccion .setText(direccion);
                            }
                        }

                    }
                })
                .addOnFailureListener(e -> {
                    if (h.tvDireccion != null) h.tvDireccion.setVisibility(View.GONE);

                });





        h.tvFechaHora.setText(formatFechaHora(c.fecha, c.hora));



        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(c);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        MaterialTextView tvServicio, tvFechaHora, tvProfesional, tvDireccion;
        Chip chipEstado;
        VH(@NonNull View v) {
            super(v);
            tvServicio   = v.findViewById(R.id.tvItemServicio);
            chipEstado   = v.findViewById(R.id.chipEstado);
            tvFechaHora  = v.findViewById(R.id.tvItemFechaHora);
            tvProfesional= v.findViewById(R.id.tvItemProfesional);
            tvDireccion = v.findViewById(R.id.tvItemUbicacion);
        }
    }

    private static String formatFechaHora(String fecha, String hora) {
        // Entrada "2025-10-05" + "10:30" → "Dom 05 Oct • 10:30"
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            java.util.Date d = in.parse(fecha + " " + hora);
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("EEE dd MMM • HH:mm", new Locale("es","PE"));
            return out.format(d);
        } catch (Exception e) { return (fecha!=null?fecha:"") + " • " + (hora!=null?hora:""); }
    }
    private static String cap(String s) { return s==null? "" : s.substring(0,1).toUpperCase() + s.substring(1); }

    /** Estructura simple para no traer todo el servicio. */
    public static class ServicioLite {
        public String nombre; public Integer duracionMin; public Double precio;
        public ServicioLite() {}
        public ServicioLite(String n, Integer d, Double p) { nombre=n; duracionMin=d; precio=p; }
    }
}