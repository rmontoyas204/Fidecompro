package fidecompro;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public abstract class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;
    String nombre, usuario, claveHash; boolean activo = true;
    Usuario(String nombre,String usuario,String clave) {this.nombre=nombre;this.usuario=usuario;this.claveHash=Main.hash(clave);}
    abstract String rol();
    abstract boolean administra();
    public String toString(){return nombre+" ("+rol()+")";}
}
