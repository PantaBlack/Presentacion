package InventarioT3Package;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class MenuPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private Inventario inventario;
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
        setTitle("Sistema de Inventario");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 800, 500);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(10, 10));

        inventario = new Inventario();

        // Tabla de productos
        String[] columnNames = {"Nombre", "Código", "Precio", "Cantidad", "Categoría"};
        tableModel = new DefaultTableModel(columnNames, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // Panel de botones
        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new GridLayout(2, 5, 5, 5));
        contentPane.add(panelBotones, BorderLayout.SOUTH);

        // Botones de menú
        JButton btnAgregar = new JButton("Agregar producto");
        btnAgregar.setBackground(Color.WHITE);
        JButton btnMostrar = new JButton("Mostrar inventario");
        JButton btnBuscar = new JButton("Buscar por nombre");
        JButton btnActualizar = new JButton("Actualizar stock");
        JButton btnEliminar = new JButton("Eliminar producto");
        JButton btnRango = new JButton("Buscar por rango de precios");
        JButton btnCategoria = new JButton("Ver por categoría");
        JButton btnOrdenar = new JButton("Ordenar por precio");
        JButton btnAgotados = new JButton("Reporte agotados");
        JButton btnSalir = new JButton("Salir");

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

        // Acciones de los botones
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
    }

    // Métodos para cada acción

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
            String nombre = nombreField.getText().trim();
            String codigo = codigoField.getText().trim();
            double precio;
            int cantidad;
            try {
                precio = Double.parseDouble(precioField.getText().trim());
                cantidad = Integer.parseInt(cantidadField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Precio o cantidad inválidos.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (nombre.isEmpty() || codigo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nombre y código son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (inventario.codigoExiste(codigo)) {
                JOptionPane.showMessageDialog(this, "El código ya existe.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            inventario.agregarProducto(nombre, precio, cantidad, codigo);
            mostrarInventario();
            JOptionPane.showMessageDialog(this, "Producto agregado con éxito.");
        }
    }

    private void mostrarInventario() {
        actualizarTabla(inventario.getProductos());
    }

    private void buscarProducto() {
        String nombre = JOptionPane.showInputDialog(this, "Ingrese nombre a buscar:");
        if (nombre != null && !nombre.trim().isEmpty()) {
            ArrayList<Producto> encontrados = inventario.buscarProductoPorNombre(nombre.trim());
            if (encontrados.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Producto no encontrado.");
            } else {
                actualizarTabla(encontrados);
            }
        }
    }

    private void actualizarStock() {
        String codigo = JOptionPane.showInputDialog(this, "Ingrese código del producto:");
        if (codigo != null && !codigo.trim().isEmpty()) {
            Producto p = inventario.buscarPorCodigo(codigo.trim());
            if (p == null) {
                JOptionPane.showMessageDialog(this, "Producto no encontrado.");
                return;
            }
            String nuevaCantidadStr = JOptionPane.showInputDialog(this, "Nueva cantidad para " + p.nombre + ":");
            try {
                int nuevaCantidad = Integer.parseInt(nuevaCantidadStr);
                p.cantidad = nuevaCantidad;
                mostrarInventario();
                JOptionPane.showMessageDialog(this, "Stock actualizado.");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Cantidad inválida.");
            }
        }
    }

    private void eliminarProducto() {
        String codigo = JOptionPane.showInputDialog(this, "Código del producto a eliminar:");
        if (codigo != null && !codigo.trim().isEmpty()) {
            Producto p = inventario.buscarPorCodigo(codigo.trim());
            if (p == null) {
                JOptionPane.showMessageDialog(this, "Producto no encontrado.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "¿Está seguro de eliminar el producto " + p.nombre + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                inventario.eliminarProducto(codigo.trim());
                mostrarInventario();
                JOptionPane.showMessageDialog(this, "Producto eliminado.");
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
                ArrayList<Producto> encontrados = inventario.buscarPorRangoPrecio(min, max);
                if (encontrados.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No se encontraron productos en ese rango.");
                } else {
                    actualizarTabla(encontrados);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Precios inválidos.");
            }
        }
    }

    private void mostrarPorCategoria() {
        String[] categorias = {"Económico", "Estándar", "Premium"};
        String categoria = (String) JOptionPane.showInputDialog(this, "Seleccione categoría:",
                "Categoría", JOptionPane.QUESTION_MESSAGE, null, categorias, categorias[0]);
        if (categoria != null) {
            ArrayList<Producto> encontrados = inventario.mostrarPorCategoria(categoria);
            if (encontrados.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay productos en esta categoría.");
            } else {
                actualizarTabla(encontrados);
            }
        }
    }

    private void ordenarPorPrecio() {
        inventario.ordenarPorPrecio();
        mostrarInventario();
    }

    private void reporteAgotados() {
        ArrayList<Producto> agotados = inventario.reporteAgotados();
        if (agotados.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay productos agotados.");
        } else {
            actualizarTabla(agotados);
        }
    }

    private void actualizarTabla(ArrayList<Producto> lista) {
        tableModel.setRowCount(0);
        for (Producto p : lista) {
            tableModel.addRow(new Object[]{p.nombre, p.codigo, p.precio, p.cantidad, p.categoria});
        }
    }
}
