package fidecompro;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public class Linea implements Serializable {
    private static final long serialVersionUID = 1L;
    String codigo,nombre; int cantidad; BigDecimal precio;
    Linea(Producto p,int c){codigo=p.codigo;nombre=p.nombre;precio=p.precio;cantidad=c;}
    BigDecimal subtotal(){return precio.multiply(BigDecimal.valueOf(cantidad));}
}
