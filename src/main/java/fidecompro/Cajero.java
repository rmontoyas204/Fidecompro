package fidecompro;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public class Cajero extends Usuario {
    private static final long serialVersionUID = 1L;
    Cajero(String n,String u,String c){super(n,u,c);}
    @Override String rol(){return "Cajero";}
    @Override boolean administra(){return false;}
}
