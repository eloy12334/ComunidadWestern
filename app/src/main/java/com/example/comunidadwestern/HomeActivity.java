package com.example.comunidadwestern;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;

public class HomeActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_home);
        if (!FirebaseSupport.isReady(this) || FirebaseAuth.getInstance().getCurrentUser()==null) { salir(); return; }
        pedirPermisoNotificaciones();
        FirebaseSupport.subscribeToCommunity();
        FirebaseSupport.db().child("profiles").child(FirebaseSupport.uid()).get().addOnSuccessListener(s -> {
            String nombre=s.child("name").getValue(String.class);
            String vivienda=s.child("housing").getValue(String.class);
            ((TextView)findViewById(R.id.txtBienvenida)).setText("Hola, " + (nombre==null?"vecino":nombre));
            ((TextView)findViewById(R.id.txtViviendaHome)).setText("Vivienda #" + (vivienda==null?"-":vivienda));
        });
        findViewById(R.id.btnChat).setOnClickListener(v -> abrir(ChatActivity.class));
        findViewById(R.id.btnVentas).setOnClickListener(v -> abrir(VentasActivity.class));
        findViewById(R.id.btnAnuncios).setOnClickListener(v -> abrir(AnunciosActivity.class));
        findViewById(R.id.btnAlertas).setOnClickListener(v -> abrir(AlertasActivity.class));
        findViewById(R.id.btnDuelo).setOnClickListener(v -> abrir(DueloActivity.class));
        findViewById(R.id.btnPerfil).setOnClickListener(v -> abrir(PerfilActivity.class));
        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> salir());
    }
    private void abrir(Class<?> c){ startActivity(new Intent(this,c)); }
    private void salir(){ if(FirebaseSupport.isReady(this)) FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this,MainActivity.class)); finish(); }
    private void pedirPermisoNotificaciones(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!= PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},40);
    }
}
