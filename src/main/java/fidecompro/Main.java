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
    static JTextField campo(JPanel p,String titulo){p.add(new JLabel(titulo));JTextField t=new JTextField(19);p.add(t);return t;}
    static JPanel formulario(){JPanel p=new JPanel(new GridLayout(0,2,8,8));p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));return p;}
    static JButton boton(String t,Runnable r){JButton b=new JButton(t);b.addActionListener(e->{try{r.run();}catch(IllegalArgumentException ex){error(ex.getMessage());}catch(Exception ex){error("Operación fallida: "+ex.getMessage());}});return b;}
    static DefaultTableModel tablaModelo(String... cols){return new DefaultTableModel(cols,0){@Override public boolean isCellEditable(int r,int c){return false;}};}
    static JPanel contenedor(JTable t,JPanel acciones){JPanel p=new JPanel(new BorderLayout(8,8));p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));p.add(new JScrollPane(t),BorderLayout.CENTER);p.add(acciones,BorderLayout.SOUTH);return p;}
    static void login(){JTextField usuario=new JTextField();JPasswordField clave=new JPasswordField();JPanel p=formulario();p.add(new JLabel("Usuario"));p.add(usuario);p.add(new JLabel("Contraseña"));p.add(clave);while(true){int op=JOptionPane.showConfirmDialog(null,p,"Fidecompro | Iniciar sesión",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);if(op!=JOptionPane.OK_OPTION)System.exit(0);String u=usuario.getText().trim();String h=hash(new String(clave.getPassword()));for(Usuario x:datos.usuarios)if(x.activo&&x.usuario.equalsIgnoreCase(u)&&x.claveHash.equals(h)){sesion=x;return;}JOptionPane.showMessageDialog(null,"Credenciales inválidas o cuenta deshabilitada.");clave.setText("");}}
    static void interfaz(){ventana=new JFrame("FIDECOMPRO | Inventario y facturación | "+sesion);ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);ventana.setSize(1060,680);ventana.setLocationRelativeTo(null);pestañas=new JTabbedPane();pestañas.addTab("Clientes",clientes());pestañas.addTab("Productos e inventario",productos());pestañas.addTab("Facturación",facturacion());pestañas.addTab("Historial",historial());if(sesion.administra())pestañas.addTab("Usuarios",usuarios());ventana.add(pestañas);ventana.setVisible(true);}
    static JPanel clientes(){DefaultTableModel m=tablaModelo("Identificación","Nombre","Teléfono","Correo","Dirección");JTable t=new JTable(m);Runnable refrescar=()->{m.setRowCount(0);for(Cliente c:datos.clientes)m.addRow(new Object[]{c.identificacion,c.nombre,c.telefono,c.correo,c.direccion});};refrescar.run();JPanel acciones=new JPanel();acciones.add(boton("Registrar cliente",()->{JPanel p=formulario();JTextField i=campo(p,"Identificación"),n=campo(p,"Nombre"),tel=campo(p,"Teléfono"),cor=campo(p,"Correo"),dir=campo(p,"Dirección");if(JOptionPane.showConfirmDialog(ventana,p,"Nuevo cliente",JOptionPane.OK_CANCEL_OPTION)==0){String id=requerido(i.getText());if(datos.clientes.stream().anyMatch(c->c.identificacion.equals(id)))throw new IllegalArgumentException("Identificación duplicada.");String email=cor.getText().trim();if(!email.isEmpty()&&!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))throw new IllegalArgumentException("Correo inválido.");datos.clientes.add(new Cliente(id,requerido(n.getText()),tel.getText().trim(),email,dir.getText().trim()));guardar();refrescar.run();}}));acciones.add(boton("Editar seleccionado",()->{int r=t.getSelectedRow();if(r<0)throw new IllegalArgumentException("Seleccione un cliente.");Cliente c=datos.clientes.get(t.convertRowIndexToModel(r));JPanel p=formulario();JTextField n=campo(p,"Nombre"),tel=campo(p,"Teléfono"),cor=campo(p,"Correo"),dir=campo(p,"Dirección");n.setText(c.nombre);tel.setText(c.telefono);cor.setText(c.correo);dir.setText(c.direccion);if(JOptionPane.showConfirmDialog(ventana,p,"Editar "+c.identificacion,JOptionPane.OK_CANCEL_OPTION)==0){String email=cor.getText().trim();if(!email.isEmpty()&&!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))throw new IllegalArgumentException("Correo inválido.");c.nombre=requerido(n.getText());c.telefono=tel.getText().trim();c.correo=email;c.direccion=dir.getText().trim();guardar();refrescar.run();}}));acciones.add(boton("Buscar",()->{String s=JOptionPane.showInputDialog(ventana,"Identificación o nombre:");if(s!=null){for(int r=0;r<t.getRowCount();r++)if((t.getValueAt(r,0)+" "+t.getValueAt(r,1)).toLowerCase().contains(s.toLowerCase())){t.setRowSelectionInterval(r,r);t.scrollRectToVisible(t.getCellRect(r,0,true));return;}error("No se encontró el cliente.");}}));return contenedor(t,acciones);}
    static JPanel productos(){DefaultTableModel m=tablaModelo("Código","Nombre","Descripción","Categoría","Precio ₡","Existencias","Mínimo","Alerta");JTable t=new JTable(m);Runnable refrescar=()->{m.setRowCount(0);for(Producto x:datos.productos)m.addRow(new Object[]{x.codigo,x.nombre,x.descripcion,x.categoria,x.precio,x.existencias,x.minimo,x.existencias<=x.minimo?"REABASTECER":"OK"});};refrescar.run();JPanel acciones=new JPanel();if(sesion.administra()){acciones.add(boton("Registrar producto",()->{JPanel p=formulario();JTextField cod=campo(p,"Código"),nom=campo(p,"Nombre"),des=campo(p,"Descripción"),cat=campo(p,"Categoría"),pre=campo(p,"Precio"),sto=campo(p,"Existencias iniciales"),min=campo(p,"Stock mínimo");if(JOptionPane.showConfirmDialog(ventana,p,"Nuevo producto",JOptionPane.OK_CANCEL_OPTION)==0){String c=requerido(cod.getText());if(datos.productos.stream().anyMatch(x->x.codigo.equalsIgnoreCase(c)))throw new IllegalArgumentException("Código duplicado.");BigDecimal precio=new BigDecimal(requerido(pre.getText()));int e=Integer.parseInt(requerido(sto.getText())),mi=Integer.parseInt(requerido(min.getText()));if(precio.signum()<0||e<0||mi<0)throw new IllegalArgumentException("Precio y cantidades no pueden ser negativos.");datos.productos.add(new Producto(c,requerido(nom.getText()),des.getText(),requerido(cat.getText()),precio,e,mi));guardar();refrescar.run();}}));acciones.add(boton("Entrada de inventario",()->{int r=t.getSelectedRow();if(r<0)throw new IllegalArgumentException("Seleccione un producto.");Producto x=datos.productos.get(t.convertRowIndexToModel(r));String s=JOptionPane.showInputDialog(ventana,"Unidades para agregar a "+x.nombre);if(s!=null){int n=Integer.parseInt(s);if(n<=0)throw new IllegalArgumentException("La cantidad debe ser positiva.");x.existencias=Math.addExact(x.existencias,n);guardar();refrescar.run();}}));acciones.add(boton("Editar producto",()->{int r=t.getSelectedRow();if(r<0)throw new IllegalArgumentException("Seleccione un producto.");Producto x=datos.productos.get(t.convertRowIndexToModel(r));JPanel p=formulario();JTextField n=campo(p,"Nombre"),d=campo(p,"Descripción"),c=campo(p,"Categoría"),pr=campo(p,"Precio"),mi=campo(p,"Mínimo");n.setText(x.nombre);d.setText(x.descripcion);c.setText(x.categoria);pr.setText(x.precio.toString());mi.setText(""+x.minimo);if(JOptionPane.showConfirmDialog(ventana,p,"Editar producto",JOptionPane.OK_CANCEL_OPTION)==0){BigDecimal precio=new BigDecimal(pr.getText());int minimo=Integer.parseInt(mi.getText());if(precio.signum()<0||minimo<0)throw new IllegalArgumentException("Valores inválidos.");x.nombre=requerido(n.getText());x.descripcion=d.getText();x.categoria=requerido(c.getText());x.precio=precio;x.minimo=minimo;guardar();refrescar.run();}}));}else acciones.add(new JLabel("Consulta de inventario (solo administración puede modificar)."));return contenedor(t,acciones);}
    static JPanel facturacion(){JPanel panel=new JPanel(new BorderLayout(10,10));panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));JPanel superior=new JPanel(new FlowLayout(FlowLayout.LEFT));JComboBox<Cliente> clientes=new JComboBox<>();JComboBox<Producto> productos=new JComboBox<>();JSpinner cantidad=new JSpinner(new SpinnerNumberModel(1,1,100000,1));superior.add(new JLabel("Cliente:"));superior.add(clientes);superior.add(new JLabel("Producto:"));superior.add(productos);superior.add(new JLabel("Cantidad:"));superior.add(cantidad);panel.add(superior,BorderLayout.NORTH);DefaultTableModel m=tablaModelo("Código","Producto","Cantidad","Precio","Subtotal");JTable t=new JTable(m);panel.add(new JScrollPane(t),BorderLayout.CENTER);List<Linea> carrito=new ArrayList<>();JLabel total=new JLabel("Subtotal ₡0 | IVA ₡0 | Total ₡0");Runnable actualizar=()->{m.setRowCount(0);BigDecimal sub=BigDecimal.ZERO;for(Linea l:carrito){m.addRow(new Object[]{l.codigo,l.nombre,l.cantidad,l.precio,l.subtotal()});sub=sub.add(l.subtotal());}BigDecimal iva=sub.multiply(IVA).setScale(2,RoundingMode.HALF_UP);total.setText("Subtotal ₡"+sub+" | IVA (13%) ₡"+iva+" | Total ₡"+sub.add(iva));};Runnable cargar=()->{clientes.removeAllItems();productos.removeAllItems();for(Cliente c:datos.clientes)clientes.addItem(c);for(Producto p:datos.productos)productos.addItem(p);};pestañasPlaceholder(panel,cargar);JPanel inferior=new JPanel(new FlowLayout(FlowLayout.LEFT));inferior.add(boton("Actualizar catálogos",cargar));inferior.add(boton("Agregar línea",()->{Producto p=(Producto)productos.getSelectedItem();if(p==null)throw new IllegalArgumentException("Registre productos primero.");int n=(int)cantidad.getValue();int ya=carrito.stream().filter(l->l.codigo.equals(p.codigo)).mapToInt(l->l.cantidad).sum();if(n>p.existencias-ya)throw new IllegalArgumentException("Existencias insuficientes.");carrito.add(new Linea(p,n));actualizar.run();}));inferior.add(boton("Quitar línea",()->{int r=t.getSelectedRow();if(r>=0){carrito.remove(t.convertRowIndexToModel(r));actualizar.run();}}));inferior.add(boton("Confirmar factura",()->{Cliente c=(Cliente)clientes.getSelectedItem();if(c==null||carrito.isEmpty())throw new IllegalArgumentException("Seleccione cliente y agregue productos.");synchronized(datos){Map<String,Integer> requeridos=new HashMap<>();for(Linea l:carrito)requeridos.merge(l.codigo,l.cantidad,Integer::sum);for(Map.Entry<String,Integer> e:requeridos.entrySet()){Producto p=datos.productos.stream().filter(x->x.codigo.equals(e.getKey())).findFirst().orElseThrow(()->new IllegalArgumentException("Producto inexistente."));if(p.existencias<e.getValue())throw new IllegalArgumentException("Stock insuficiente para "+p.nombre);}for(Producto p:datos.productos)p.existencias-=requeridos.getOrDefault(p.codigo,0);Factura f=new Factura(datos.facturas.size()+1,c.toString(),sesion.usuario,carrito);datos.facturas.add(f);guardar();carrito.clear();actualizar.run();cargar.run();JTextArea a=new JTextArea(f.comprobante(),20,55);a.setEditable(false);JOptionPane.showMessageDialog(ventana,new JScrollPane(a),"Factura confirmada",JOptionPane.INFORMATION_MESSAGE);exportar(f);}}));inferior.add(total);panel.add(inferior,BorderLayout.SOUTH);return panel;}
    static void pestañasPlaceholder(JPanel panel,Runnable r){panel.addHierarchyListener(e->{if(panel.isShowing())r.run();});}
    static void exportar(Factura f){JFileChooser ch=new JFileChooser();ch.setSelectedFile(new File("Factura_"+f.numero+".txt"));if(ch.showSaveDialog(ventana)==JFileChooser.APPROVE_OPTION)try{Files.writeString(ch.getSelectedFile().toPath(),f.comprobante());JOptionPane.showMessageDialog(ventana,"Comprobante guardado.");}catch(IOException ex){error("No se pudo exportar: "+ex.getMessage());}}
    static JPanel historial(){DefaultTableModel m=tablaModelo("Número","Fecha","Cliente","Usuario","Total ₡");JTable t=new JTable(m);Runnable refrescar=()->{m.setRowCount(0);for(Factura f:datos.facturas)m.addRow(new Object[]{f.numero,f.fecha,f.cliente,f.usuario,f.total()});};JPanel a=new JPanel();a.add(boton("Actualizar",refrescar));a.add(boton("Ver comprobante",()->{int r=t.getSelectedRow();if(r<0)throw new IllegalArgumentException("Seleccione una factura.");Factura f=datos.facturas.get(t.convertRowIndexToModel(r));JTextArea area=new JTextArea(f.comprobante(),22,55);area.setEditable(false);JOptionPane.showMessageDialog(ventana,new JScrollPane(area),"Factura "+f.numero,JOptionPane.INFORMATION_MESSAGE);}));a.add(boton("Exportar seleccionado",()->{int r=t.getSelectedRow();if(r<0)throw new IllegalArgumentException("Seleccione una factura.");exportar(datos.facturas.get(t.convertRowIndexToModel(r)));}));a.add(boton("Buscar",()->{String s=JOptionPane.showInputDialog(ventana,"Número, fecha o cliente:");if(s!=null)for(int r=0;r<t.getRowCount();r++)if((t.getValueAt(r,0)+" "+t.getValueAt(r,1)+" "+t.getValueAt(r,2)).toLowerCase().contains(s.toLowerCase())){t.setRowSelectionInterval(r,r);return;}error("Sin resultados.");}));refrescar.run();return contenedor(t,a);}
    static JPanel usuarios(){DefaultTableModel m=tablaModelo("Usuario","Nombre","Rol","Activo");JTable t=new JTable(m);Runnable refrescar=()->{m.setRowCount(0);for(Usuario u:datos.usuarios)m.addRow(new Object[]{u.usuario,u.nombre,u.rol(),u.activo});};JPanel a=new JPanel();a.add(boton("Crear usuario",()->{JPanel p=formulario();JTextField n=campo(p,"Nombre"),u=campo(p,"Usuario");p.add(new JLabel("Contraseña"));JPasswordField clave=new JPasswordField();p.add(clave);p.add(new JLabel("Rol"));JComboBox<String> rol=new JComboBox<>(new String[]{"Cajero","Administrador"});p.add(rol);if(JOptionPane.showConfirmDialog(ventana,p,"Nuevo usuario",JOptionPane.OK_CANCEL_OPTION)==0){String usuario=requerido(u.getText()),password=new String(clave.getPassword());if(password.length()<8)throw new IllegalArgumentException("Contraseña mínima: 8 caracteres.");if(datos.usuarios.stream().anyMatch(x->x.usuario.equalsIgnoreCase(usuario)))throw new IllegalArgumentException("Usuario duplicado.");datos.usuarios.add(rol.getSelectedIndex()==1?new Administrador(requerido(n.getText()),usuario,password):new Cajero(requerido(n.getText()),usuario,password));guardar();refrescar.run();}}));a.add(boton("Activar / desactivar",()->{int r=t.getSelectedRow();if(r<0)throw new IllegalArgumentException("Seleccione un usuario.");Usuario u=datos.usuarios.get(t.convertRowIndexToModel(r));if(u==sesion)throw new IllegalArgumentException("No puede desactivar su propia sesión.");u.activo=!u.activo;guardar();refrescar.run();}));refrescar.run();return contenedor(t,a);}
    public static void main(String[] args){SwingUtilities.invokeLater(()->{UIManager.put("OptionPane.messageFont",new Font("SansSerif",Font.PLAIN,13));cargar();login();interfaz();});}
}
