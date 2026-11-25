package com.example.comunidadwestern;

public final class AppConfig {

    private AppConfig() { }

    public static final String COMMUNITY_ID = "comunidad01";

    public static final String FCM_TOPIC =
            "comunidad_" + COMMUNITY_ID;

    // Topic para publicar las alertas de Western.
    public static final String MQTT_TOPIC =
            "hogar/" + COMMUNITY_ID + "/alertas";

    // Topic raíz para recibir los mensajes del laboratorio.
    public static final String MQTT_SUBSCRIPTION =
            "hogar/#";
}