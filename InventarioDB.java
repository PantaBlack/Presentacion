package InventarioT3Package;

import java.sql.*;
import java.util.ArrayList;

public class InventarioDB {
    
    public void agregarProducto(String codigo, String nombre, double precio, int cantidad) throws SQLException {
        int idCategoria = obtenerIdCategoria(precio);
        
        String sql = "INSERT INTO Productos (codigo, nombre, precio, cantidad, vendidos, idCategoria) VALUES (?, ?, ?, ?, 0, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, codigo);
            stmt.setString(2, nombre);
            stmt.setDouble(3, precio);
            stmt.setInt(4, cantidad);
            stmt.setInt(5, idCategoria);
            
            stmt.executeUpdate();
            
            registrarHistorial(obtenerIdProductoPorCodigo(codigo), "AGREGAR", 0, cantidad);
        }
    }
    
    public boolean codigoExiste(String codigo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Productos WHERE codigo = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, codigo);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }
    
    public ArrayList<Producto> getProductos() throws SQLException {
        ArrayList<Producto> productos = new ArrayList<>();
        String sql = "SELECT p.idProducto, p.codigo, p.nombre, p.precio, p.cantidad, p.vendidos, p.idCategoria, c.nombreCategoria " +
                    "FROM Productos p INNER JOIN Categorias c ON p.idCategoria = c.idCategoria";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                productos.add(new Producto(
                    rs.getInt("idProducto"),
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getDouble("precio"),
                    rs.getInt("cantidad"),
                    rs.getInt("vendidos"),
                    rs.getInt("idCategoria"),
                    rs.getString("nombreCategoria")
                ));
            }
        }
        return productos;
    }
    
    public Producto buscarPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT p.idProducto, p.codigo, p.nombre, p.precio, p.cantidad, p.vendidos, p.idCategoria, c.nombreCategoria " +
                    "FROM Productos p INNER JOIN Categorias c ON p.idCategoria = c.idCategoria WHERE p.codigo = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, codigo);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new Producto(
                    rs.getInt("idProducto"),
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getDouble("precio"),
                    rs.getInt("cantidad"),
                    rs.getInt("vendidos"),
                    rs.getInt("idCategoria"),
                    rs.getString("nombreCategoria")
                );
            }
        }
        return null;
    }
    
    public ArrayList<Producto> buscarProductoPorNombre(String nombre) throws SQLException {
        ArrayList<Producto> productos = new ArrayList<>();
        String sql = "SELECT p.idProducto, p.codigo, p.nombre, p.precio, p.cantidad, p.vendidos, p.idCategoria, c.nombreCategoria " +
                    "FROM Productos p INNER JOIN Categorias c ON p.idCategoria = c.idCategoria WHERE p.nombre LIKE ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + nombre + "%");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                productos.add(new Producto(
                    rs.getInt("idProducto"),
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getDouble("precio"),
                    rs.getInt("cantidad"),
                    rs.getInt("vendidos"),
                    rs.getInt("idCategoria"),
                    rs.getString("nombreCategoria")
                ));
            }
        }
        return productos;
    }
    
    public void eliminarProducto(String codigo) throws SQLException {
        Producto producto = buscarPorCodigo(codigo);
        if (producto == null) {
            throw new SQLException("Producto no encontrado con código: " + codigo);
        }
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            String sqlHistorial = "DELETE FROM HistorialInventario WHERE idProducto = ?";
            try (PreparedStatement stmtHistorial = conn.prepareStatement(sqlHistorial)) {
                stmtHistorial.setInt(1, producto.idProducto);
                stmtHistorial.executeUpdate();
            }
            
            String sqlVentas = "DELETE FROM Ventas WHERE idProducto = ?";
            try (PreparedStatement stmtVentas = conn.prepareStatement(sqlVentas)) {
                stmtVentas.setInt(1, producto.idProducto);
                stmtVentas.executeUpdate();
            }
            
            String sqlProducto = "DELETE FROM Productos WHERE codigo = ?";
            try (PreparedStatement stmtProducto = conn.prepareStatement(sqlProducto)) {
                stmtProducto.setString(1, codigo);
                int rowsAffected = stmtProducto.executeUpdate();
                
                if (rowsAffected == 0) {
                    throw new SQLException("No se pudo eliminar el producto");
                }
            }
            
            conn.commit();
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                }
            }
        }
    }
    
    public ArrayList<Producto> buscarPorRangoPrecio(double min, double max) throws SQLException {
        ArrayList<Producto> productos = new ArrayList<>();
        String sql = "SELECT p.idProducto, p.codigo, p.nombre, p.precio, p.cantidad, p.vendidos, p.idCategoria, c.nombreCategoria " +
                    "FROM Productos p INNER JOIN Categorias c ON p.idCategoria = c.idCategoria WHERE p.precio BETWEEN ? AND ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDouble(1, min);
            stmt.setDouble(2, max);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                productos.add(new Producto(
                    rs.getInt("idProducto"),
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getDouble("precio"),
                    rs.getInt("cantidad"),
                    rs.getInt("vendidos"),
                    rs.getInt("idCategoria"),
                    rs.getString("nombreCategoria")
                ));
            }
        }
        return productos;
    }
    
    public ArrayList<Producto> mostrarPorCategoria(String categoria) throws SQLException {
        ArrayList<Producto> productos = new ArrayList<>();
        String sql = "SELECT p.idProducto, p.codigo, p.nombre, p.precio, p.cantidad, p.vendidos, p.idCategoria, c.nombreCategoria " +
                    "FROM Productos p INNER JOIN Categorias c ON p.idCategoria = c.idCategoria WHERE c.nombreCategoria = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, categoria);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                productos.add(new Producto(
                    rs.getInt("idProducto"),
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getDouble("precio"),
                    rs.getInt("cantidad"),
                    rs.getInt("vendidos"),
                    rs.getInt("idCategoria"),
                    rs.getString("nombreCategoria")
                ));
            }
        }
        return productos;
    }
    
    public ArrayList<Producto> ordenarPorPrecio() throws SQLException {
        ArrayList<Producto> productos = new ArrayList<>();
        String sql = "SELECT p.idProducto, p.codigo, p.nombre, p.precio, p.cantidad, p.vendidos, p.idCategoria, c.nombreCategoria " +
                    "FROM Productos p INNER JOIN Categorias c ON p.idCategoria = c.idCategoria ORDER BY p.precio ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                productos.add(new Producto(
                    rs.getInt("idProducto"),
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getDouble("precio"),
                    rs.getInt("cantidad"),
                    rs.getInt("vendidos"),
                    rs.getInt("idCategoria"),
                    rs.getString("nombreCategoria")
                ));
            }
        }
        return productos;
    }
    
    public ArrayList<Producto> reporteAgotados() throws SQLException {
        ArrayList<Producto> productos = new ArrayList<>();
        String sql = "SELECT p.idProducto, p.codigo, p.nombre, p.precio, p.cantidad, p.vendidos, p.idCategoria, c.nombreCategoria " +
                    "FROM Productos p INNER JOIN Categorias c ON p.idCategoria = c.idCategoria WHERE p.cantidad = 0";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                productos.add(new Producto(
                    rs.getInt("idProducto"),
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getDouble("precio"),
                    rs.getInt("cantidad"),
                    rs.getInt("vendidos"),
                    rs.getInt("idCategoria"),
                    rs.getString("nombreCategoria")
                ));
            }
        }
        return productos;
    }
    
    public void registrarVenta(String codigo, int cantidadVendida) throws SQLException {
        if (cantidadVendida <= 0) {
            throw new SQLException("La cantidad a vender debe ser mayor que 0");
        }
        
        Producto producto = buscarPorCodigo(codigo);
        if (producto == null) {
            throw new SQLException("Producto no encontrado con código: " + codigo);
        }
        
        if (producto.cantidad < cantidadVendida) {
            throw new SQLException("Stock insuficiente. Disponible: " + producto.cantidad + ", Solicitado: " + cantidadVendida);
        }
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            String sqlUpdateProducto = "UPDATE Productos SET cantidad = cantidad - ?, vendidos = vendidos + ? WHERE codigo = ?";
            try (PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdateProducto)) {
                stmtUpdate.setInt(1, cantidadVendida);
                stmtUpdate.setInt(2, cantidadVendida);
                stmtUpdate.setString(3, codigo);
                
                int rowsAffected = stmtUpdate.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("No se pudo actualizar el producto");
                }
            }
            
            String sqlVenta = "INSERT INTO Ventas (idProducto, cantidadVendida, fechaVenta) VALUES (?, ?, GETDATE())";
            try (PreparedStatement stmtVenta = conn.prepareStatement(sqlVenta)) {
                stmtVenta.setInt(1, producto.idProducto);
                stmtVenta.setInt(2, cantidadVendida);
                stmtVenta.executeUpdate();
            }
            
            registrarHistorial(conn, producto.idProducto, "VENTA", producto.cantidad, producto.cantidad - cantidadVendida);
            
            conn.commit();
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                }
            }
        }
    }

    public ArrayList<Producto> getVendidos() throws SQLException {
        ArrayList<Producto> productos = new ArrayList<>();
        String sql = "SELECT p.idProducto, p.codigo, p.nombre, p.precio, p.cantidad, p.vendidos, p.idCategoria, c.nombreCategoria " +
                    "FROM Productos p INNER JOIN Categorias c ON p.idCategoria = c.idCategoria WHERE p.vendidos > 0";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                productos.add(new Producto(
                    rs.getInt("idProducto"),
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getDouble("precio"),
                    rs.getInt("cantidad"),
                    rs.getInt("vendidos"),
                    rs.getInt("idCategoria"),
                    rs.getString("nombreCategoria")
                ));
            }
        }
        return productos;
    }
    
    public void actualizarProducto(String codigo, String nuevoNombre, double nuevoPrecio) throws SQLException {
        Producto productoAntes = buscarPorCodigo(codigo);
        if (productoAntes != null) {
            int nuevaCategoria = obtenerIdCategoria(nuevoPrecio);
            
            String sql = "UPDATE Productos SET nombre = ?, precio = ?, idCategoria = ? WHERE codigo = ?";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, nuevoNombre);
                stmt.setDouble(2, nuevoPrecio);
                stmt.setInt(3, nuevaCategoria);
                stmt.setString(4, codigo);
                
                stmt.executeUpdate();
                
                registrarHistorial(productoAntes.idProducto, "EDITAR", productoAntes.cantidad, productoAntes.cantidad);
            }
        }
    }
    
    public void actualizarStock(String codigo, int nuevaCantidad) throws SQLException {
        Producto producto = buscarPorCodigo(codigo);
        if (producto != null) {
            String sql = "UPDATE Productos SET cantidad = ? WHERE codigo = ?";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, nuevaCantidad);
                stmt.setString(2, codigo);
                
                stmt.executeUpdate();
                
                registrarHistorial(producto.idProducto, "ACTUALIZAR_STOCK", producto.cantidad, nuevaCantidad);
            }
        }
    }
    
    private int obtenerIdCategoria(double precio) {
        if (precio < 50) return 1; // Económico
        else if (precio <= 200) return 2; // Estándar
        else return 3; // Premium
    }
    
    private int obtenerIdProductoPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT idProducto FROM Productos WHERE codigo = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, codigo);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("idProducto");
            }
        }
        return -1;
    }
    
    private void registrarHistorial(int idProducto, String accion, int cantidadAntes, int cantidadDespues) throws SQLException {
        String sql = "INSERT INTO HistorialInventario (idProducto, accion, cantidadAntes, cantidadDespues, fecha) VALUES (?, ?, ?, ?, GETDATE())";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idProducto);
            stmt.setString(2, accion);
            stmt.setInt(3, cantidadAntes);
            stmt.setInt(4, cantidadDespues);
            
            stmt.executeUpdate();
        }
    }
    
    private void registrarHistorial(Connection conn, int idProducto, String accion, int cantidadAntes, int cantidadDespues) throws SQLException {
        String sql = "INSERT INTO HistorialInventario (idProducto, accion, cantidadAntes, cantidadDespues, fecha) VALUES (?, ?, ?, ?, GETDATE())";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idProducto);
            stmt.setString(2, accion);
            stmt.setInt(3, cantidadAntes);
            stmt.setInt(4, cantidadDespues);
            
            stmt.executeUpdate();
        }
    }
}