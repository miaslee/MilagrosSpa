package com.devmiax.spamilagros.models;
import com.google.firebase.firestore.DocumentSnapshot;
public class ServicioCliente {

    public String id;
    public String nombre;
    public String categoria;
    public int duracionMin;
    public double precio;
    public String descripcion;
    public Boolean activo; // opcional

    public static ServicioCliente from(DocumentSnapshot d) {
        ServicioCliente s = new ServicioCliente();
        s.id = d.getId();
        s.nombre = d.getString("nombre");
        s.categoria = d.getString("categoria");
        Long dur = d.getLong("duracionMin");
        s.duracionMin = dur != null ? dur.intValue() : 0;
        Double pr = d.getDouble("precio");
        s.precio = pr != null ? pr : 0.0;
        s.descripcion = d.getString("descripcion");
        Boolean ac = d.getBoolean("activo");
        s.activo = ac == null ? true : ac;
        return s;
    }
}
