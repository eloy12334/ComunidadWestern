package com.example.comunidadwestern;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private final ArrayList<String> mensajes=new ArrayList<>();
    private ArrayAdapter<String> adapter; private DatabaseReference ref; private Query consulta;
    private ValueEventListener listener;
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_chat);
        ListView lista=findViewById(R.id.listaMensajes); adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,mensajes); lista.setAdapter(adapter);
        ref=FirebaseSupport.db().child("messages").child(AppConfig.COMMUNITY_ID);
        consulta=ref.orderByChild("createdAt").limitToLast(50);
        listener=new ValueEventListener(){@Override public void onDataChange(DataSnapshot s){
            mensajes.clear(); for(DataSnapshot item:s.getChildren()){
                String name=item.child("name").getValue(String.class), home=item.child("housing").getValue(String.class), body=item.child("text").getValue(String.class);
                Long time=item.child("createdAt").getValue(Long.class);
                String fecha=time==null?"":new SimpleDateFormat("dd/MM HH:mm",Locale.getDefault()).format(new Date(time));
                mensajes.add((name==null?"Vecino":name)+" · #"+(home==null?"-":home)+"\n"+(body==null?"":body)+"\n"+fecha);
            } adapter.notifyDataSetChanged(); lista.setSelection(Math.max(0,mensajes.size()-1));
        } @Override public void onCancelled(com.google.firebase.database.DatabaseError e){ Toast.makeText(ChatActivity.this,"No se pudo cargar el chat",Toast.LENGTH_SHORT).show(); }};
        consulta.addValueEventListener(listener);
        findViewById(R.id.btnEnviarMensaje).setOnClickListener(v -> enviar());
        findViewById(R.id.btnVolverChat).setOnClickListener(v -> finish());
    }
    private void enviar(){
        EditText txt=findViewById(R.id.txtMensaje); String contenido=txt.getText().toString().trim();
        if(contenido.isEmpty())return; if(contenido.length()>500){txt.setError("Máximo 500 caracteres");return;}
        FirebaseSupport.db().child("profiles").child(FirebaseSupport.uid()).get().addOnSuccessListener(p->{
            Map<String,Object> m=new HashMap<>(); m.put("uid",FirebaseSupport.uid()); m.put("name",p.child("name").getValue(String.class));
            m.put("housing",p.child("housing").getValue(String.class)); m.put("text",contenido); m.put("createdAt",System.currentTimeMillis());
            ref.push().setValue(m).addOnCompleteListener(t->{if(t.isSuccessful())txt.setText("");else Toast.makeText(this,"No se pudo enviar el mensaje",Toast.LENGTH_SHORT).show();});
        });
    }
    @Override protected void onDestroy(){super.onDestroy(); if(consulta!=null&&listener!=null)consulta.removeEventListener(listener);}
}
