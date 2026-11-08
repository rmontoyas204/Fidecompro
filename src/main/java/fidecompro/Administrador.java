package fidecompro;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public class Administrador extends Usuario {
    private static final long serialVersionUID = 1L;
    Administrador(String n,String u,String c){super(n,u,c);}
    @Override String rol(){return "Administrador";}
    @Override boolean administra(){return true;}
}
