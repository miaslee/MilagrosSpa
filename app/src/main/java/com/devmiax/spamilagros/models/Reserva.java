package com.devmiax.spamilagros.models;


import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

public class Reserva {
    public String id;
    public String uidCliente;
    public String servicioId;
    public String staffId;     // puede ser null
    public String fecha;       // "YYYY-MM-DD"
    public String hora;        // "HH:mm"
    public Double precio;
    public Double deposito;
    public String estado;      // pendiente|confirmada|rechazada
    public String voucherUrl;  // url absoluta
    public String codigo;      // opcional
    public Timestamp createdAt;

    public static Reserva from(DocumentSnapshot d) {
        Reserva r = new Reserva();
        r.id = d.getId();
        r.uidCliente = d.getString("uidCliente");
        r.servicioId = d.getString("servicioId");
        r.staffId    = d.getString("staffId");
        r.fecha      = d.getString("fecha");
        r.hora       = d.getString("hora");
        r.precio     = d.getDouble("precio");
        r.deposito   = d.getDouble("deposito");
        r.estado     = d.getString("estado");
        r.voucherUrl = d.getString("voucherUrl");
        r.codigo     = d.getString("codigo");
        r.createdAt  = d.getTimestamp("createdAt");
        return r;
    }
}
