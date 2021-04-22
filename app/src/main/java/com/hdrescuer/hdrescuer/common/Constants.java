package com.hdrescuer.hdrescuer.common;

/**
 * Clase de variables y métodos comunes entre las distintas clases. Cosas que puedan ser usadas por más de una instancia
 * @author Domingo Lopez
 */
public class Constants {

    public static final String API_BASE_URL = "http://10.0.2.2:8080/";
    public static final String DATA_RESCUER_API_GATEWAY= "http://192.168.1.135:8080/";


    /*API FINAL
    public static final String API_BASE_URL = "http://localhost:8080/api/";
    */

    //PREFERENCES
    public static final String PREF_TOKEN = "PREF_TOKEN";
    public static final String PREF_USERNAME = "PREF_CREATED";
    public static final String PREF_EMAIL = "PREF_EMAIL";
    public static final String PREF_CREATED = "PREF_CREATED";


    //API E4BAND
    public static final String EMPATICA_API_KEY = "b30b5a4f98984d4db37153a97fb21bde";

    //CAPABILITIES TO DETECT EACH OTHER (PHONE AND WATCH)
    //Si se cambia debe cambiarse también en /res/values/wear.xml
    public static final String CAPABILITY_WEAR_APP = "verify_remote_example_wear_app";

    //SAMPLE RATE
    public static int SAMPLE_RATE = 200;


    /**
     * Método que calcula las horas, minutos y segundos de un entero de segundos
     * @author Domingo Lopez
     * @param secs
     * @return String
     */
    public static String getHMS(int secs){

        int hours = secs / 3600;
        int minutes = (secs % 3600) / 60;
        int seconds = secs % 60;

        String chain = String.valueOf(hours)+"h : "+ String.valueOf(minutes) +"m : "+ String.valueOf(seconds)+"s";

        return chain;


    }

}
