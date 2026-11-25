package com.example.comunidadwestern;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Online 1 vs. 1 duel using Realtime Database transactions.
 * This version does not call Firebase Cloud Functions and works with Spark.
 */
public class DueloRoomActivity extends AppCompatActivity {

    public static final String EXTRA_CODE = "room_code";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private String code;
    private String uid;
    private String hostUid;
    private String guestUid;
    private String opponentUid;
    private String phase = "";

    private int bestOf = 1;
    private int targetWins = 1;
    private int roundNumber = 0;

    private long dueAt = 0L;
    private long serverOffset = 0L;

    private boolean isHost = false;
    private boolean clockReady = false;
    private boolean shotRequestInProgress = false;

    private DatabaseReference roomRef;
    private DatabaseReference offsetRef;
    private ValueEventListener roomListener;
    private ValueEventListener offsetListener;
    private DataSnapshot lastSnapshot;

    private TextView txtCodigo;
    private TextView txtRonda;
    private TextView txtTuNombre;
    private TextView txtRivalNombre;
    private TextView txtMarcador;
    private TextView txtCuenta;
    private TextView txtEstado;
    private TextView txtDetalle;

    private Button btnDisparar;
    private Button btnAccionSala;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!"COUNTDOWN".equals(phase) || dueAt <= 0L || isFinishing()) {
                return;
            }

            long serverNow = System.currentTimeMillis() + serverOffset;
            long remaining = dueAt - serverNow;

            if (remaining > 0L) {
                int seconds = (int) Math.ceil(remaining / 1000.0);
                txtCuenta.setText(String.valueOf(seconds));
                txtEstado.setText("Espera la cuenta regresiva...");
            } else {
                txtCuenta.setText("0");
                txtEstado.setText("¡DISPARA!");
            }

            handler.postDelayed(this, 60L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duelo_sala);

        code = getIntent().getStringExtra(EXTRA_CODE);
        uid = FirebaseAuth.getInstance().getCurrentUser() == null
                ? ""
                : FirebaseAuth.getInstance().getCurrentUser().getUid();

        txtCodigo = findViewById(R.id.txtCodigoSalaTitulo);
        txtRonda = findViewById(R.id.txtRonda);
        txtTuNombre = findViewById(R.id.txtTuNombre);
        txtRivalNombre = findViewById(R.id.txtRivalNombre);
        txtMarcador = findViewById(R.id.txtMarcador);
        txtCuenta = findViewById(R.id.txtCuenta);
        txtEstado = findViewById(R.id.txtEstadoDuelo);
        txtDetalle = findViewById(R.id.txtDetalleDuelo);
        btnDisparar = findViewById(R.id.btnDisparar);
        btnAccionSala = findViewById(R.id.btnAccionSala);

        txtCodigo.setText("SALA: " + (code == null ? "------" : code));

        btnDisparar.setEnabled(false);
        btnDisparar.setOnClickListener(v -> disparar(false));
        btnAccionSala.setOnClickListener(v -> accionPrincipal());

        findViewById(R.id.btnCopiarCodigo).setOnClickListener(v -> copiarCodigo());
        findViewById(R.id.btnSalirSala).setOnClickListener(v -> finish());

        if (code == null || !code.matches("[A-F0-9]{6}") || uid.isEmpty()) {
            Toast.makeText(this, "No se pudo abrir la sala.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        roomRef = FirebaseSupport.db().child("duels").child(code);
        roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                lastSnapshot = snapshot;

                if (!snapshot.exists()) {
                    txtEstado.setText("La sala ya no está disponible.");
                    btnDisparar.setEnabled(false);
                    prepararAccion("", false, false);
                    return;
                }

                render(snapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                txtEstado.setText("No se pudo leer la partida: " + error.getMessage());
                btnDisparar.setEnabled(false);
                prepararAccion("", false, false);
            }
        };
        roomRef.addValueEventListener(roomListener);

        offsetRef = FirebaseSupport.db()
                .child(".info")
                .child("serverTimeOffset");

        offsetListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Object value = snapshot.getValue();
                if (value instanceof Number) {
                    serverOffset = ((Number) value).longValue();
                    clockReady = true;
                }

                if (lastSnapshot != null) {
                    render(lastSnapshot);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                clockReady = false;
                txtEstado.setText("No se pudo sincronizar el reloj del servidor.");
            }
        };
        offsetRef.addValueEventListener(offsetListener);
    }

    private void render(DataSnapshot snapshot) {
        handler.removeCallbacks(ticker);

        hostUid = stringValue(snapshot.child("hostUid"), "");
        guestUid = stringValue(snapshot.child("guestUid"), "");
        phase = stringValue(snapshot.child("phase"), "");
        if (!"COUNTDOWN".equals(phase)) {
            shotRequestInProgress = false;
        }

        bestOf = (int) numberValue(snapshot.child("bestOf"), 1L);
        targetWins = (int) numberValue(snapshot.child("targetWins"), 1L);
        roundNumber = (int) numberValue(snapshot.child("roundNumber"), 0L);
        dueAt = numberValue(snapshot.child("dueAt"), 0L);

        isHost = uid.equals(hostUid);
        boolean isGuest = uid.equals(guestUid);

        if (!isHost && !isGuest) {
            txtEstado.setText("Esta cuenta no pertenece a la sala.");
            btnDisparar.setEnabled(false);
            prepararAccion("", false, false);
            return;
        }

        DataSnapshot players = snapshot.child("players");
        opponentUid = isHost ? guestUid : hostUid;

        String ownName = nameOf(players.child(uid));
        String opponentName = guestUid.isEmpty()
                ? "Esperando rival"
                : nameOf(players.child(opponentUid));

        txtTuNombre.setText("TÚ\n" + ownName);
        txtRivalNombre.setText("RIVAL\n" + opponentName);

        if (roundNumber == 0) {
            txtRonda.setText("MEJOR DE " + bestOf);
        } else {
            txtRonda.setText("RONDA " + roundNumber + " / " + bestOf);
        }

        int myScore = score(snapshot.child("scores").child(uid));
        int rivalScore = opponentUid == null || opponentUid.isEmpty()
                ? 0
                : score(snapshot.child("scores").child(opponentUid));

        txtMarcador.setText(myScore + "  —  " + rivalScore);
        btnDisparar.setEnabled(false);
        prepararAccion("", false, false);

        switch (phase) {
            case "WAITING":
                txtCuenta.setText("VS");
                txtEstado.setText("Esperando que se una tu rival...");
                txtDetalle.setText("Comparte el código de sala para jugar 1 vs. 1.");
                break;

            case "READY":
                txtCuenta.setText("¡LISTOS!");
                txtDetalle.setText("La cuenta regresiva será aleatoria, entre 3 y 5 segundos.");

                if (isHost) {
                    if (clockReady) {
                        txtEstado.setText("Los dos jugadores están listos. Inicia la ronda.");
                        prepararAccion("INICIAR DUELO", true, true);
                    } else {
                        txtEstado.setText("Sincronizando reloj del servidor...");
                        prepararAccion("INICIAR DUELO", true, false);
                    }
                } else {
                    txtEstado.setText("Esperando que el anfitrión inicie la ronda.");
                }
                break;

            case "COUNTDOWN":
                txtDetalle.setText("No dispares antes de que llegue a 0. La falsa salida resta una ronda.");
                if (clockReady) {
                    btnDisparar.setEnabled(!shotRequestInProgress);
                    ticker.run();
                } else {
                    txtCuenta.setText("...");
                    txtEstado.setText("Sincronizando reloj del servidor...");
                }
                break;

            case "ROUND_RESULT":
                txtCuenta.setText("¡FIN!");
                mostrarResultado(snapshot);
                if (isHost) {
                    prepararAccion("SIGUIENTE RONDA", true, clockReady);
                } else {
                    txtDetalle.setText(txtDetalle.getText() + "\nEsperando la siguiente ronda.");
                }
                break;

            case "MATCH_FINISHED":
                txtCuenta.setText("★");
                String matchWinner = stringValue(snapshot.child("winnerUid"), "");
                txtEstado.setText(uid.equals(matchWinner)
                        ? "¡GANASTE EL DUELO!"
                        : "PERDISTE EL DUELO");
                txtDetalle.setText("Marcador final: " + myScore + " — " + rivalScore);
                prepararAccion("VOLVER AL MENÚ", true, true);
                break;

            default:
                txtCuenta.setText("VS");
                txtEstado.setText("Estado de partida desconocido.");
                txtDetalle.setText("Código de sala: " + code);
                break;
        }
    }

    private void mostrarResultado(DataSnapshot snapshot) {
        String winner = stringValue(
                snapshot.child("roundResult").child("winnerUid"), "");
        String reason = stringValue(
                snapshot.child("roundResult").child("reason"), "");

        boolean won = uid.equals(winner);

        if ("FALSE_START".equals(reason)) {
            txtEstado.setText(won
                    ? "¡FALSA SALIDA DEL RIVAL!"
                    : "¡FALSA SALIDA!");
            txtDetalle.setText(won
                    ? "Tu rival disparó antes de tiempo. Ganaste la ronda."
                    : "Disparaste antes de tiempo. Perdiste la ronda.");
        } else {
            txtEstado.setText(won ? "¡GANASTE LA RONDA!" : "PERDISTE LA RONDA");
            txtDetalle.setText(won
                    ? "Tu disparo fue el primero aceptado por Firebase."
                    : "El disparo del rival fue aceptado primero.");
        }
    }

    private void prepararAccion(String label, boolean visible, boolean enabled) {
        btnAccionSala.setText(label);
        btnAccionSala.setVisibility(visible ? View.VISIBLE : View.GONE);
        btnAccionSala.setEnabled(enabled);
    }

    private void accionPrincipal() {
        if ("MATCH_FINISHED".equals(phase)) {
            finish();
            return;
        }

        if (!isHost) return;

        if (!clockReady) {
            Toast.makeText(this, "Espera la sincronización del reloj.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!"READY".equals(phase) && !"ROUND_RESULT".equals(phase)) {
            return;
        }

        prepararAccion(btnAccionSala.getText().toString(), true, false);

        roomRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                if (currentData.getValue() == null) {
                    return Transaction.abort();
                }

                String currentPhase = stringValue(currentData.child("phase"), "");
                String currentHost = stringValue(currentData.child("hostUid"), "");
                String currentGuest = stringValue(currentData.child("guestUid"), "");

                if (!uid.equals(currentHost)
                        || currentGuest.isEmpty()
                        || (!"READY".equals(currentPhase)
                        && !"ROUND_RESULT".equals(currentPhase))) {
                    return Transaction.abort();
                }

                long currentRound = numberValue(currentData.child("roundNumber"), 0L);
                long serverNow = System.currentTimeMillis() + serverOffset;

                // The value is randomized by the host between 3 and 5 seconds.
                long delay = 3000L + random.nextInt(2001);
                long scheduledShot = serverNow + delay;

                currentData.child("roundNumber").setValue(currentRound + 1L);
                currentData.child("dueAt").setValue(scheduledShot);
                currentData.child("phase").setValue("COUNTDOWN");
                currentData.child("winnerUid").setValue("");
                currentData.child("roundResult").setValue(null);

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(
                    DatabaseError error,
                    boolean committed,
                    DataSnapshot currentData
            ) {
                if (error != null) {
                    if (lastSnapshot != null) {
                        render(lastSnapshot);
                    }
                    Toast.makeText(
                            DueloRoomActivity.this,
                            "No se pudo iniciar la ronda: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                } else if (!committed) {
                    if (lastSnapshot != null) {
                        render(lastSnapshot);
                    }
                    Toast.makeText(
                            DueloRoomActivity.this,
                            "La partida cambió de estado. Revisa la pantalla.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        }, false);
    }

    private void disparar(boolean reintento) {
        if (!"COUNTDOWN".equals(phase) || !clockReady) return;
        if (shotRequestInProgress && !reintento) return;

        if (!reintento) {
            shotRequestInProgress = true;
            btnDisparar.setEnabled(false);
            txtEstado.setText("Registrando disparo...");
        }

        roomRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                if (currentData.getValue() == null
                        || !"COUNTDOWN".equals(
                        stringValue(currentData.child("phase"), ""))) {
                    return Transaction.abort();
                }

                String currentHost = stringValue(currentData.child("hostUid"), "");
                String currentGuest = stringValue(currentData.child("guestUid"), "");

                if (!uid.equals(currentHost) && !uid.equals(currentGuest)) {
                    return Transaction.abort();
                }

                if (currentGuest.isEmpty()) {
                    return Transaction.abort();
                }

                long scheduledShot = numberValue(currentData.child("dueAt"), 0L);
                long estimatedServerNow = System.currentTimeMillis() + serverOffset;

                if (scheduledShot <= 0L) {
                    return Transaction.abort();
                }

                boolean falseStart = estimatedServerNow < scheduledShot;
                String opponent = uid.equals(currentHost) ? currentGuest : currentHost;
                String roundWinner = falseStart ? opponent : uid;

                long oldScore = numberValue(
                        currentData.child("scores").child(roundWinner), 0L);
                long newScore = oldScore + 1L;
                long requiredWins = numberValue(currentData.child("targetWins"), 1L);
                long currentRound = numberValue(currentData.child("roundNumber"), 0L);

                currentData.child("scores").child(roundWinner).setValue(newScore);

                Map<String, Object> result = new HashMap<>();
                result.put("winnerUid", roundWinner);
                result.put("shooterUid", uid);
                result.put("reason", falseStart ? "FALSE_START" : "QUICK_DRAW");
                result.put("roundNumber", currentRound);
                result.put("shotAt", estimatedServerNow);

                currentData.child("roundResult").setValue(result);

                boolean matchFinished = newScore >= requiredWins;
                currentData.child("phase").setValue(
                        matchFinished ? "MATCH_FINISHED" : "ROUND_RESULT");
                currentData.child("winnerUid").setValue(
                        matchFinished ? roundWinner : "");

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(
                    DatabaseError error,
                    boolean committed,
                    DataSnapshot currentData
            ) {
                if (error != null) {
                    long estimatedNow = System.currentTimeMillis() + serverOffset;

                    // If the request crossed the countdown boundary in transit,
                    // retry once using the updated server-clock estimate.
                    if (!reintento
                            && error.getCode() == DatabaseError.PERMISSION_DENIED
                            && phase.equals("COUNTDOWN")
                            && estimatedNow >= dueAt) {
                        handler.postDelayed(() -> disparar(true), 150L);
                        return;
                    }

                    shotRequestInProgress = false;
                    btnDisparar.setEnabled("COUNTDOWN".equals(phase));
                    Toast.makeText(
                            DueloRoomActivity.this,
                            "No se pudo registrar el disparo: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                shotRequestInProgress = false;

                if (!committed) {
                    btnDisparar.setEnabled(false);
                    Toast.makeText(
                            DueloRoomActivity.this,
                            "La ronda ya fue resuelta o dejó de estar activa.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        }, false);
    }

    private void copiarCodigo() {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard != null) {
            clipboard.setPrimaryClip(
                    ClipData.newPlainText("Código de sala", code));
            Toast.makeText(this, "Código copiado", Toast.LENGTH_SHORT).show();
        }
    }

    private String nameOf(DataSnapshot player) {
        String name = stringValue(player.child("name"), "Vaquero");
        String housing = stringValue(player.child("housing"), "");
        return housing.isEmpty() ? name : name + " · " + housing;
    }

    private String stringValue(DataSnapshot snapshot, String fallback) {
        String value = snapshot.getValue(String.class);
        return value == null ? fallback : value;
    }

    private String stringValue(MutableData data, String fallback) {
        String value = data.getValue(String.class);
        return value == null ? fallback : value;
    }

    private long numberValue(DataSnapshot snapshot, long fallback) {
        Object value = snapshot.getValue();
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    private long numberValue(MutableData data, long fallback) {
        Object value = data.getValue();
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    private int score(DataSnapshot snapshot) {
        return (int) numberValue(snapshot, 0L);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);

        if (roomRef != null && roomListener != null) {
            roomRef.removeEventListener(roomListener);
        }

        if (offsetRef != null && offsetListener != null) {
            offsetRef.removeEventListener(offsetListener);
        }

        super.onDestroy();
    }
}
