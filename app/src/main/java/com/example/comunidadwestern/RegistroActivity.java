package com.example.comunidadwestern;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {
    private EditText txtNombre, txtVivienda, txtCorreo, txtClave, txtConfirmar;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);
        txtNombre=findViewById(R.id.txtNombre); txtVivienda=findViewById(R.id.txtVivienda);
        txtCorreo=findViewById(R.id.txtCorreoRegistro); txtClave=findViewById(R.id.txtContrasenaRegistro);
        txtConfirmar=findViewById(R.id.txtConfirmarContrasena);
        findViewById(R.id.btnCrearCuenta).setOnClickListener(v -> registrar());
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
    }

    private void registrar() {
        String nombre=txtNombre.getText().toString().trim();
        String vivienda=txtVivienda.getText().toString().trim();
        String correo=txtCorreo.getText().toString().trim();
        String clave=txtClave.getText().toString();
        String confirmar=txtConfirmar.getText().toString();
        if (!AppValidation.isValidRegistration(nombre,vivienda,correo,clave,confirmar)) {
            if (nombre.isEmpty()) txtNombre.setError("Ingresa tu nombre");
            else if (!AppValidation.isValidHousing(vivienda)) txtVivienda.setError("Ingresa una vivienda válida, por ejemplo 311A");
            else if (!AppValidation.isValidEmail(correo)) txtCorreo.setError("Correo no válido");
            else if (clave.length()<6) txtClave.setError("Mínimo 6 caracteres");
            else txtConfirmar.setError("Las contraseñas no coinciden");
            return;
        }
        if (!FirebaseSupport.isReady(this)) { FirebaseSupport.showSetupMessage(this); return; }
        findViewById(R.id.btnCrearCuenta).setEnabled(false);
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(correo,clave).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                findViewById(R.id.btnCrearCuenta).setEnabled(true);
                Toast.makeText(this,"No se pudo crear la cuenta: " +
                        (task.getException()==null?"intenta nuevamente":task.getException().getMessage()),Toast.LENGTH_LONG).show();
                return;
            }
            String uid=FirebaseAuth.getInstance().getCurrentUser().getUid();
            Map<String,Object> perfil=new HashMap<>(); perfil.put("name",nombre); perfil.put("housing",vivienda);
            perfil.put("email",correo); perfil.put("createdAt",System.currentTimeMillis());
            FirebaseSupport.db().child("profiles").child(uid).setValue(perfil).addOnCompleteListener(save -> {
                if (save.isSuccessful()) {
                    FirebaseSupport.subscribeToCommunity();
                    startActivity(new Intent(this,HomeActivity.class)); finish();
                } else {
                    Toast.makeText(this,"La cuenta se creó, pero no se pudo guardar el perfil. Contacta al administrador.",Toast.LENGTH_LONG).show();
                    FirebaseAuth.getInstance().signOut(); findViewById(R.id.btnCrearCuenta).setEnabled(true);
                }
            });
        });
    }
}
