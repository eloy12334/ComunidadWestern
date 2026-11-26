package com.example.comunidadwestern;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VentasActivity extends AppCompatActivity {

    private final ArrayList<DataSnapshot> productos = new ArrayList<>();
    private VentaAdapter adapter;
    private ValueEventListener listener;
    private DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ventas);

        ListView lista = findViewById(R.id.listaVentas);
        adapter = new VentaAdapter();
        lista.setAdapter(adapter);
        lista.setDivider(null);
        lista.setDividerHeight(0);

        ref = FirebaseSupport.db().child("sales").child(AppConfig.COMMUNITY_ID);
        listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                productos.clear();
                for (DataSnapshot item : snapshot.getChildren()) {
                    productos.add(item);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                Toast.makeText(VentasActivity.this,
                        "No se pudieron cargar las ventas", Toast.LENGTH_SHORT).show();
            }
        };
        ref.orderByChild("createdAt").limitToLast(100).addValueEventListener(listener);

        findViewById(R.id.btnPublicarProducto).setOnClickListener(v -> publicar());
        ImageButton volver = findViewById(R.id.btnVolverVentas);
        volver.setOnClickListener(v -> finish());

        lista.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < productos.size()) {
                gestionar(productos.get(position));
            }
        });
    }

    private void publicar() {
        EditText nombre = findViewById(R.id.txtProducto);
        EditText descripcion = findViewById(R.id.txtDescripcionProducto);
        EditText precio = findViewById(R.id.txtPrecioProducto);

        String title = nombre.getText().toString().trim();
        String desc = descripcion.getText().toString().trim();
        String price = precio.getText().toString().trim();

        if (!AppValidation.isValidSale(title, desc, price)) {
            if (title.isEmpty()) {
                nombre.setError("Ingresa el producto");
            } else if (desc.isEmpty()) {
                descripcion.setError("Describe el producto");
            } else {
                precio.setError("Ingresa un precio válido");
            }
            return;
        }

        FirebaseSupport.db().child("profiles").child(FirebaseSupport.uid()).get()
                .addOnSuccessListener(profile -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("title", title);
                    item.put("description", desc);
                    item.put("price", price);
                    item.put("sellerUid", FirebaseSupport.uid());
                    item.put("sellerName", profile.child("name").getValue(String.class));
                    item.put("housing", profile.child("housing").getValue(String.class));
                    item.put("status", "DISPONIBLE");
                    item.put("createdAt", System.currentTimeMillis());

                    ref.push().setValue(item).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            nombre.setText("");
                            descripcion.setText("");
                            precio.setText("");
                            Toast.makeText(this, "Producto publicado", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "No se pudo publicar", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(error -> Toast.makeText(
                        this, "No se pudo obtener tu perfil", Toast.LENGTH_SHORT).show());
    }

    private void gestionar(DataSnapshot item) {
        String uid = item.child("sellerUid").getValue(String.class);
        String status = item.child("status").getValue(String.class);

        if (FirebaseSupport.uid().equals(uid) && "DISPONIBLE".equals(status)) {
            new AlertDialog.Builder(this)
                    .setTitle("Producto")
                    .setMessage("¿Marcar este producto como vendido?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Marcar vendido", (dialog, which) ->
                            ref.child(item.getKey()).child("status").setValue("VENDIDO"))
                    .show();
        } else {
            Toast.makeText(this,
                    "Solo el vendedor puede cambiar el estado", Toast.LENGTH_SHORT).show();
        }
    }

    private class VentaAdapter extends BaseAdapter {
        private final LayoutInflater inflater = LayoutInflater.from(VentasActivity.this);

        @Override
        public int getCount() {
            return productos.size();
        }

        @Override
        public DataSnapshot getItem(int position) {
            return productos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VentaHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_venta, parent, false);
                holder = new VentaHolder();
                holder.title = convertView.findViewById(R.id.txtVentaTitulo);
                holder.description = convertView.findViewById(R.id.txtVentaDescripcion);
                holder.price = convertView.findViewById(R.id.txtVentaPrecio);
                holder.seller = convertView.findViewById(R.id.txtVentaVendedor);
                holder.soldStamp = convertView.findViewById(R.id.imgVentaVendido);
                holder.productIcon = convertView.findViewById(R.id.imgIconoProducto);
                convertView.setTag(holder);
            } else {
                holder = (VentaHolder) convertView.getTag();
            }

            DataSnapshot product = getItem(position);
            String title = valueOr(product.child("title").getValue(String.class), "Producto sin nombre");
            String description = valueOr(product.child("description").getValue(String.class), "Sin descripción");
            String rawPrice = valueOr(product.child("price").getValue(String.class), "0");
            String seller = valueOr(product.child("sellerName").getValue(String.class), "Vecino");
            String housing = valueOr(product.child("housing").getValue(String.class), "-");
            String status = valueOr(product.child("status").getValue(String.class), "DISPONIBLE");
            Long createdAt = product.child("createdAt").getValue(Long.class);

            String date = createdAt == null ? "" : new SimpleDateFormat(
                    "dd/MM HH:mm", Locale.getDefault()).format(new Date(createdAt));

            holder.title.setText(title);
            holder.description.setText(description);
            holder.price.setText("$" + rawPrice + " CLP");
            holder.seller.setText(seller + " · #" + housing + (date.isEmpty() ? "" : " · " + date));
            holder.soldStamp.setVisibility("VENDIDO".equalsIgnoreCase(status) ? View.VISIBLE : View.GONE);
            holder.productIcon.setImageResource(R.drawable.sales_crate);

            String accessibility = title + ". " + description + ". Precio: " + rawPrice
                    + " pesos. Publicado por " + seller + ", vivienda " + housing
                    + ". Estado: " + status + ". Toca para ver opciones.";
            convertView.setContentDescription(accessibility);
            return convertView;
        }

        private String valueOr(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value;
        }
    }

    private static class VentaHolder {
        TextView title;
        TextView description;
        TextView price;
        TextView seller;
        ImageView soldStamp;
        ImageView productIcon;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null && ref != null) {
            ref.removeEventListener(listener);
        }
    }
}
