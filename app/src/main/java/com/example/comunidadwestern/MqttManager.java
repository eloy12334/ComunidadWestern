
package com.example.comunidadwestern;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MqttManager {


    private static final String[] FALLBACK_URIS = {
            "ssl://h0bc12ce.ala.us-east-1.emqxsl.com:8883",
            "wss://h0bc12ce.ala.us-east-1.emqxsl.com:8084/mqtt"
    };

    public interface Listener {
        void onStatus(String status);
        void onMessage(String payload);
    }

    public interface PublishCallback {
        void onSuccess();
        void onFailure(String error);
    }

    private final Context context;
    private final Listener listener;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private MqttAndroidClient client;
    private boolean connecting = false;

    public MqttManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    // Comprueba que las credenciales estén configuradas.
    public boolean isConfigured() {
        return !BuildConfig.MQTT_URI.trim().isEmpty()
                && !BuildConfig.MQTT_USERNAME.trim().isEmpty()
                && !BuildConfig.MQTT_PASSWORD.trim().isEmpty();
    }

    // Comprueba el estado de la conexión.
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    // Establece la conexión con el broker.
    public void connect() {

        if (!isConfigured()) {
            notifyStatus(
                    "MQTT sin configurar. Revisa local.properties."
            );
            return;
        }

        if (client != null && client.isConnected()) {
            notifyStatus("MQTT conectado.");
            return;
        }

        if (connecting) {
            return;
        }

        try {

            connecting = true;

            String clientId = "western-" + UUID.randomUUID();

            client = new MqttAndroidClient(
                    context,
                    BuildConfig.MQTT_URI,
                    clientId,
                    Ack.AUTO_ACK
            );

            // Configuración de los eventos MQTT.
            client.setCallback(new MqttCallbackExtended() {

                @Override
                public void connectComplete(
                        boolean reconnect,
                        String serverURI
                ) {

                    connecting = false;

                    notifyStatus(
                            reconnect
                                    ? "MQTT reconectado."
                                    : "MQTT conectado."
                    );

                    // Suscribirse después de conectar.
                    suscribir();
                }

                @Override
                public void connectionLost(Throwable cause) {

                    connecting = false;

                    String detalle = cause == null
                            ? "Sin detalles"
                            : cause.getMessage();

                    notifyStatus(
                            "MQTT desconectado. " + detalle
                    );
                }

                @Override
                public void messageArrived(
                        String topic,
                        MqttMessage message
                ) {

                    // Procesar solamente las alertas de Western.
                    if (AppConfig.MQTT_TOPIC.equals(topic)) {

                        String payload = new String(
                                message.getPayload(),
                                StandardCharsets.UTF_8
                        );

                        notifyMessage(payload);
                    }
                }

                @Override
                public void deliveryComplete(
                        IMqttDeliveryToken token
                ) {
                    // El broker confirmó la entrega.
                }
            });

            // Opciones de conexión.
            MqttConnectOptions options =
                    new MqttConnectOptions();

            options.setServerURIs(FALLBACK_URIS);

            options.setUserName(
                    BuildConfig.MQTT_USERNAME
            );

            options.setPassword(
                    BuildConfig.MQTT_PASSWORD.toCharArray()
            );

            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);

            client.connect(
                    options,
                    null,
                    new IMqttActionListener() {

                        @Override
                        public void onSuccess(IMqttToken token) {

                            connecting = false;

                            notifyStatus(
                                    "Conexión MQTT establecida."
                            );
                        }

                        @Override
                        public void onFailure(
                                IMqttToken token,
                                Throwable error
                        ) {

                            connecting = false;

                            notifyStatus(
                                    "Error de conexión MQTT: "
                                            + getError(error)
                            );
                        }
                    }
            );

        } catch (Exception error) {

            connecting = false;

            notifyStatus(
                    "Error al iniciar MQTT: " + getError(error)
            );
        }
    }

    // Suscripción al topic raíz
    private void suscribir() {

        try {

            if (client == null || !client.isConnected()) {
                return;
            }

            client.subscribe(
                    AppConfig.MQTT_SUBSCRIPTION,
                    1,
                    null,
                    new IMqttActionListener() {

                        @Override
                        public void onSuccess(IMqttToken token) {

                            notifyStatus(
                                    "MQTT conectado. Suscripción activa (QoS 1)."
                            );
                        }

                        @Override
                        public void onFailure(
                                IMqttToken token,
                                Throwable error
                        ) {

                            notifyStatus(
                                    "Error de suscripción: "
                                            + getError(error)
                            );
                        }
                    }
            );

        } catch (Exception error) {

            notifyStatus(
                    "Error al suscribirse: " + getError(error)
            );
        }
    }

    // Publicar una alerta en el topic de nuestra comunidad.
    public void publish(
            String payload,
            PublishCallback callback
    ) {

        if (client == null || !client.isConnected()) {

            notifyStatus("No hay conexión MQTT.");

            callback.onFailure(
                    "No hay conexión MQTT."
            );

            return;
        }

        if (payload == null || payload.trim().isEmpty()) {

            callback.onFailure(
                    "El mensaje de alerta está vacío."
            );

            return;
        }

        try {

            MqttMessage message = new MqttMessage(
                    payload.getBytes(StandardCharsets.UTF_8)
            );

            // QoS 1.
            message.setQos(1);

            // No conservar alertas antiguas.
            message.setRetained(false);

            client.publish(
                    AppConfig.MQTT_TOPIC,
                    message,
                    null,
                    new IMqttActionListener() {

                        @Override
                        public void onSuccess(IMqttToken token) {

                            mainHandler.post(
                                    callback::onSuccess
                            );
                        }

                        @Override
                        public void onFailure(
                                IMqttToken token,
                                Throwable error
                        ) {

                            mainHandler.post(
                                    () -> callback.onFailure(
                                            getError(error)
                                    )
                            );
                        }
                    }
            );

        } catch (Exception error) {

            String detalle = getError(error);

            mainHandler.post(
                    () -> callback.onFailure(detalle)
            );
        }
    }

    // Desconectar el cliente MQTT.
    public void disconnect() {

        connecting = false;

        try {

            if (client != null && client.isConnected()) {
                client.disconnect();
            }

        } catch (Exception error) {

            notifyStatus(
                    "Error al desconectar MQTT: " + getError(error)
            );
        }
    }

    // Enviar estados al hilo principal de Android.
    private void notifyStatus(String status) {

        mainHandler.post(
                () -> listener.onStatus(status)
        );
    }

    // Enviar mensajes recibidos al hilo principal.
    private void notifyMessage(String payload) {

        mainHandler.post(
                () -> listener.onMessage(payload)
        );
    }

    // Obtener una descripción del error.
    private String getError(Throwable error) {

        if (error == null) {
            return "Error desconocido.";
        }

        String message = error.getMessage();

        return message == null || message.trim().isEmpty()
                ? error.getClass().getSimpleName()
                : message;
    }
}
