package com.example.comunidadwestern;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AnuncioDetalleActivity extends AppCompatActivity {

    private TextView txtTitulo;
    private TextView txtCuerpo;
    private TextView txtAutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anuncio_detalle);

        txtTitulo = findViewById(R.id.txtDetalleTitulo);
        txtCuerpo = findViewById(R.id.txtDetalleCuerpo);
        txtAutor = findViewById(R.id.txtDetalleAutor);

        findViewById(R.id.btnVolverDetalleAnuncio)
                .setOnClickListener(v -> finish());

        cargarAnuncio(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        cargarAnuncio(intent);
    }

    private void cargarAnuncio(Intent intent) {
        String id = intent == null
                ? null
                : intent.getStringExtra("announcementId");

        if (id == null || id.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "No se recibió el identificador del anuncio",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        txtTitulo.setText("Cargando anuncio...");
        txtCuerpo.setText("");
        txtAutor.setText("");

        FirebaseSupport.db()
                .child("announcements")
                .child(AppConfig.COMMUNITY_ID)
                .child(id)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        txtTitulo.setText("Anuncio no disponible");
                        txtCuerpo.setText("El anuncio ya no existe o no está disponible.");
                        txtAutor.setText("");
                        return;
                    }

                    String title = snapshot.child("title").getValue(String.class);
                    String body = snapshot.child("body").getValue(String.class);
                    String author = snapshot.child("author").getValue(String.class);

                    txtTitulo.setText(title == null ? "Anuncio comunitario" : title);
                    txtCuerpo.setText(body == null ? "" : body);
                    txtAutor.setText(
                            "Publicado por " + (author == null ? "Administrador" : author)
                    );
                })
                .addOnFailureListener(error -> {
                    txtTitulo.setText("No se pudo cargar el anuncio");
                    txtCuerpo.setText(error.getMessage());
                    txtAutor.setText("");
                });
    }
}
