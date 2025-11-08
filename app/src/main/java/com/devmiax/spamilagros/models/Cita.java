package com.devmiax.spamilagros.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

public class Cita {
    public String id;          // id del doc (slotId o cualquiera)
    public String uidCliente;
    public String servicioId;
    public String staffId;
    public String fecha;       // "YYYY-MM-DD"
    public String hora;        // "HH:mm"
    public Double precio;
    public String estado;      // opcional: si la guardas también aquí
    public String codigo;      // opcional si lo copias de reserva
    public Timestamp createdAt;

    public static Cita from(DocumentSnapshot d) {
        Cita c = new Cita();
        c.id = d.getId();
        c.uidCliente = d.getString("uidCliente");
        c.servicioId = d.getString("servicioId");
        c.staffId    = d.getString("staffId");
        c.fecha      = d.getString("fecha");
        c.hora       = d.getString("hora");
        c.precio     = d.getDouble("precio");
        c.estado     = d.getString("estado"); // si lo usas en citas
        c.codigo     = d.getString("codigo"); // si lo copiaste
        c.createdAt  = d.getTimestamp("createdAt");
        return c;
    }
}