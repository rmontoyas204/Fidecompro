package fidecompro;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public class Datos implements Serializable {
    private static final long serialVersionUID = 1L;
    List<Usuario> usuarios=new ArrayList<>(); List<Cliente> clientes=new ArrayList<>(); List<Producto> productos=new ArrayList<>(); List<Factura> facturas=new ArrayList<>();
}
