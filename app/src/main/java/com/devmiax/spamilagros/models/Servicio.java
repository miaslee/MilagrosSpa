package com.devmiax.spamilagros.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class Servicio {
    public String id;
    public String nombre;
    public String categoria;
    public int duracionMin;
    public double precio;
    public String descripcion;
    public Timestamp createdAt;

    public Servicio() {} // Necesario para Firestore

    public Servicio(String id, String nombre, String categoria, int duracionMin, double precio, String descripcion, Timestamp createdAt) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.duracionMin = duracionMin;
        this.precio = precio;
        this.descripcion = descripcion;
        this.createdAt = createdAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", nombre);
        m.put("categoria", categoria);
        m.put("duracionMin", duracionMin);
        m.put("precio", precio);
        m.put("descripcion", descripcion);
        if (createdAt != null) m.put("createdAt", createdAt);
        return m;
    }

    public static Servicio from(DocumentSnapshot d) {
        Servicio s = new Servicio();
        s.id = d.getId();
        s.nombre = d.getString("nombre");
        s.categoria = d.getString("categoria");
        Long dur = d.getLong("duracionMin");
        s.duracionMin = dur != null ? dur.intValue() : 0;
        Double pr = d.getDouble("precio");
        s.precio = pr != null ? pr : 0.0;
        s.descripcion = d.getString("descripcion");
        s.createdAt = d.getTimestamp("createdAt");
        return s;
    }
}