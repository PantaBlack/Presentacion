package InventarioT3Package;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;

public class MenuPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private InventarioDB inventarioDB;
    private JTable table;
    private DefaultTableModel tableModel;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                MenuPrincipal frame = new MenuPrincipal();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public MenuPrincipal() {
        setBackground(SystemColor.textText);
        setTitle("Sistema de Inventario - Base de Datos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1235, 550);
        contentPane = new JPanel();
        contentPane.setBackground(SystemColor.activeCaption);
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(10, 10));
        inventarioDB = new InventarioDB();

        String[] columnNames = {"Nombre", "Código", "Precio", "Cantidad", "Categoría", "Vendidos"};
        tableModel = new DefaultTableModel(columnNames, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        contentPane.add(scrollPane, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel();
        panelBotones.setBackground(SystemColor.activeCaption);
        panelBotones.setLayout(new GridLayout(3, 5, 5, 5));
        contentPane.add(panelBotones, BorderLayout.SOUTH);

        JButton btnAgregar = new JButton("Agregar producto");
        btnAgregar.setBackground(SystemColor.info);
        JButton btnMostrar = new JButton("Mostrar inventario");
        btnMostrar.setBackground(SystemColor.info);
        JButton btnBuscar = new JButton("Buscar por nombre");
        btnBuscar.setBackground(SystemColor.info);
        JButton btnActualizar = new JButton("Actualizar stock");
        btnActualizar.setBackground(SystemColor.info);
        JButton btnEliminar = new JButton("Eliminar producto");
        btnEliminar.setBackground(SystemColor.info);
        JButton btnRango = new JButton("Buscar por rango de precios");
        btnRango.setBackground(SystemColor.info);
        JButton btnCategoria = new JButton("Ver por categoría");
        btnCategoria.setBackground(SystemColor.info);
        JButton btnOrdenar = new JButton("Ordenar por precio");
        btnOrdenar.setBackground(SystemColor.info);
        JButton btnAgotados = new JButton("Reporte agotados");
        btnAgotados.setBackground(SystemColor.info);
        JButton btnSalir = new JButton("Salir");
        btnSalir.setBackground(SystemColor.info);
        JButton btnEditar = new JButton("Editar producto");
        btnEditar.setBackground(SystemColor.info);
        JButton btnVender = new JButton("Vender producto");
        btnVender.setBackground(SystemColor.info);
        JButton btnReporteVentas = new JButton("Reporte de ventas");
        btnReporteVentas.setBackground(SystemColor.info);
        JButton btnConexion = new JButton("Probar conexión");
        btnConexion.setBackground(SystemColor.info);

        panelBotones.add(btnAgregar);
        panelBotones.add(btnMostrar);
        panelBotones.add(btnBuscar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnRango);
        panelBotones.add(btnCategoria);
        panelBotones.add(btnOrdenar);
        panelBotones.add(btnAgotados);
        panelBotones.add(btnSalir);
        panelBotones.add(btnEditar);
        panelBotones.add(btnVender);
        panelBotones.add(btnReporteVentas);
        panelBotones.add(btnConexion);

        btnAgregar.addActionListener(e -> agregarProducto());
        btnMostrar.addActionListener(e -> mostrarInventario());
        btnBuscar.addActionListener(e -> buscarProducto());
        btnActualizar.addActionListener(e -> actualizarStock());
        btnEliminar.addActionListener(e -> eliminarProducto());
        btnRango.addActionListener(e -> buscarPorRangoPrecio());
        btnCategoria.addActionListener(e -> mostrarPorCategoria());
        btnOrdenar.addActionListener(e -> ordenarPorPrecio());
        btnAgotados.addActionListener(e -> reporteAgotados());
        btnSalir.addActionListener(e -> System.exit(0));    
        btnEditar.addActionListener(e -> editarProducto());
        btnVender.addActionListener(e -> venderProducto());
        btnReporteVentas.addActionListener(e -> reporteVentas());
        btnConexion.addActionListener(e -> probarConexion());

        mostrarInventario();
    }

    private void agregarProducto() {
        JTextField nombreField = new JTextField();
        JTextField codigoField = new JTextField();
        JTextField precioField = new JTextField();
        JTextField cantidadField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Nombre:"));
        panel.add(nombreField);
        panel.add(new JLabel("Código único:"));
        panel.add(codigoField);
        panel.add(new JLabel("Precio:"));
        panel.add(precioField);
        panel.add(new JLabel("Cantidad:"));
        panel.add(cantidadField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Agregar Producto", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String nombre = nombreField.getText().trim();
                String codigo = codigoField.getText().trim();
                double precio = Double.parseDouble(precioField.getText().trim());
                int cantidad = Integer.parseInt(cantidadField.getText().trim());

                if (nombre.isEmpty() || codigo.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Nombre y código son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (inventarioDB.codigoExiste(codigo)) {
                    JOptionPane.showMessageDialog(this, "El código ya existe.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                inventarioDB.agregarProducto(codigo, nombre, precio, cantidad);
                mostrarInventario();
                JOptionPane.showMessageDialog(this, "Producto agregado con éxito.");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Precio o cantidad inválidos.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error de base de datos: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void mostrarInventario() {
        try {
            ArrayList<Producto> productos = inventarioDB.getProductos();
            actualizarTabla(productos);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar inventario: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buscarProducto() {
        String nombre = JOptionPane.showInputDialog(this, "Ingrese nombre a buscar:");
        if (nombre != null && !nombre.trim().isEmpty()) {
            try {
                ArrayList<Producto> encontrados = inventarioDB.buscarProductoPorNombre(nombre.trim());
                if (encontrados.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Producto no encontrado.");
                } else {
                    actualizarTabla(encontrados);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error en la búsqueda: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void actualizarStock() {
        String codigo = JOptionPane.showInputDialog(this, "Ingrese código del producto:");
        if (codigo != null && !codigo.trim().isEmpty()) {
            try {
                Producto p = inventarioDB.buscarPorCodigo(codigo.trim());
                if (p == null) {
                    JOptionPane.showMessageDialog(this, "Producto no encontrado.");
                    return;
                }

                String nuevaCantidadStr = JOptionPane.showInputDialog(this, "Nueva cantidad para " + p.nombre + ":");
                int nuevaCantidad = Integer.parseInt(nuevaCantidadStr);

                inventarioDB.actualizarStock(codigo.trim(), nuevaCantidad);
                mostrarInventario();
                JOptionPane.showMessageDialog(this, "Stock actualizado.");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Cantidad inválida.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al actualizar stock: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void eliminarProducto() {
        String codigo = JOptionPane.showInputDialog(this, "Código del producto a eliminar:");
        if (codigo != null && !codigo.trim().isEmpty()) {
            try {
                Producto p = inventarioDB.buscarPorCodigo(codigo.trim());
                if (p == null) {
                    JOptionPane.showMessageDialog(this, "Producto no encontrado.");
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(this, "¿Está seguro de eliminar el producto " + p.nombre + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    inventarioDB.eliminarProducto(codigo.trim());
                    mostrarInventario();
                    JOptionPane.showMessageDialog(this, "Producto eliminado.");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al eliminar producto: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void buscarPorRangoPrecio() {
        JTextField minField = new JTextField();
        JTextField maxField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Precio mínimo:"));
        panel.add(minField);
        panel.add(new JLabel("Precio máximo:"));
        panel.add(maxField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Buscar por rango de precios", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                double min = Double.parseDouble(minField.getText().trim());
                double max = Double.parseDouble(maxField.getText().trim());
                ArrayList<Producto> encontrados = inventarioDB.buscarPorRangoPrecio(min, max);
                
                if (encontrados.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No se encontraron productos en ese rango.");
                } else {
                    actualizarTabla(encontrados);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Precios inválidos.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error en la búsqueda: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void mostrarPorCategoria() {
        String[] categorias = {"Económico", "Estándar", "Premium"};
        String categoria = (String) JOptionPane.showInputDialog(this, "Seleccione categoría:",
                "Categoría", JOptionPane.QUESTION_MESSAGE, null, categorias, categorias[0]);
        if (categoria != null) {
            try {
                ArrayList<Producto> encontrados = inventarioDB.mostrarPorCategoria(categoria);
                if (encontrados.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No hay productos en esta categoría.");
                } else {
                    actualizarTabla(encontrados);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al buscar por categoría: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void ordenarPorPrecio() {
        try {
            ArrayList<Producto> productos = inventarioDB.ordenarPorPrecio();
            actualizarTabla(productos);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al ordenar productos: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reporteAgotados() {
        try {
            ArrayList<Producto> agotados = inventarioDB.reporteAgotados();
            if (agotados.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay productos agotados.");
            } else {
                actualizarTabla(agotados);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al generar reporte: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarTabla(ArrayList<Producto> lista) {
        tableModel.setRowCount(0);
        for (Producto p : lista) {
            tableModel.addRow(new Object[]{p.nombre, p.codigo, p.precio, p.cantidad, p.categoria, p.vendidos});
        }
    }

    private void editarProducto() {
        String codigo = JOptionPane.showInputDialog(this, "Ingrese código del producto:");
        if (codigo != null && !codigo.trim().isEmpty()) {
            try {
                Producto p = inventarioDB.buscarPorCodigo(codigo.trim());
                if (p == null) {
                    JOptionPane.showMessageDialog(this, "Producto no encontrado.");
                    return;
                }

                JTextField nombreField = new JTextField(p.nombre);
                JTextField precioField = new JTextField(String.valueOf(p.precio));

                JPanel panel = new JPanel(new GridLayout(0, 1));
                panel.add(new JLabel("Nuevo nombre:"));
                panel.add(nombreField);
                panel.add(new JLabel("Nuevo precio:"));
                panel.add(precioField);

                int result = JOptionPane.showConfirmDialog(this, panel, "Editar Producto", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    String nuevoNombre = nombreField.getText().trim();
                    double nuevoPrecio = Double.parseDouble(precioField.getText().trim());

                    inventarioDB.actualizarProducto(codigo.trim(), nuevoNombre, nuevoPrecio);
                    mostrarInventario();
                    JOptionPane.showMessageDialog(this, "Producto actualizado.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Precio inválido.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al editar producto: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void venderProducto() {
        String codigo = JOptionPane.showInputDialog(this, "Ingrese código del producto a vender:");
        if (codigo != null && !codigo.trim().isEmpty()) {
            try {
                Producto p = inventarioDB.buscarPorCodigo(codigo.trim());
                if (p == null) {
                    JOptionPane.showMessageDialog(this, "Producto no encontrado.");
                    return;
                }

                String cantidadStr = JOptionPane.showInputDialog(this, "Ingrese cantidad a vender (Stock disponible: " + p.cantidad + "):");
                int cantidad = Integer.parseInt(cantidadStr.trim());
                
                if (cantidad <= 0 || cantidad > p.cantidad) {
                    JOptionPane.showMessageDialog(this, "Cantidad inválida o insuficiente.");
                    return;
                }

                inventarioDB.registrarVenta(codigo.trim(), cantidad);
                mostrarInventario();
                JOptionPane.showMessageDialog(this, "Venta realizada correctamente.");
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Cantidad inválida.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al registrar venta: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void reporteVentas() {
        try {
            ArrayList<Producto> vendidos = inventarioDB.getVendidos();
            if (vendidos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay ventas registradas.");
            } else {
                DefaultTableModel ventasModel = new DefaultTableModel(new String[]{"Nombre", "Código", "Vendidos", "Precio Unitario", "Total (S/.)"}, 0);
                double totalGeneral = 0;

                for (Producto p : vendidos) {
                    double totalProducto = p.vendidos * p.precio;
                    totalGeneral += totalProducto;
                    ventasModel.addRow(new Object[]{
                        p.nombre, 
                        p.codigo, 
                        p.vendidos, 
                        String.format("S/ %.2f", p.precio),
                        String.format("S/ %.2f", totalProducto)
                    });
                }

                JTable tablaVentas = new JTable(ventasModel);
                tablaVentas.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                JScrollPane scrollPane = new JScrollPane(tablaVentas);
                scrollPane.setPreferredSize(new Dimension(600, 300));

                JPanel panel = new JPanel(new BorderLayout());
                panel.add(scrollPane, BorderLayout.CENTER);

                JLabel totalLabel = new JLabel("Total general de ventas: S/ " + String.format("%.2f", totalGeneral));
                totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                totalLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 14f));
                panel.add(totalLabel, BorderLayout.SOUTH);

                JOptionPane.showMessageDialog(this, panel, "Reporte de Ventas", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al generar reporte de ventas: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void probarConexion() {
        try {
            DatabaseConnection.getConnection();
            JOptionPane.showMessageDialog(this, "Conexión a la base de datos exitosa.", "Conexión OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión: " + ex.getMessage(), "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }
}