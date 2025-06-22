package InventarioT3Package;

import java.util.ArrayList;
import java.util.Comparator;

public class Inventario {
    private final ArrayList<Producto> productos = new ArrayList<>();

    public void agregarProducto(String nombre, double precio, int cantidad, String codigo) {
        Producto nuevo = new Producto(nombre, precio, cantidad, codigo);
        productos.add(nuevo);
    }

    public boolean codigoExiste(String codigo) {
        for (Producto p : productos) {
            if (p.codigo.equals(codigo)) return true;
        }
        return false;
    }

    public ArrayList<Producto> getProductos() {
        return productos;
    }

    public Producto buscarPorCodigo(String codigo) {
        for (Producto p : productos) {
            if (p.codigo.equals(codigo)) return p;
        }
        return null;
    }

    public ArrayList<Producto> buscarProductoPorNombre(String nombre) {
        ArrayList<Producto> encontrados = new ArrayList<>();
        String nombreLower = nombre.toLowerCase();
        for (Producto p : productos) {
            if (p.nombre.toLowerCase().contains(nombreLower)) {
                encontrados.add(p);
            }
        }
        return encontrados;
    }

    public void eliminarProducto(String codigo) {
        productos.removeIf(p -> p.codigo.equals(codigo));
    }

    public ArrayList<Producto> buscarPorRangoPrecio(double min, double max) {
        ArrayList<Producto> encontrados = new ArrayList<>();
        for (Producto p : productos) {
            if (p.precio >= min && p.precio <= max) {
                encontrados.add(p);
            }
        }
        return encontrados;
    }

    public ArrayList<Producto> mostrarPorCategoria(String categoria) {
        ArrayList<Producto> encontrados = new ArrayList<>();
        String categoriaLower = categoria.toLowerCase();
        for (Producto p : productos) {
            if (p.categoria.toLowerCase().equals(categoriaLower)) {
                encontrados.add(p);
            }
        }
        return encontrados;
    }

    public void ordenarPorPrecio() {
        productos.sort(Comparator.comparingDouble(p -> p.precio));
    }

    public ArrayList<Producto> reporteAgotados() {
        ArrayList<Producto> agotados = new ArrayList<>();
        for (Producto p : productos) {
            if (p.cantidad == 0) {
                agotados.add(p);
            }
        }
        return agotados;
    }
}
