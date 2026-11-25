package com.example.comunidadwestern;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class AppValidationTest {
    @Test public void registroAceptaViviendaAlfanumerica(){assertTrue(AppValidation.isValidRegistration("Ana Perez","311A","ana@mail.cl","clave123","clave123"));}
    @Test public void registroRechazaCamposVacios(){assertFalse(AppValidation.isValidRegistration("","311A","ana@mail.cl","clave123","clave123"));}
    @Test public void ventaValidaPrecioNumerico(){assertTrue(AppValidation.isValidSale("Bicicleta","En buen estado","45000"));}
    @Test public void ventaRechazaPrecioNoNumerico(){assertFalse(AppValidation.isValidSale("Bicicleta","En buen estado","gratis"));}
    @Test public void alertaGeneraIdentificadoresUnicos(){String first=AppValidation.generateAlertId();String second=AppValidation.generateAlertId();assertNotEquals(first,second);assertFalse(first.isEmpty());}
    @Test public void alertaNoSeDuplica(){assertFalse(AppValidation.isNewAlertId("evt-1", Arrays.asList("evt-1","evt-2")));assertTrue(AppValidation.isNewAlertId("evt-3",Arrays.asList("evt-1","evt-2")));}
    @Test public void alertaRespetaVentanaAntirepeticion(){assertFalse(AppValidation.canSendAlert(1000,12000));assertTrue(AppValidation.canSendAlert(1000,17000));}
}
