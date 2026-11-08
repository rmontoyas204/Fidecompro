package fidecompro;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public class Factura implements Serializable {
    private static final long serialVersionUID = 1L;
    int numero; LocalDateTime fecha; String cliente,usuario; List<Linea> lineas;
    Factura(int n,String c,String u,List<Linea> l){numero=n;cliente=c;usuario=u;fecha=LocalDateTime.now();lineas=new ArrayList<>(l);}
    BigDecimal subtotal(){return lineas.stream().map(Linea::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add);}
    BigDecimal impuesto(){return subtotal().multiply(Main.IVA).setScale(2,RoundingMode.HALF_UP);}
    BigDecimal total(){return subtotal().add(impuesto());}
    String comprobante(){StringBuilder b=new StringBuilder("FIDECOMPRO - COMPROBANTE ACADÉMICO (NO ELECTRÓNICO)\nFactura: "+numero+"\nFecha: "+fecha+"\nCliente: "+cliente+"\nColaborador: "+usuario+"\n----------------------------------------\n");for(Linea l:lineas)b.append(l.nombre).append(" x").append(l.cantidad).append(" @ ₡").append(l.precio).append(" = ₡").append(l.subtotal()).append('\n');return b.append("----------------------------------------\nSubtotal: ₡").append(subtotal()).append("\nIVA (13%): ₡").append(impuesto()).append("\nTOTAL: ₡").append(total()).append("\n").toString();}
}
