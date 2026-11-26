package com.example.comunidadwestern;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AnunciosActivity extends AppCompatActivity {

    private final ArrayList<AnuncioItem> anuncios = new ArrayList<>();

    private AnuncioAdapter adapter;
    private DatabaseReference ref;
    private ValueEventListener listener;

    private EditText txtTitulo;
    private EditText txtCuerpo;
    private View panelAdmin;
    private View btnPublicar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anuncios);

        ListView list = findViewById(R.id.listaAnuncios);
        txtTitulo = findViewById(R.id.txtTituloAnuncio);
        txtCuerpo = findViewById(R.id.txtCuerpoAnuncio);
        panelAdmin = findViewById(R.id.panelAdminAnuncio);
        btnPublicar = findViewById(R.id.btnPublicarAnuncio);

        adapter = new AnuncioAdapter();
        list.setAdapter(adapter);
        list.setEmptyView(findViewById(R.id.emptyAnuncios));

        findViewById(R.id.btnVolverAnuncios).setOnClickListener(v -> finish());
        btnPublicar.setOnClickListener(v -> publicar());

        ref = FirebaseSupport.db()
                .child("announcements")
                .child(AppConfig.COMMUNITY_ID);

        comprobarRolAdministrador();
        escucharAnuncios();

        list.setOnItemClickListener((parent, view, position, itemId) -> {
            if (position < 0 || position >= anuncios.size()) {
                return;
            }

            Intent intent = new Intent(this, AnuncioDetalleActivity.class);
            intent.putExtra("announcementId", anuncios.get(position).id);
            startActivity(intent);
        });
    }

    private void comprobarRolAdministrador() {
        String uid = FirebaseSupport.uid();
        if (uid == null || uid.trim().isEmpty()) {
            panelAdmin.setVisibility(View.GONE);
            return;
        }

        FirebaseSupport.db()
                .child("roles")
                .child(uid)
                .get()
                .addOnSuccessListener(role -> {
                    String value = role.getValue(String.class);
                    panelAdmin.setVisibility(
                            "admin".equals(value) ? View.VISIBLE : View.GONE
                    );
                })
                .addOnFailureListener(error -> {
                    panelAdmin.setVisibility(View.GONE);
                    Toast.makeText(
                            this,
                            "No se pudieron comprobar los permisos de publicación",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void escucharAnuncios() {
        listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                anuncios.clear();

                for (DataSnapshot item : snapshot.getChildren()) {
                    String id = item.getKey();
                    if (id == null) {
                        continue;
                    }

                    AnuncioItem anuncio = new AnuncioItem();
                    anuncio.id = id;
                    anuncio.title = item.child("title").getValue(String.class);
                    anuncio.body = item.child("body").getValue(String.class);
                    anuncio.author = item.child("author").getValue(String.class);
                    anuncio.authorHousing = item.child("authorHousing").getValue(String.class);

                    Long createdAt = item.child("createdAt").getValue(Long.class);
                    anuncio.createdAt = createdAt == null ? 0L : createdAt;
                    anuncios.add(anuncio);
                }

                // Firebase devuelve el orden ascendente por createdAt; mostramos primero lo más reciente.
                Collections.reverse(anuncios);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(
                        AnunciosActivity.this,
                        "No se pudieron cargar los anuncios: " + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        ref.orderByChild("createdAt")
                .limitToLast(100)
                .addValueEventListener(listener);
    }

    private void publicar() {
        String title = txtTitulo.getText().toString().trim();
        String body = txtCuerpo.getText().toString().trim();

        if (title.isEmpty()) {
            txtTitulo.setError("Escribe un título");
            txtTitulo.requestFocus();
            return;
        }

        if (title.length() > 100) {
            txtTitulo.setError("El título no puede superar 100 caracteres");
            txtTitulo.requestFocus();
            return;
        }

        if (body.isEmpty()) {
            txtCuerpo.setError("Escribe el contenido");
            txtCuerpo.requestFocus();
            return;
        }

        if (body.length() > 1000) {
            txtCuerpo.setError("El contenido no puede superar 1000 caracteres");
            txtCuerpo.requestFocus();
            return;
        }

        String uid = FirebaseSupport.uid();
        btnPublicar.setEnabled(false);

        FirebaseSupport.db()
                .child("profiles")
                .child(uid)
                .get()
                .addOnSuccessListener(profile -> {
                    Map<String, Object> announcement = new HashMap<>();
                    announcement.put("title", title);
                    announcement.put("body", body);
                    announcement.put(
                            "author",
                            profile.child("name").getValue(String.class)
                    );
                    announcement.put(
                            "authorHousing",
                            profile.child("housing").getValue(String.class)
                    );
                    announcement.put("authorUid", uid);
                    announcement.put("createdAt", System.currentTimeMillis());

                    ref.push()
                            .setValue(announcement)
                            .addOnCompleteListener(task -> {
                                btnPublicar.setEnabled(true);

                                if (task.isSuccessful()) {
                                    txtTitulo.setText("");
                                    txtCuerpo.setText("");

                                    Toast.makeText(
                                            this,
                                            "Anuncio publicado. El relay FCM enviará la notificación si está activo.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                } else {
                                    String error = task.getException() == null
                                            ? "Error desconocido"
                                            : task.getException().getMessage();

                                    Toast.makeText(
                                            this,
                                            "No se pudo publicar el anuncio: " + error,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                })
                .addOnFailureListener(error -> {
                    btnPublicar.setEnabled(true);
                    Toast.makeText(
                            this,
                            "No se pudo consultar el perfil: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    @Override
    protected void onDestroy() {
        if (listener != null && ref != null) {
            ref.removeEventListener(listener);
        }
        super.onDestroy();
    }

    private static class AnuncioItem {
        String id;
        String title;
        String body;
        String author;
        String authorHousing;
        long createdAt;
    }

    private class AnuncioAdapter extends BaseAdapter {

        private final LayoutInflater inflater = LayoutInflater.from(AnunciosActivity.this);
        private final SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd/MM · HH:mm",
                Locale.getDefault()
        );

        @Override
        public int getCount() {
            return anuncios.size();
        }

        @Override
        public AnuncioItem getItem(int position) {
            return anuncios.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = inflater.inflate(R.layout.item_anuncio, parent, false);
            }

            AnuncioItem anuncio = getItem(position);

            TextView titulo = row.findViewById(R.id.txtAnuncioTituloFila);
            TextView autor = row.findViewById(R.id.txtAnuncioAutorFila);
            TextView cuerpo = row.findViewById(R.id.txtAnuncioCuerpoFila);
            TextView fecha = row.findViewById(R.id.txtAnuncioFechaFila);
            ImageView icono = row.findViewById(R.id.imgIconoAnuncio);

            titulo.setText(isBlank(anuncio.title) ? "Anuncio comunitario" : anuncio.title.trim());
            autor.setText(formatearAutor(anuncio));
            cuerpo.setText(isBlank(anuncio.body) ? "Sin contenido adicional." : anuncio.body.trim());
            fecha.setText(anuncio.createdAt > 0
                    ? "★ " + dateFormat.format(new Date(anuncio.createdAt))
                    : "★ Publicación comunitaria");
            icono.setImageResource(iconoPara(anuncio));

            return row;
        }

        private String formatearAutor(AnuncioItem anuncio) {
            String nombre = isBlank(anuncio.author) ? "Administrador" : anuncio.author.trim();
            if (isBlank(anuncio.authorHousing)) {
                return nombre;
            }
            return nombre + " · Vivienda #" + anuncio.authorHousing.trim();
        }

        private int iconoPara(AnuncioItem anuncio) {
            String texto = ((anuncio.title == null ? "" : anuncio.title) + " "
                    + (anuncio.body == null ? "" : anuncio.body)).toLowerCase(Locale.ROOT);

            if (texto.contains("manten") || texto.contains("repar") || texto.contains("espacio común")) {
                return R.drawable.ann_maintenance;
            }
            if (texto.contains("notific") || texto.contains("recordatorio") || texto.contains("aviso")) {
                return R.drawable.ann_bell;
            }
            return R.drawable.ann_megaphone;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
