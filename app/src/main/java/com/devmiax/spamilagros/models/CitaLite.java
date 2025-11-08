package com.devmiax.spamilagros.models;

import com.google.firebase.firestore.DocumentSnapshot;

public class CitaLite {
    public String id;
    public String uidCliente;
    public String servicioId;
    public String staffId;
    public String fecha;
    public String hora;
    public Double precio;

    public static CitaLite from(DocumentSnapshot d) {
        CitaLite c = new CitaLite();
        c.id = d.getId();
        c.uidCliente = d.getString("uidCliente");
        c.servicioId = d.getString("servicioId");
        c.staffId    = d.getString("staffId");
        c.fecha      = d.getString("fecha");
        c.hora       = d.getString("hora");
        c.precio     = d.getDouble("precio");
        return c;
    }
}
