
package com.example.comunidadwestern;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AlertasActivity extends AppCompatActivity {

    private final ArrayList<String> filas = new ArrayList<>();
    private final ArrayList<DataSnapshot> alertas = new ArrayList<>();

    private ArrayAdapter<String> adapter;

    private DatabaseReference ref;
    private ValueEventListener listener;

    private MqttManager mqtt;
    private TextView txtEstadoMqtt;

    private boolean admin = false;
    private boolean permisosCargados = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alertas);

        ListView list = findViewById(R.id.listaAlertas);
        txtEstadoMqtt = findViewById(R.id.txtEstadoMqtt);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                filas
        );

        list.setAdapter(adapter);

        findViewById(R.id.btnVolverAlertas)
                .setOnClickListener(v -> finish());

        findViewById(R.id.btnActivarAlerta)
                .setOnClickListener(v -> confirmarAlerta());

        ref = FirebaseSupport.db()
                .child("alerts")
                .child(AppConfig.COMMUNITY_ID);

        comprobarPermisos();

        listener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {

                filas.clear();
                alertas.clear();

                for (DataSnapshot d : snapshot.getChildren()) {

                    alertas.add(d);

                    String nombre = d.child("name")
                            .getValue(String.class);

                    String vivienda = d.child("housing")
                            .getValue(String.class);

                    String estado = d.child("status")
                            .getValue(String.class);

                    if (nombre == null) nombre = "Usuario";
                    if (vivienda == null) vivienda = "?";
                    if (estado == null) estado = "DESCONOCIDO";

                    filas.add(
                            nombre + " · #" + vivienda
                                    + "\nEstado: " + estado
                                    + " · ID: " + d.getKey()
                    );
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {

                Toast.makeText(
                        AlertasActivity.this,
                        "No se pudo cargar el historial: "
                                + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        ref.orderByChild("createdAt")
                .limitToLast(50)
                .addValueEventListener(listener);

        list.setOnItemClickListener((parent, view, position, id) -> {

            if (position >= 0 && position < alertas.size()) {
                gestionar(alertas.get(position));
            }
        });

        mqtt = new MqttManager(
                this,
                new MqttManager.Listener() {

                    @Override
                    public void onStatus(String status) {

                        runOnUiThread(() -> {

                            if (txtEstadoMqtt != null) {
                                txtEstadoMqtt.setText(status);
                            }
                        });
                    }

                    @Override
                    public void onMessage(String payload) {
                        procesarEvento(payload);
                    }
                }
        );
    }

    private void comprobarPermisos() {

        String uid = FirebaseSupport.uid();

        if (uid == null || uid.isEmpty()) {
            permisosCargados = true;
            admin = false;
            return;
        }

        FirebaseSupport.db()
                .child("roles")
                .child(uid)
                .get()
                .addOnCompleteListener(task -> {

                    permisosCargados = true;

                    if (task.isSuccessful()
                            && task.getResult() != null) {

                        String rol = task.getResult()
                                .getValue(String.class);

                        admin = "admin".equalsIgnoreCase(rol);

                        if (admin) {
                            Toast.makeText(
                                    this,
                                    "Permisos de administrador activos",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                    } else {

                        admin = false;

                        Toast.makeText(
                                this,
                                "No se pudieron comprobar los permisos",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mqtt != null) {
            mqtt.connect();
        }
    }

    @Override
    protected void onStop() {

        if (mqtt != null) {
            mqtt.disconnect();
        }

        super.onStop();
    }

    private void confirmarAlerta() {

        new AlertDialog.Builder(this)
                .setTitle("Alerta comunitaria")
                .setMessage(
                        "¿Confirmas que deseas enviar una alerta a la comunidad?"
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(
                        "Enviar alerta",
                        (dialog, which) -> crearAlerta()
                )
                .show();
    }

    private void crearAlerta() {

        long now = System.currentTimeMillis();

        if (!AppValidation.canSendAlert(
                getPreferences(0).getLong("last_alert", 0),
                now
        )) {

            Toast.makeText(
                    this,
                    "Espera 15 segundos antes de activar otra alerta",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        getPreferences(0)
                .edit()
                .putLong("last_alert", now)
                .apply();

        findViewById(R.id.btnActivarAlerta)
                .setEnabled(false);

        String uid = FirebaseSupport.uid();

        FirebaseSupport.db()
                .child("profiles")
                .child(uid)
                .get()
                .addOnSuccessListener(profile -> {

                    String id = AppValidation.generateAlertId();

                    Map<String, Object> alerta = new HashMap<>();

                    alerta.put("alertId", id);
                    alerta.put("communityId", AppConfig.COMMUNITY_ID);
                    alerta.put("uid", uid);
                    alerta.put(
                            "name",
                            profile.child("name").getValue(String.class)
                    );
                    alerta.put(
                            "housing",
                            profile.child("housing").getValue(String.class)
                    );
                    alerta.put("createdAt", now);
                    alerta.put("status", "PENDIENTE");

                    ref.child(id)
                            .setValue(alerta)
                            .addOnCompleteListener(save -> {

                                if (!save.isSuccessful()) {

                                    findViewById(R.id.btnActivarAlerta)
                                            .setEnabled(true);

                                    Toast.makeText(
                                            this,
                                            "No se pudo registrar la alerta",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                try {

                                    JSONObject json = new JSONObject();

                                    json.put("alertId", id);
                                    json.put(
                                            "communityId",
                                            AppConfig.COMMUNITY_ID
                                    );
                                    json.put("uid", uid);
                                    json.put("name", alerta.get("name"));
                                    json.put("housing", alerta.get("housing"));
                                    json.put("createdAt", now);

                                    mqtt.publish(
                                            json.toString(),
                                            new MqttManager.PublishCallback() {

                                                @Override
                                                public void onSuccess() {

                                                    ref.child(id)
                                                            .child("status")
                                                            .setValue("ENVIADA");

                                                    runOnUiThread(() -> {

                                                        findViewById(
                                                                R.id.btnActivarAlerta
                                                        ).setEnabled(true);

                                                        Toast.makeText(
                                                                AlertasActivity.this,
                                                                "Alerta enviada por MQTT",
                                                                Toast.LENGTH_LONG
                                                        ).show();
                                                    });
                                                }

                                                @Override
                                                public void onFailure(String error) {

                                                    runOnUiThread(() -> {

                                                        findViewById(
                                                                R.id.btnActivarAlerta
                                                        ).setEnabled(true);

                                                        Toast.makeText(
                                                                AlertasActivity.this,
                                                                "Quedó pendiente: " + error,
                                                                Toast.LENGTH_LONG
                                                        ).show();
                                                    });
                                                }
                                            }
                                    );

                                } catch (Exception error) {

                                    findViewById(R.id.btnActivarAlerta)
                                            .setEnabled(true);

                                    Toast.makeText(
                                            this,
                                            "No se pudo preparar el evento",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                })
                .addOnFailureListener(error -> {

                    findViewById(R.id.btnActivarAlerta)
                            .setEnabled(true);

                    Toast.makeText(
                            this,
                            "No se pudo consultar el perfil",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void procesarEvento(String payload) {

        try {

            JSONObject json = new JSONObject(payload);

            String id = json.optString("alertId");

            if (!AppValidation.isNewAlertId(
                    id,
                    getSharedPreferences(
                            "mqtt_eventos",
                            MODE_PRIVATE
                    ).getAll().keySet()
            )) {
                return;
            }

            getSharedPreferences(
                    "mqtt_eventos",
                    MODE_PRIVATE
            ).edit().putBoolean(id, true).apply();

            String body = json.optString("name")
                    + " · Vivienda #"
                    + json.optString("housing")
                    + " ha solicitado ayuda";

            NotificationHelper.show(
                    this,
                    "Alerta comunitaria",
                    body,
                    AlertasActivity.class,
                    id.hashCode()
            );

            runOnUiThread(() ->
                    Toast.makeText(
                            this,
                            body,
                            Toast.LENGTH_LONG
                    ).show()
            );

        } catch (Exception ignored) {
            // Ignorar mensajes con formato incorrecto.
        }
    }

    private void gestionar(DataSnapshot item) {

        if (!permisosCargados) {

            Toast.makeText(
                    this,
                    "Comprobando permisos de administrador...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!admin) {
            confirmarRecepcion(item);
            return;
        }

        String key = item.getKey();

        if (key == null) {
            return;
        }

        FirebaseSupport.db()
                .child("alertReceipts")
                .child(AppConfig.COMMUNITY_ID)
                .child(key)
                .get()
                .addOnSuccessListener(receipts -> {

                    StringBuilder detalle =
                            new StringBuilder(
                                    "Confirmaciones de lectura: "
                            );

                    if (!receipts.exists()) {

                        detalle.append("ninguna todavía");

                    } else {

                        detalle.append(receipts.getChildrenCount())
                                .append("\n");

                        for (DataSnapshot receipt : receipts.getChildren()) {

                            String nombre = receipt.child("name")
                                    .getValue(String.class);

                            String vivienda = receipt.child("housing")
                                    .getValue(String.class);

                            detalle.append("• ")
                                    .append(nombre == null
                                            ? "Usuario" : nombre)
                                    .append(" #")
                                    .append(vivienda == null
                                            ? "?" : vivienda)
                                    .append("\n");
                        }
                    }

                    // Acciones independientes en botones visibles.
                    new AlertDialog.Builder(this)
                            .setTitle("Gestionar alerta")
                            .setMessage(detalle.toString())

                            .setPositiveButton(
                                    "Marcar como atendida",
                                    (dialog, which) ->
                                            actualizarEstado(
                                                    key,
                                                    "ATENDIDA"
                                            )
                            )

                            .setNegativeButton(
                                    "Cerrar alerta",
                                    (dialog, which) ->
                                            actualizarEstado(
                                                    key,
                                                    "CERRADA"
                                            )
                            )

                            .setNeutralButton(
                                    "Cancelar",
                                    null
                            )

                            .show();
                })
                .addOnFailureListener(error -> {

                    Toast.makeText(
                            this,
                            "No se pudieron consultar las confirmaciones: "
                                    + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void actualizarEstado(
            String key,
            String nuevoEstado
    ) {

        ref.child(key)
                .child("status")
                .setValue(nuevoEstado)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                this,
                                "Alerta actualizada: " + nuevoEstado,
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        String error = task.getException() == null
                                ? "Error desconocido"
                                : task.getException().getMessage();

                        Toast.makeText(
                                this,
                                "No se pudo actualizar: " + error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void confirmarRecepcion(DataSnapshot item) {

        String key = item.getKey();

        if (key == null) {
            return;
        }

        String uid = FirebaseSupport.uid();

        if (uid.equals(item.child("uid").getValue(String.class))) {

            Toast.makeText(
                    this,
                    "Esta es tu propia alerta. Solo el administrador puede gestionarla.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        DatabaseReference receipt = FirebaseSupport.db()
                .child("alertReceipts")
                .child(AppConfig.COMMUNITY_ID)
                .child(key)
                .child(uid);

        receipt.get()
                .addOnSuccessListener(existing -> {

                    if (existing.exists()) {

                        Toast.makeText(
                                this,
                                "Ya confirmaste que viste esta alerta",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    FirebaseSupport.db()
                            .child("profiles")
                            .child(uid)
                            .get()
                            .addOnSuccessListener(profile -> {

                                Map<String, Object> data = new HashMap<>();

                                data.put("uid", uid);
                                data.put(
                                        "name",
                                        profile.child("name")
                                                .getValue(String.class)
                                );
                                data.put(
                                        "housing",
                                        profile.child("housing")
                                                .getValue(String.class)
                                );
                                data.put(
                                        "seenAt",
                                        System.currentTimeMillis()
                                );

                                receipt.setValue(data)
                                        .addOnCompleteListener(task -> {

                                            Toast.makeText(
                                                    this,
                                                    task.isSuccessful()
                                                            ? "Confirmaste que viste la alerta"
                                                            : "No se pudo guardar la confirmación",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        });
                            });
                });
    }

    @Override
    protected void onDestroy() {

        if (listener != null && ref != null) {
            ref.removeEventListener(listener);
        }

        if (mqtt != null) {
            mqtt.disconnect();
        }

        super.onDestroy();
    }
}
