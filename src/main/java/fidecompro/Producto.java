package fidecompro;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public class Producto implements Serializable {
    private static final long serialVersionUID = 1L;
    String codigo,nombre,descripcion,categoria; BigDecimal precio; int existencias,minimo;
    Producto(String c,String n,String d,String cat,BigDecimal p,int e,int m){codigo=c;nombre=n;descripcion=d;categoria=cat;precio=p;existencias=e;minimo=m;}
    public String toString(){return codigo+" - "+nombre+" ("+existencias+" disponibles)";}
}
