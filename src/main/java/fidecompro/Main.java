package fidecompro;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

/** Fidecompro - Avance 2. Aplicación académica Java Swing sin frameworks. */
public class Main {
    static final Path ARCHIVO = Paths.get(System.getProperty("user.home"), ".fidecompro", "datos.ser");
    static Datos datos;
    static Usuario sesion;
    static JFrame ventana;
    static JTabbedPane pestañas;
    static final BigDecimal IVA = new BigDecimal("0.13"); // configurable en el código, demostración académica









    static String hash(String s){try{byte[] d=MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:d)b.append(String.format("%02x",x));return b.toString();}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
    static void guardar(){
        try {
            Files.createDirectories(ARCHIVO.getParent());
            Path temporal = Files.createTempFile(ARCHIVO.getParent(), "datos-", ".tmp");
            try {
                try(ObjectOutputStream o = new ObjectOutputStream(Files.newOutputStream(temporal))) { o.writeObject(datos); }
                try { Files.move(temporal, ARCHIVO, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch(AtomicMoveNotSupportedException ex) { Files.move(temporal, ARCHIVO, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temporal); }
        } catch(IOException ex) { error("No se pudieron guardar los datos en " + ARCHIVO + ": " + ex.getMessage()); }
    }
    static void cargar(){
        if (!Files.exists(ARCHIVO)) { iniciarDatosNuevos(); return; }
        try(ObjectInputStream o = new ObjectInputStream(Files.newInputStream(ARCHIVO))) {
            datos = (Datos)o.readObject();
            if(datos == null || datos.usuarios == null || datos.clientes == null || datos.productos == null || datos.facturas == null)
                throw new IOException("Estructura de datos incompleta");
        } catch(IOException | ClassNotFoundException | ClassCastException ex) {
            String mensaje = "No se puede leer el archivo local:\n" + ARCHIVO +
                "\n\nCausa: " + ex.getClass().getSimpleName() + ": " + ex.getMessage() +
                "\n\nEl archivo original NO se borrara. Puede crear una base nueva para continuar.";
            Object[] opciones = {"Respaldar y crear base nueva", "Salir sin modificar"};
            int respuesta = JOptionPane.showOptionDialog(null,mensaje,"Fidecompro - Recuperacion de datos",
                JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,opciones,opciones[1]);
            if(respuesta != 0) System.exit(1);
            try {
                Path respaldo = ARCHIVO.resolveSibling("datos_respaldo_" +
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").format(LocalDateTime.now()) + ".ser");
                Files.move(ARCHIVO, respaldo);
                JOptionPane.showMessageDialog(null,"Se conservo el archivo anterior en:\n" + respaldo +
                    "\n\nSe creara una base nueva. Los datos anteriores no se importan automaticamente.");
                iniciarDatosNuevos();
            } catch(IOException fallo) {
                JOptionPane.showMessageDialog(null,"No fue posible respaldar el archivo original:\n" + fallo.getMessage() +
                    "\nNo se ha reemplazado la base anterior.","Error",JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
    }
    static void iniciarDatosNuevos(){
        datos = new Datos();
        datos.usuarios.add(new Administrador("Administrador inicial","admin","Admin123!"));
        guardar();
    }
    static void error(String m){JOptionPane.showMessageDialog(ventana,m,"Validación",JOptionPane.WARNING_MESSAGE);}
    static String requerido(String s){if(s==null||s.trim().isEmpty())throw new IllegalArgumentException("Complete todos los campos obligatorios.");return s.trim();}
    /** Normaliza teléfonos de Costa Rica; el campo es opcional. */
    static String validarTelefono(String telefono) {
        String limpio = telefono == null ? "" : telefono.trim().replaceAll("[\\s-]", "");
        if (!limpio.isEmpty() && !limpio.matches("\\d{8}")) {
            throw new IllegalArgumentException("El teléfono debe contener exactamente 8 dígitos.");
        }
        return limpio;
    }

    /** Comprueba el formato del correo electrónico opcional. */
    static String validarCorreo(String correo) {
        String email = correo == null ? "" : correo.trim();
        if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Correo inválido.");
        }
        return email;
    }

    static JTextField campo(JPanel p,String titulo){p.add(new JLabel(titulo));JTextField t=new JTextField(19);p.add(t);return t;}
    static JPanel formulario(){JPanel p=new JPanel(new GridLayout(0,2,8,8));p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));return p;}
    static JButton boton(String t,Runnable r){JButton b=new JButton(t);b.addActionListener(e->{try{r.run();}catch(IllegalArgumentException ex){error(ex.getMessage());}catch(Exception ex){error("Operación fallida: "+ex.getMessage());}});return b;}
    static DefaultTableModel tablaModelo(String... cols){return new DefaultTableModel(cols,0){@Override public boolean isCellEditable(int r,int c){return false;}};}
    static JPanel contenedor(JTable t,JPanel acciones){JPanel p=new JPanel(new BorderLayout(8,8));p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));p.add(new JScrollPane(t),BorderLayout.CENTER);p.add(acciones,BorderLayout.SOUTH);return p;}
    static void login(){JTextField usuario=new JTextField();JPasswordField clave=new JPasswordField();JPanel p=formulario();p.add(new JLabel("Usuario"));p.add(usuario);p.add(new JLabel("Contraseña"));p.add(clave);while(true){int op=JOptionPane.showConfirmDialog(null,p,"Fidecompro | Iniciar sesión",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);if(op!=JOptionPane.OK_OPTION)System.exit(0);String u=usuario.getText().trim();String h=hash(new String(clave.getPassword()));for(Usuario x:datos.usuarios)if(x.activo&&x.usuario.equalsIgnoreCase(u)&&x.claveHash.equals(h)){sesion=x;return;}JOptionPane.showMessageDialog(null,"Credenciales inválidas o cuenta deshabilitada.");clave.setText("");}}
    static void interfaz(){ventana=new JFrame("FIDECOMPRO | Inventario y facturación | "+sesion);ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);ventana.setSize(1060,680);ventana.setLocationRelativeTo(null);pestañas=new JTabbedPane();pestañas.addTab("Clientes",clientes());pestañas.addTab("Productos e inventario",productos());pestañas.addTab("Facturación",facturacion());pestañas.addTab("Historial",historial());if(sesion.administra())pestañas.addTab("Usuarios",usuarios());ventana.add(pestañas);ventana.setVisible(true);}
    static JPanel clientes() {
        DefaultTableModel modelo = tablaModelo("Identificación", "Nombre", "Teléfono", "Correo", "Dirección");
        JTable tabla = new JTable(modelo);
        Runnable refrescar = () -> {
            modelo.setRowCount(0);
            for (Cliente cliente : datos.clientes) {
                modelo.addRow(new Object[]{cliente.identificacion, cliente.nombre,
                    cliente.telefono, cliente.correo, cliente.direccion});
            }
        };
        refrescar.run();
        JPanel acciones = new JPanel();

        acciones.add(boton("Registrar cliente", () -> {
            JPanel formularioCliente = formulario();
            JTextField identificacion = campo(formularioCliente, "Identificación");
            JTextField nombre = campo(formularioCliente, "Nombre");
            JTextField telefono = campo(formularioCliente, "Teléfono");
            JTextField correo = campo(formularioCliente, "Correo");
            JTextField direccion = campo(formularioCliente, "Dirección");
            if (JOptionPane.showConfirmDialog(ventana, formularioCliente,
                    "Nuevo cliente", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                String id = requerido(identificacion.getText());
                if (datos.clientes.stream().anyMatch(c -> c.identificacion.equalsIgnoreCase(id))) {
                    throw new IllegalArgumentException("Identificación duplicada.");
                }
                // Validar todo antes de modificar la lista de clientes.
                String nombreValidado = requerido(nombre.getText());
                String telefonoValidado = validarTelefono(telefono.getText());
                String correoValidado = validarCorreo(correo.getText());
                datos.clientes.add(new Cliente(id, nombreValidado, telefonoValidado,
                    correoValidado, direccion.getText().trim()));
                guardar();
                refrescar.run();
            }
        }));

        acciones.add(boton("Editar seleccionado", () -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0) {
                throw new IllegalArgumentException("Seleccione un cliente.");
            }
            Cliente cliente = datos.clientes.get(tabla.convertRowIndexToModel(fila));
            JPanel formularioCliente = formulario();
            JTextField nombre = campo(formularioCliente, "Nombre");
            JTextField telefono = campo(formularioCliente, "Teléfono");
            JTextField correo = campo(formularioCliente, "Correo");
            JTextField direccion = campo(formularioCliente, "Dirección");
            nombre.setText(cliente.nombre);
            telefono.setText(cliente.telefono);
            correo.setText(cliente.correo);
            direccion.setText(cliente.direccion);
            if (JOptionPane.showConfirmDialog(ventana, formularioCliente,
                    "Editar " + cliente.identificacion, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                // Validar primero: un dato incorrecto no debe producir una edición parcial.
                String nombreValidado = requerido(nombre.getText());
                String telefonoValidado = validarTelefono(telefono.getText());
                String correoValidado = validarCorreo(correo.getText());
                cliente.nombre = nombreValidado;
                cliente.telefono = telefonoValidado;
                cliente.correo = correoValidado;
                cliente.direccion = direccion.getText().trim();
                guardar();
                refrescar.run();
            }
        }));

        acciones.add(boton("Buscar", () -> {
            String entrada = JOptionPane.showInputDialog(ventana, "Identificación o nombre:");
            if (entrada == null) {
                return;
            }
            String busqueda = entrada.trim().toLowerCase(java.util.Locale.ROOT);
            if (busqueda.isEmpty()) {
                error("Ingrese una identificación o nombre para buscar.");
                return;
            }
            for (int fila = 0; fila < tabla.getRowCount(); fila++) {
                String contenido = (tabla.getValueAt(fila, 0) + " " + tabla.getValueAt(fila, 1))
                    .toLowerCase(java.util.Locale.ROOT);
                if (contenido.contains(busqueda)) {
                    tabla.setRowSelectionInterval(fila, fila);
                    tabla.scrollRectToVisible(tabla.getCellRect(fila, 0, true));
                    return;
                }
            }
            error("No se encontró el cliente.");
        }));
        return contenedor(tabla, acciones);
    }

    /** Valida precios monetarios sin aceptar valores negativos ni formatos inválidos. */
    static BigDecimal validarPrecio(String texto) {
        String valor = requerido(texto);
        try {
            BigDecimal precio = new BigDecimal(valor);
            if (precio.signum() < 0 || precio.scale() > 2) {
                throw new IllegalArgumentException("El precio debe ser positivo o cero y tener máximo 2 decimales.");
            }
            return precio;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Ingrese un precio numérico válido (ejemplo: 1500.50).");
        }
    }

    /** Valida números enteros no negativos para existencias y stock mínimo. */
    static int validarCantidad(String texto, String campo) {
        String valor = requerido(texto);
        try {
            int cantidad = Integer.parseInt(valor);
            if (cantidad < 0) {
                throw new IllegalArgumentException(campo + " no puede ser negativo.");
            }
            return cantidad;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(campo + " debe ser un número entero válido.");
        }
    }

    /** Estado del inventario, priorizando el agotamiento sobre el mínimo. */
    static String estadoInventario(Producto producto) {
        if (producto.existencias == 0) return "AGOTADO";
        if (producto.existencias <= producto.minimo) return "REABASTECER";
        return "OK";
    }

    static JPanel productos() {
        DefaultTableModel modelo = tablaModelo("Código", "Nombre", "Descripción", "Categoría",
            "Precio ₡", "Existencias", "Mínimo", "Alerta");
        JTable tabla = new JTable(modelo);
        Runnable refrescar = () -> {
            modelo.setRowCount(0);
            for (Producto producto : datos.productos) {
                modelo.addRow(new Object[]{producto.codigo, producto.nombre, producto.descripcion,
                    producto.categoria, producto.precio, producto.existencias, producto.minimo,
                    estadoInventario(producto)});
            }
        };
        refrescar.run();
        JPanel acciones = new JPanel();

        if (sesion.administra()) {
            acciones.add(boton("Registrar producto", () -> {
                JPanel panel = formulario();
                JTextField codigo = campo(panel, "Código");
                JTextField nombre = campo(panel, "Nombre");
                JTextField descripcion = campo(panel, "Descripción");
                JTextField categoria = campo(panel, "Categoría");
                JTextField precio = campo(panel, "Precio");
                JTextField existencias = campo(panel, "Existencias iniciales");
                JTextField minimo = campo(panel, "Stock mínimo");
                if (JOptionPane.showConfirmDialog(ventana, panel, "Nuevo producto",
                        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    String codigoValidado = requerido(codigo.getText());
                    if (datos.productos.stream().anyMatch(p -> p.codigo.equalsIgnoreCase(codigoValidado))) {
                        throw new IllegalArgumentException("Código duplicado.");
                    }
                    String nombreValidado = requerido(nombre.getText());
                    String categoriaValidada = requerido(categoria.getText());
                    BigDecimal precioValidado = validarPrecio(precio.getText());
                    int existenciasValidadas = validarCantidad(existencias.getText(), "Existencias iniciales");
                    int minimoValidado = validarCantidad(minimo.getText(), "Stock mínimo");
                    datos.productos.add(new Producto(codigoValidado, nombreValidado,
                        descripcion.getText().trim(), categoriaValidada, precioValidado,
                        existenciasValidadas, minimoValidado));
                    guardar();
                    refrescar.run();
                }
            }));

            acciones.add(boton("Entrada de inventario", () -> {
                int fila = tabla.getSelectedRow();
                if (fila < 0) throw new IllegalArgumentException("Seleccione un producto.");
                Producto producto = datos.productos.get(tabla.convertRowIndexToModel(fila));
                String entrada = JOptionPane.showInputDialog(ventana,
                    "Unidades para agregar a " + producto.nombre);
                if (entrada != null) {
                    int cantidad = validarCantidad(entrada, "Cantidad de entrada");
                    if (cantidad == 0) {
                        throw new IllegalArgumentException("La cantidad de entrada debe ser mayor que cero.");
                    }
                    try {
                        int nuevasExistencias = Math.addExact(producto.existencias, cantidad);
                        producto.existencias = nuevasExistencias;
                    } catch (ArithmeticException ex) {
                        throw new IllegalArgumentException("La entrada supera el máximo de existencias permitido.");
                    }
                    guardar();
                    refrescar.run();
                }
            }));

            acciones.add(boton("Editar producto", () -> {
                int fila = tabla.getSelectedRow();
                if (fila < 0) throw new IllegalArgumentException("Seleccione un producto.");
                Producto producto = datos.productos.get(tabla.convertRowIndexToModel(fila));
                JPanel panel = formulario();
                JTextField nombre = campo(panel, "Nombre");
                JTextField descripcion = campo(panel, "Descripción");
                JTextField categoria = campo(panel, "Categoría");
                JTextField precio = campo(panel, "Precio");
                JTextField minimo = campo(panel, "Mínimo");
                nombre.setText(producto.nombre);
                descripcion.setText(producto.descripcion);
                categoria.setText(producto.categoria);
                precio.setText(producto.precio.toString());
                minimo.setText(String.valueOf(producto.minimo));
                if (JOptionPane.showConfirmDialog(ventana, panel, "Editar producto",
                        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    String nombreValidado = requerido(nombre.getText());
                    String categoriaValidada = requerido(categoria.getText());
                    BigDecimal precioValidado = validarPrecio(precio.getText());
                    int minimoValidado = validarCantidad(minimo.getText(), "Stock mínimo");
                    // No modificar el producto hasta validar todos los campos.
                    producto.nombre = nombreValidado;
                    producto.descripcion = descripcion.getText().trim();
                    producto.categoria = categoriaValidada;
                    producto.precio = precioValidado;
                    producto.minimo = minimoValidado;
                    guardar();
                    refrescar.run();
                }
            }));
        } else {
            acciones.add(new JLabel("Consulta de inventario (solo administración puede modificar)."));
        }
        return contenedor(tabla, acciones);
    }

    /** Calcula el subtotal del carrito sin modificar los datos persistentes. */
    static BigDecimal subtotalCarrito(List<Linea> carrito) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Linea linea : carrito) subtotal = subtotal.add(linea.subtotal());
        return subtotal;
    }

    /** Cantidad ya solicitada de un producto, sin desbordamiento silencioso. */
    static int cantidadEnCarrito(List<Linea> carrito, String codigo) {
        int acumulado = 0;
        for (Linea linea : carrito) {
            if (linea.codigo.equals(codigo)) {
                try {
                    acumulado = Math.addExact(acumulado, linea.cantidad);
                } catch (ArithmeticException ex) {
                    throw new IllegalArgumentException("La cantidad acumulada supera el máximo permitido.");
                }
            }
        }
        return acumulado;
    }

    static JPanel facturacion() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel superior = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<Cliente> clientes = new JComboBox<>();
        JComboBox<Producto> productos = new JComboBox<>();
        JSpinner cantidad = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        superior.add(new JLabel("Cliente:"));
        superior.add(clientes);
        superior.add(new JLabel("Producto:"));
        superior.add(productos);
        superior.add(new JLabel("Cantidad:"));
        superior.add(cantidad);
        panel.add(superior, BorderLayout.NORTH);

        DefaultTableModel modelo = tablaModelo("Código", "Producto", "Cantidad", "Precio", "Subtotal");
        JTable tabla = new JTable(modelo);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        List<Linea> carrito = new ArrayList<>();
        JLabel total = new JLabel("Subtotal ₡0.00 | IVA (13%) ₡0.00 | Total ₡0.00");

        Runnable actualizar = () -> {
            modelo.setRowCount(0);
            for (Linea linea : carrito) {
                modelo.addRow(new Object[]{linea.codigo, linea.nombre, linea.cantidad,
                    linea.precio, linea.subtotal()});
            }
            BigDecimal subtotal = subtotalCarrito(carrito).setScale(2, RoundingMode.HALF_UP);
            BigDecimal impuesto = subtotal.multiply(IVA).setScale(2, RoundingMode.HALF_UP);
            total.setText("Subtotal ₡" + subtotal + " | IVA (13%) ₡" + impuesto
                + " | Total ₡" + subtotal.add(impuesto));
        };

        Runnable cargar = () -> {
            Cliente clienteAnterior = (Cliente) clientes.getSelectedItem();
            Producto productoAnterior = (Producto) productos.getSelectedItem();
            clientes.removeAllItems();
            productos.removeAllItems();
            for (Cliente cliente : datos.clientes) clientes.addItem(cliente);
            for (Producto producto : datos.productos) productos.addItem(producto);
            if (clienteAnterior != null) clientes.setSelectedItem(clienteAnterior);
            if (productoAnterior != null) productos.setSelectedItem(productoAnterior);
        };
        pestañasPlaceholder(panel, cargar);
        JPanel inferior = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inferior.add(boton("Actualizar catálogos", cargar));

        inferior.add(boton("Agregar línea", () -> {
            Producto producto = (Producto) productos.getSelectedItem();
            if (producto == null) throw new IllegalArgumentException("Registre un producto primero.");
            int unidades;
            try {
                cantidad.commitEdit();
                unidades = ((Number) cantidad.getValue()).intValue();
            } catch (java.text.ParseException | ClassCastException ex) {
                throw new IllegalArgumentException("Ingrese una cantidad entera válida.");
            }
            if (unidades <= 0 || unidades > 100000) {
                throw new IllegalArgumentException("La cantidad debe estar entre 1 y 100000.");
            }
            if (producto.existencias <= 0) {
                throw new IllegalArgumentException("El producto está agotado: " + producto.nombre);
            }
            int solicitadas = cantidadEnCarrito(carrito, producto.codigo);
            if (unidades > producto.existencias - solicitadas) {
                throw new IllegalArgumentException("Existencias insuficientes para " + producto.nombre
                    + ". Disponibles para agregar: " + Math.max(0, producto.existencias - solicitadas));
            }
            // Consolidar el mismo código en una sola línea con el precio actual del producto.
            for (int i = 0; i < carrito.size(); i++) {
                Linea anterior = carrito.get(i);
                if (anterior.codigo.equals(producto.codigo)) {
                    // Linea no expone setters: se reemplaza con una nueva instancia.
                    Linea combinada = new Linea(producto, Math.addExact(anterior.cantidad, unidades));
                    carrito.set(i, combinada);
                    actualizar.run();
                    return;
                }
            }
            carrito.add(new Linea(producto, unidades));
            actualizar.run();
        }));

        inferior.add(boton("Quitar línea", () -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0) throw new IllegalArgumentException("Seleccione una línea para quitar.");
            carrito.remove(tabla.convertRowIndexToModel(fila));
            actualizar.run();
        }));

        inferior.add(boton("Confirmar factura", () -> {
            Cliente cliente = (Cliente) clientes.getSelectedItem();
            if (cliente == null) throw new IllegalArgumentException("Seleccione un cliente.");
            if (carrito.isEmpty()) throw new IllegalArgumentException("Agregue al menos un producto.");
            synchronized (datos) {
                Map<String, Integer> requeridos = new HashMap<>();
                for (Linea linea : carrito) {
                    if (linea.cantidad <= 0) throw new IllegalArgumentException("Cantidad inválida en el carrito.");
                    try {
                        requeridos.merge(linea.codigo, linea.cantidad, Math::addExact);
                    } catch (ArithmeticException ex) {
                        throw new IllegalArgumentException("La cantidad acumulada supera el máximo permitido.");
                    }
                }
                for (Map.Entry<String, Integer> entrada : requeridos.entrySet()) {
                    Producto productoActual = datos.productos.stream()
                        .filter(p -> p.codigo.equals(entrada.getKey())).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Un producto del carrito ya no existe."));
                    if (productoActual.existencias < entrada.getValue()) {
                        throw new IllegalArgumentException("Stock insuficiente para " + productoActual.nombre
                            + ". Disponible: " + productoActual.existencias);
                    }
                }
                BigDecimal subtotal = subtotalCarrito(carrito).setScale(2, RoundingMode.HALF_UP);
                BigDecimal impuesto = subtotal.multiply(IVA).setScale(2, RoundingMode.HALF_UP);
                BigDecimal totalFactura = subtotal.add(impuesto);
                String resumen = "Cliente: " + cliente + "\nLíneas: " + carrito.size()
                    + "\nSubtotal: ₡" + subtotal + "\nIVA (13%): ₡" + impuesto
                    + "\nTOTAL: ₡" + totalFactura + "\n\n¿Desea registrar la factura y descontar el inventario?";
                int respuesta = JOptionPane.showConfirmDialog(ventana, resumen,
                    "Confirmar venta", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (respuesta != JOptionPane.YES_OPTION) return;

                for (Producto producto : datos.productos) {
                    producto.existencias -= requeridos.getOrDefault(producto.codigo, 0);
                }
                Factura factura = new Factura(datos.facturas.size() + 1,
                    cliente.toString(), sesion.usuario, new ArrayList<>(carrito));
                datos.facturas.add(factura);
                guardar();
                carrito.clear();
                actualizar.run();
                cargar.run();
                JTextArea comprobante = new JTextArea(factura.comprobante(), 20, 55);
                comprobante.setEditable(false);
                comprobante.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                JOptionPane.showMessageDialog(ventana, new JScrollPane(comprobante),
                    "Factura confirmada", JOptionPane.INFORMATION_MESSAGE);
                exportar(factura);
            }
        }));
        inferior.add(total);
        panel.add(inferior, BorderLayout.SOUTH);
        return panel;
    }
    static void pestañasPlaceholder(JPanel panel,Runnable r){panel.addHierarchyListener(e->{if(panel.isShowing())r.run();});}
    static void exportar(Factura f){JFileChooser ch=new JFileChooser();ch.setSelectedFile(new File("Factura_"+f.numero+".txt"));if(ch.showSaveDialog(ventana)==JFileChooser.APPROVE_OPTION)try{Files.writeString(ch.getSelectedFile().toPath(),f.comprobante());JOptionPane.showMessageDialog(ventana,"Comprobante guardado.");}catch(IOException ex){error("No se pudo exportar: "+ex.getMessage());}}
    static JPanel historial(){DefaultTableModel m=tablaModelo("Número","Fecha","Cliente","Usuario","Total ₡");JTable t=new JTable(m);Runnable refrescar=()->{m.setRowCount(0);for(Factura f:datos.facturas)m.addRow(new Object[]{f.numero,f.fecha,f.cliente,f.usuario,f.total()});};JPanel a=new JPanel();a.add(boton("Actualizar",refrescar));a.add(boton("Ver comprobante",()->{int r=t.getSelectedRow();if(r<0)throw new IllegalArgumentException("Seleccione una factura.");Factura f=datos.facturas.get(t.convertRowIndexToModel(r));JTextArea area=new JTextArea(f.comprobante(),22,55);area.setEditable(false);JOptionPane.showMessageDialog(ventana,new JScrollPane(area),"Factura "+f.numero,JOptionPane.INFORMATION_MESSAGE);}));a.add(boton("Exportar seleccionado",()->{int r=t.getSelectedRow();if(r<0)throw new IllegalArgumentException("Seleccione una factura.");exportar(datos.facturas.get(t.convertRowIndexToModel(r)));}));a.add(boton("Buscar",()->{String s=JOptionPane.showInputDialog(ventana,"Número, fecha o cliente:");if(s!=null)for(int r=0;r<t.getRowCount();r++)if((t.getValueAt(r,0)+" "+t.getValueAt(r,1)+" "+t.getValueAt(r,2)).toLowerCase().contains(s.toLowerCase())){t.setRowSelectionInterval(r,r);return;}error("Sin resultados.");}));refrescar.run();return contenedor(t,a);}
    static JPanel usuarios(){DefaultTableModel m=tablaModelo("Usuario","Nombre","Rol","Activo");JTable t=new JTable(m);Runnable refrescar=()->{m.setRowCount(0);for(Usuario u:datos.usuarios)m.addRow(new Object[]{u.usuario,u.nombre,u.rol(),u.activo});};JPanel a=new JPanel();a.add(boton("Crear usuario",()->{JPanel p=formulario();JTextField n=campo(p,"Nombre"),u=campo(p,"Usuario");p.add(new JLabel("Contraseña"));JPasswordField clave=new JPasswordField();p.add(clave);p.add(new JLabel("Rol"));JComboBox<String> rol=new JComboBox<>(new String[]{"Cajero","Administrador"});p.add(rol);if(JOptionPane.showConfirmDialog(ventana,p,"Nuevo usuario",JOptionPane.OK_CANCEL_OPTION)==0){String usuario=requerido(u.getText()),password=new String(clave.getPassword());if(password.length()<8)throw new IllegalArgumentException("Contraseña mínima: 8 caracteres.");if(datos.usuarios.stream().anyMatch(x->x.usuario.equalsIgnoreCase(usuario)))throw new IllegalArgumentException("Usuario duplicado.");datos.usuarios.add(rol.getSelectedIndex()==1?new Administrador(requerido(n.getText()),usuario,password):new Cajero(requerido(n.getText()),usuario,password));guardar();refrescar.run();}}));a.add(boton("Activar / desactivar",()->{int r=t.getSelectedRow();if(r<0)throw new IllegalArgumentException("Seleccione un usuario.");Usuario u=datos.usuarios.get(t.convertRowIndexToModel(r));if(u==sesion)throw new IllegalArgumentException("No puede desactivar su propia sesión.");u.activo=!u.activo;guardar();refrescar.run();}));refrescar.run();return contenedor(t,a);}
    public static void main(String[] args){SwingUtilities.invokeLater(()->{UIManager.put("OptionPane.messageFont",new Font("SansSerif",Font.PLAIN,13));cargar();login();interfaz();});}
}
