package InventarioT3Package;

public class Producto {
    public int idProducto;
    public String codigo;
    public String nombre;
    public double precio;
    public int cantidad;
    public int vendidos;
    public int idCategoria;
    public String categoria;

    public Producto(int idProducto, String codigo, String nombre, double precio, int cantidad, int vendidos, int idCategoria, String categoria) {
        this.idProducto = idProducto;
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
        this.vendidos = vendidos;
        this.idCategoria = idCategoria;
        this.categoria = categoria;
    }

    public Producto(String codigo, String nombre, double precio, int cantidad, int idCategoria) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
        this.vendidos = 0;
        this.idCategoria = idCategoria;
    }
}