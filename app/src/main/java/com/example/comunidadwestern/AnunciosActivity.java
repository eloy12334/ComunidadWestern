package com.example.comunidadwestern;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AnunciosActivity extends AppCompatActivity {

    private final ArrayList<String> filas = new ArrayList<>();
    private final ArrayList<String> ids = new ArrayList<>();

    private ArrayAdapter<String> adapter;
    private DatabaseReference ref;
    private ValueEventListener listener;

    private EditText txtTitulo;
    private EditText txtCuerpo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anuncios);

        ListView list = findViewById(R.id.listaAnuncios);
        txtTitulo = findViewById(R.id.txtTituloAnuncio);
        txtCuerpo = findViewById(R.id.txtCuerpoAnuncio);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                filas
        );
        list.setAdapter(adapter);

        findViewById(R.id.btnVolverAnuncios)
                .setOnClickListener(v -> finish());

        findViewById(R.id.btnPublicarAnuncio)
                .setOnClickListener(v -> publicar());

        ref = FirebaseSupport.db()
                .child("announcements")
                .child(AppConfig.COMMUNITY_ID);

        comprobarRolAdministrador();
        escucharAnuncios();

        list.setOnItemClickListener((parent, view, position, itemId) -> {
            if (position < 0 || position >= ids.size()) {
                return;
            }

            Intent intent = new Intent(this, AnuncioDetalleActivity.class);
            intent.putExtra("announcementId", ids.get(position));
            startActivity(intent);
        });
    }

    private void comprobarRolAdministrador() {
        String uid = FirebaseSupport.uid();

        FirebaseSupport.db()
                .child("roles")
                .child(uid)
                .get()
                .addOnSuccessListener(role -> {
                    String value = role.getValue(String.class);
                    findViewById(R.id.panelAdminAnuncio).setVisibility(
                            "admin".equals(value) ? View.VISIBLE : View.GONE
                    );
                })
                .addOnFailureListener(error -> {
                    findViewById(R.id.panelAdminAnuncio).setVisibility(View.GONE);
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
                filas.clear();
                ids.clear();

                for (DataSnapshot item : snapshot.getChildren()) {
                    String id = item.getKey();
                    if (id == null) {
                        continue;
                    }

                    String title = item.child("title").getValue(String.class);
                    String author = item.child("author").getValue(String.class);
                    String body = item.child("body").getValue(String.class);

                    ids.add(id);
                    filas.add(
                            (title == null ? "Anuncio" : title)
                                    + "\n"
                                    + (author == null ? "Administrador" : author)
                                    + " · "
                                    + (body == null ? "" : body)
                    );
                }

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
        findViewById(R.id.btnPublicarAnuncio).setEnabled(false);

        FirebaseSupport.db()
                .child("profiles")
                .child(uid)
                .get()
                .addOnSuccessListener(profile -> {
                    Map<String, Object> announcement = new HashMap<>();
                    announcement.put(
                            "title",
                            title
                    );
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
                                findViewById(R.id.btnPublicarAnuncio)
                                        .setEnabled(true);

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
                    findViewById(R.id.btnPublicarAnuncio).setEnabled(true);
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
}
