package fidecompro;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public class Cliente implements Serializable {
    private static final long serialVersionUID = 1L;
    String identificacion,nombre,telefono,correo,direccion;
    Cliente(String i,String n,String t,String c,String d){identificacion=i;nombre=n;telefono=t;correo=c;direccion=d;}
    public String toString(){return identificacion+" - "+nombre;}
}
