package com.example.comunidadwestern;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VentasActivity extends AppCompatActivity {
    private final ArrayList<String> filas=new ArrayList<>(); private final ArrayList<DataSnapshot> productos=new ArrayList<>();
    private ArrayAdapter<String> adapter; private ValueEventListener listener; private DatabaseReference ref;
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_ventas);
        ListView list=findViewById(R.id.listaVentas); adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,filas);list.setAdapter(adapter);
        ref=FirebaseSupport.db().child("sales").child(AppConfig.COMMUNITY_ID);
        listener=new ValueEventListener(){@Override public void onDataChange(DataSnapshot s){filas.clear();productos.clear();
            for(DataSnapshot d:s.getChildren()){productos.add(d);String status=d.child("status").getValue(String.class);
                filas.add(d.child("title").getValue(String.class)+"\n$"+d.child("price").getValue(String.class)+" · "+(status==null?"":status)+"\n"+
                        d.child("sellerName").getValue(String.class)+" #"+d.child("housing").getValue(String.class));}
            adapter.notifyDataSetChanged();}
            @Override public void onCancelled(com.google.firebase.database.DatabaseError e){Toast.makeText(VentasActivity.this,"No se pudieron cargar las ventas",Toast.LENGTH_SHORT).show();}};
        ref.orderByChild("createdAt").limitToLast(100).addValueEventListener(listener);
        findViewById(R.id.btnPublicarProducto).setOnClickListener(v->publicar());findViewById(R.id.btnVolverVentas).setOnClickListener(v->finish());
        list.setOnItemClickListener((p,v,pos,id)->gestionar(productos.get(pos)));
    }
    private void publicar(){EditText t=findViewById(R.id.txtProducto),d=findViewById(R.id.txtDescripcionProducto),p=findViewById(R.id.txtPrecioProducto);
        String title=t.getText().toString().trim(),desc=d.getText().toString().trim(),price=p.getText().toString().trim();
        if(!AppValidation.isValidSale(title,desc,price)){if(title.isEmpty())t.setError("Ingresa el producto");else if(desc.isEmpty())d.setError("Describe el producto");else p.setError("Ingresa un precio válido");return;}
        FirebaseSupport.db().child("profiles").child(FirebaseSupport.uid()).get().addOnSuccessListener(profile->{Map<String,Object> item=new HashMap<>();
            item.put("title",title);item.put("description",desc);item.put("price",price);item.put("sellerUid",FirebaseSupport.uid());
            item.put("sellerName",profile.child("name").getValue(String.class));item.put("housing",profile.child("housing").getValue(String.class));
            item.put("status","DISPONIBLE");item.put("createdAt",System.currentTimeMillis());
            ref.push().setValue(item).addOnCompleteListener(task->{if(task.isSuccessful()){t.setText("");d.setText("");p.setText("");Toast.makeText(this,"Producto publicado",Toast.LENGTH_SHORT).show();}
                else Toast.makeText(this,"No se pudo publicar",Toast.LENGTH_SHORT).show();});});
    }
    private void gestionar(DataSnapshot item){String uid=item.child("sellerUid").getValue(String.class),status=item.child("status").getValue(String.class);
        if(FirebaseSupport.uid().equals(uid)&&"DISPONIBLE".equals(status))new AlertDialog.Builder(this).setTitle("Producto")
            .setMessage("¿Marcar este producto como vendido?").setNegativeButton("Cancelar",null).setPositiveButton("Marcar vendido",(a,b)->
                    ref.child(item.getKey()).child("status").setValue("VENDIDO")).show();
        else Toast.makeText(this,"Solo el vendedor puede cambiar el estado",Toast.LENGTH_SHORT).show();
    }
    @Override protected void onDestroy(){super.onDestroy();if(listener!=null&&ref!=null)ref.removeEventListener(listener);}
}
