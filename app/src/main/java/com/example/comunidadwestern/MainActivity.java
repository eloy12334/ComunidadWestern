package com.example.comunidadwestern;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private EditText txtCorreo, txtContrasena;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtCorreo = findViewById(R.id.txtCorreo);
        txtContrasena = findViewById(R.id.txtContrasena);
        findViewById(R.id.btnIngresar).setOnClickListener(v -> iniciarSesion());
        findViewById(R.id.btnIrRegistro).setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));
        findViewById(R.id.btnRecuperar).setOnClickListener(v -> recuperarClave());

        if (FirebaseSupport.isReady(this) && FirebaseAuth.getInstance().getCurrentUser() != null) {
            abrirInicio();
        }
    }

    private void iniciarSesion() {
        String correo = txtCorreo.getText().toString().trim();
        String clave = txtContrasena.getText().toString();
        if (!AppValidation.isValidEmail(correo)) {
            txtCorreo.setError("Ingresa un correo válido"); txtCorreo.requestFocus(); return;
        }
        if (clave.isEmpty()) {
            txtContrasena.setError("Ingresa tu contraseña"); txtContrasena.requestFocus(); return;
        }
        if (!FirebaseSupport.isReady(this)) {
            FirebaseSupport.showSetupMessage(this); return;
        }
        findViewById(R.id.btnIngresar).setEnabled(false);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(correo, clave)
                .addOnCompleteListener(task -> {
                    findViewById(R.id.btnIngresar).setEnabled(true);
                    if (task.isSuccessful()) abrirInicio();
                    else Toast.makeText(this, "No se pudo iniciar sesión: " +
                            (task.getException() == null ? "verifica tus datos" : task.getException().getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void recuperarClave() {
        String correo = txtCorreo.getText().toString().trim();
        if (!AppValidation.isValidEmail(correo)) {
            txtCorreo.setError("Escribe tu correo para recuperar la contraseña"); return;
        }
        if (!FirebaseSupport.isReady(this)) { FirebaseSupport.showSetupMessage(this); return; }
        FirebaseAuth.getInstance().sendPasswordResetEmail(correo).addOnCompleteListener(task ->
                Toast.makeText(this, task.isSuccessful() ? "Revisa tu correo electrónico" : "No se pudo enviar el enlace",
                        Toast.LENGTH_LONG).show());
    }

    private void abrirInicio() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
