package InventarioT3Package;

public class Producto {
    public String nombre;
    public double precio;
    public int cantidad;
    public String categoria;
    public String codigo;

    public Producto(String nombre, double precio, int cantidad, String codigo) {
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
        this.codigo = codigo;
        this.categoria = categorizar(precio);
    }

    private String categorizar(double precio) {
        if (precio < 50) return "Económico";
        else if (precio <= 200) return "Estándar";
        else return "Premium";
    }
}
