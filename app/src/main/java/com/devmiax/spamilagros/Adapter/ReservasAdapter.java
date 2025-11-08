package com.devmiax.spamilagros.Adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.devmiax.spamilagros.models.Reserva;
import com.devmiax.spamilagros.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReservasAdapter extends RecyclerView.Adapter<ReservasAdapter.VH> {

    public interface OnAction {
        void onAceptar(Reserva r);
        void onRechazar(Reserva r);
    }

    private final Context ctx;
    private final List<Reserva> data = new ArrayList<>();
    private OnAction listener;
    private final Map<String, String> servicioNombre; // servicioId -> nombre
    private final Map<String, String> userNombre;     // uid -> nombre (opcional)

    public ReservasAdapter(Context ctx, Map<String,String> servicioNombre, Map<String,String> userNombre) {
        this.ctx = ctx;
        this.servicioNombre = servicioNombre;
        this.userNombre = userNombre;
    }

    public void setOnAction(OnAction l) { listener = l; }

    public void setItems(List<Reserva> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_reserva, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Reserva r = data.get(pos);

        String nomServ = servicioNombre.getOrDefault(r.servicioId, "Servicio");
        h.tvServicio.setText(nomServ);

        h.tvFechaHora.setText(formatFechaHora(r.fecha, r.hora));
        String nomCli = userNombre.getOrDefault(r.uidCliente, r.uidCliente);
        h.tvCliente.setText("Cliente: " + nomCli);

        double dep = r.deposito != null ? r.deposito : ((r.precio!=null? r.precio:0)*0.5);
        h.chipDeposito.setText(String.format(Locale.getDefault(),"Depósito S/ %.2f", dep));

        h.btnVerVoucher.setOnClickListener(v -> {
            if (r.voucherUrl != null && !r.voucherUrl.isEmpty()) {
                ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(r.voucherUrl)));
            }
        });

        h.btnAceptar.setOnClickListener(v -> { if (listener != null) listener.onAceptar(r); });
        h.btnRechazar.setOnClickListener(v -> { if (listener != null) listener.onRechazar(r); });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        MaterialTextView tvServicio, tvFechaHora, tvCliente;
        Chip chipDeposito;
        MaterialButton btnVerVoucher, btnAceptar, btnRechazar;
        VH(View v) {
            super(v);
            tvServicio   = v.findViewById(R.id.tvReservaServicio);
            tvFechaHora  = v.findViewById(R.id.tvReservaFechaHora);
            tvCliente    = v.findViewById(R.id.tvReservaCliente);
            chipDeposito = v.findViewById(R.id.chipDeposito);
            btnVerVoucher= v.findViewById(R.id.btnVerVoucher);
            btnAceptar   = v.findViewById(R.id.btnAceptar);
            btnRechazar  = v.findViewById(R.id.btnRechazar);
        }
    }

    private static String formatFechaHora(String fecha, String hora) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date d = in.parse(fecha + " " + hora);
            return new SimpleDateFormat("EEE dd MMM • HH:mm", new Locale("es","PE")).format(d);
        } catch (Exception e) { return (fecha!=null?fecha:"") + " • " + (hora!=null?hora:""); }
    }
}
