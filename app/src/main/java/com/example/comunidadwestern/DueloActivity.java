package com.example.comunidadwestern;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Menu and room creation/join for Duelo del Oeste.
 * Uses Realtime Database transactions only; no Cloud Functions required.
 */
public class DueloActivity extends AppCompatActivity {

    private static final SecureRandom RANDOM = new SecureRandom();

    private RadioGroup rgModo;
    private EditText txtCodigo;
    private Button btnCrearSala;
    private Button btnUnirseSala;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duelo);

        rgModo = findViewById(R.id.rgModo);
        txtCodigo = findViewById(R.id.txtCodigoSala);
        btnCrearSala = findViewById(R.id.btnCrearSala);
        btnUnirseSala = findViewById(R.id.btnUnirseSala);

        btnCrearSala.setOnClickListener(v -> crearSala());
        btnUnirseSala.setOnClickListener(v -> unirseSala());
        findViewById(R.id.btnVolverDuelo).setOnClickListener(v -> finish());
        findViewById(R.id.btnComoJugar).setOnClickListener(v -> mostrarReglas());
    }

    private boolean validarSesion() {
        if (!FirebaseSupport.isReady(this)
                || FirebaseAuth.getInstance().getCurrentUser() == null) {
            FirebaseSupport.showSetupMessage(this);
            return false;
        }
        return true;
    }

    private String uidActual() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private int modoSeleccionado() {
        int selected = rgModo.getCheckedRadioButtonId();
        if (selected == R.id.rbMejor5) return 5;
        if (selected == R.id.rbMejor3) return 3;
        return 1;
    }

    private int victoriasNecesarias(int bestOf) {
        return (bestOf / 2) + 1;
    }

    private void crearSala() {
        if (!validarSesion()) return;

        setBotonesHabilitados(false);

        String uid = uidActual();
        int bestOf = modoSeleccionado();

        FirebaseSupport.db()
                .child("profiles")
                .child(uid)
                .get()
                .addOnSuccessListener(profile -> {
                    String name = profile.child("name").getValue(String.class);
                    String housing = profile.child("housing").getValue(String.class);

                    if (name == null || name.trim().isEmpty()
                            || housing == null || housing.trim().isEmpty()) {
                        setBotonesHabilitados(true);
                        mostrarError("Completa tu nombre y vivienda en Mi perfil antes de jugar.");
                        return;
                    }

                    crearSalaConPerfil(uid, name, housing, bestOf, 0);
                })
                .addOnFailureListener(error -> {
                    setBotonesHabilitados(true);
                    mostrarError("No se pudo consultar tu perfil: " + error.getMessage());
                });
    }

    private void crearSalaConPerfil(
            String uid,
            String name,
            String housing,
            int bestOf,
            int attempt
    ) {
        if (attempt >= 5) {
            setBotonesHabilitados(true);
            mostrarError("No se pudo generar un código de sala. Intenta nuevamente.");
            return;
        }

        String code = generarCodigoSala();
        DatabaseReference roomRef = FirebaseSupport.db()
                .child("duels")
                .child(code);

        Map<String, Object> player = new HashMap<>();
        player.put("name", name);
        player.put("housing", housing);

        Map<String, Object> players = new HashMap<>();
        players.put(uid, player);

        Map<String, Object> scores = new HashMap<>();
        scores.put(uid, 0L);

        Map<String, Object> room = new HashMap<>();
        room.put("code", code);
        room.put("hostUid", uid);
        room.put("guestUid", "");
        room.put("players", players);
        room.put("bestOf", bestOf);
        room.put("targetWins", victoriasNecesarias(bestOf));
        room.put("scores", scores);
        room.put("phase", "WAITING");
        room.put("roundNumber", 0L);
        room.put("dueAt", 0L);
        room.put("winnerUid", "");
        room.put("createdAt", System.currentTimeMillis());

        roomRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                // Do not overwrite a room if the random code already exists.
                if (currentData.getValue() != null) {
                    return Transaction.abort();
                }

                currentData.setValue(room);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(
                    DatabaseError error,
                    boolean committed,
                    DataSnapshot currentData
            ) {
                if (error != null) {
                    setBotonesHabilitados(true);
                    mostrarError("No se pudo crear la sala: " + error.getMessage());
                } else if (committed) {
                    abrirSala(code);
                } else {
                    // A code collision is unlikely; generate another one.
                    crearSalaConPerfil(uid, name, housing, bestOf, attempt + 1);
                }
            }
        }, false);
    }

    private void unirseSala() {
        if (!validarSesion()) return;

        String code = txtCodigo.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-F0-9]{6}")) {
            txtCodigo.setError("Ingresa un código de 6 caracteres hexadecimales");
            return;
        }

        setBotonesHabilitados(false);

        String uid = uidActual();

        FirebaseSupport.db()
                .child("profiles")
                .child(uid)
                .get()
                .addOnSuccessListener(profile -> {
                    String name = profile.child("name").getValue(String.class);
                    String housing = profile.child("housing").getValue(String.class);

                    if (name == null || name.trim().isEmpty()
                            || housing == null || housing.trim().isEmpty()) {
                        setBotonesHabilitados(true);
                        mostrarError("Completa tu nombre y vivienda en Mi perfil antes de jugar.");
                        return;
                    }

                    unirseSalaConPerfil(code, uid, name, housing);
                })
                .addOnFailureListener(error -> {
                    setBotonesHabilitados(true);
                    mostrarError("No se pudo consultar tu perfil: " + error.getMessage());
                });
    }

    private void unirseSalaConPerfil(
            String code,
            String uid,
            String name,
            String housing
    ) {
        DatabaseReference roomRef = FirebaseSupport.db()
                .child("duels")
                .child(code);

        // First check whether this account already belongs to the room.
        roomRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                setBotonesHabilitados(true);
                mostrarError("No existe una sala con ese código.");
                return;
            }

            if (snapshot.child("players").child(uid).exists()) {
                abrirSala(code);
                return;
            }

            String phase = snapshot.child("phase").getValue(String.class);
            String guestUid = snapshot.child("guestUid").getValue(String.class);

            if (!"WAITING".equals(phase) || (guestUid != null && !guestUid.isEmpty())) {
                setBotonesHabilitados(true);
                mostrarError("La sala ya tiene dos jugadores o el duelo ya comenzó.");
                return;
            }

            roomRef.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData currentData) {
                    if (currentData.getValue() == null) {
                        return Transaction.abort();
                    }

                    String currentPhase = currentData.child("phase").getValue(String.class);
                    String hostUid = currentData.child("hostUid").getValue(String.class);
                    String currentGuest = currentData.child("guestUid").getValue(String.class);

                    if (!"WAITING".equals(currentPhase)
                            || hostUid == null
                            || hostUid.equals(uid)
                            || (currentGuest != null && !currentGuest.isEmpty())) {
                        return Transaction.abort();
                    }

                    currentData.child("players").child(uid).child("name").setValue(name);
                    currentData.child("players").child(uid).child("housing").setValue(housing);
                    currentData.child("scores").child(uid).setValue(0L);
                    currentData.child("guestUid").setValue(uid);
                    currentData.child("phase").setValue("READY");

                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(
                        DatabaseError error,
                        boolean committed,
                        DataSnapshot currentData
                ) {
                    if (error != null) {
                        setBotonesHabilitados(true);
                        mostrarError("No se pudo unir al duelo: " + error.getMessage());
                    } else if (committed) {
                        abrirSala(code);
                    } else {
                        setBotonesHabilitados(true);
                        mostrarError("Otro jugador ocupó la sala o el anfitrión ya comenzó.");
                    }
                }
            }, false);
        }).addOnFailureListener(error -> {
            setBotonesHabilitados(true);
            mostrarError("No se pudo consultar la sala: " + error.getMessage());
        });
    }

    private String generarCodigoSala() {
        return String.format(Locale.ROOT, "%06X", RANDOM.nextInt(0x1000000));
    }

    private void abrirSala(String code) {
        setBotonesHabilitados(true);
        Intent intent = new Intent(this, DueloRoomActivity.class);
        intent.putExtra(DueloRoomActivity.EXTRA_CODE, code);
        startActivity(intent);
    }

    private void setBotonesHabilitados(boolean enabled) {
        btnCrearSala.setEnabled(enabled);
        btnUnirseSala.setEnabled(enabled);
    }

    private void mostrarReglas() {
        new AlertDialog.Builder(this)
                .setTitle("Cómo jugar")
                .setMessage(
                        "1. Crea una sala y comparte el código con otro vecino.\n\n" +
                        "2. El anfitrión inicia la ronda. La cuenta será aleatoria, entre 3 y 5 segundos.\n\n" +
                        "3. Puedes disparar durante la cuenta: si el servidor recibe el disparo antes de llegar a 0, es falsa salida y pierdes la ronda.\n\n" +
                        "4. En mejor de 1, perder la ronda significa perder el duelo completo. En mejor de 3 o 5, pierdes solamente esa ronda.\n\n" +
                        "5. Gana quien llegue primero a la cantidad de rondas necesaria.\n\n" +
                        "La primera jugada aceptada por Firebase resuelve la ronda."
                )
                .setPositiveButton("Entendido", null)
                .show();
    }

    private void mostrarError(String message) {
        Toast.makeText(
                this,
                message == null || message.trim().isEmpty()
                        ? "No se pudo completar la operación."
                        : message,
                Toast.LENGTH_LONG
        ).show();
    }
}
