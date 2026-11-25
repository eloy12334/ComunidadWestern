package com.example.comunidadwestern;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.HashMap;
import java.util.Map;

public class PerfilActivity extends AppCompatActivity {
    private EditText txtNombre,txtVivienda;private TextView txtCorreo,txtRol;
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_perfil);
        txtNombre=findViewById(R.id.txtNombrePerfil);txtVivienda=findViewById(R.id.txtViviendaPerfil);txtCorreo=findViewById(R.id.txtCorreoPerfil);txtRol=findViewById(R.id.txtRolPerfil);
        FirebaseSupport.db().child("profiles").child(FirebaseSupport.uid()).get().addOnSuccessListener(p->{txtNombre.setText(p.child("name").getValue(String.class));txtVivienda.setText(p.child("housing").getValue(String.class));txtCorreo.setText(p.child("email").getValue(String.class));});
        FirebaseSupport.db().child("roles").child(FirebaseSupport.uid()).get().addOnSuccessListener(r->txtRol.setText("admin".equals(r.getValue(String.class))?"Administrador":"Vecino"));
        findViewById(R.id.btnGuardarPerfil).setOnClickListener(v->guardar());findViewById(R.id.btnCerrarSesionPerfil).setOnClickListener(v->{FirebaseAuth.getInstance().signOut();startActivity(new Intent(this,MainActivity.class));finishAffinity();});
        findViewById(R.id.btnVolverPerfil).setOnClickListener(v->finish());
    }
    private void guardar(){String name=txtNombre.getText().toString().trim(),housing=txtVivienda.getText().toString().trim();
        if(name.isEmpty()){txtNombre.setError("Ingresa tu nombre");return;}if(!AppValidation.isValidHousing(housing)){txtVivienda.setError("Vivienda no válida");return;}
        Map<String,Object> update=new HashMap<>();update.put("name",name);update.put("housing",housing);
        FirebaseSupport.db().child("profiles").child(FirebaseSupport.uid()).updateChildren(update).addOnCompleteListener(t->Toast.makeText(this,t.isSuccessful()?"Perfil actualizado":"No se pudo actualizar",Toast.LENGTH_SHORT).show());
    }
}
