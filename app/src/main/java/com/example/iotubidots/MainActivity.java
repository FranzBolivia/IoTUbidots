package com.example.iotubidots;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static android.content.ContentValues.TAG;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {

    private final String token = "A1E-CZHbvOOx05ro4IKBdgdRkVPdFnBvdw";
    private final String idIluminacion = "5b32b34dc03f972c655ee056";
    private final String idBoton = "5b32b360c03f972cc8cf2d70";
    private final String PIN_BUTTON = "BCM23";
    private Gpio mButtonGpio;
    private Double buttonstatus = 0.0;
    private Handler handler = new Handler();
    private Runnable runnable = new UpdateRunner();

    //MQTT
    private static final String TAG = "Things";
    private static final String topic = "<villalpando.franz>/test";
    private static final String topic2 = "<villalpando.franz>/boton";
    private static final String hello = "Hello world!";
    private static final String hello2 = "CLICK!";
    private static final int qos = 1;
    private static final String broker = "tcp://iot.eclipse.org:1883";
    private static final String clientId = "Test134567325";

    //MQTT LED



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PeripheralManager service = PeripheralManager.getInstance();
        try {
            mButtonGpio = service.openGpio(PIN_BUTTON);
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            mButtonGpio.setActiveType(Gpio.ACTIVE_LOW);
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
            mButtonGpio.registerGpioCallback(mCallback);
        } catch (IOException e) {
            Log.e(TAG, "Error en PeripheralIO API", e);
        }
        handler.post(runnable);
//MQQTT
        try {
            MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());
            Log.i(TAG, "Conectando al broker " + broker);
            client.connect();
            Log.i(TAG, "Conectado");
            Log.i(TAG, "Publicando mensaje: " + hello);
            MqttMessage message = new MqttMessage(hello.getBytes());
            message.setQos(qos);
            client.publish(topic, message);
            Log.i(TAG, "Mensaje publicado");
            client.disconnect();
            Log.i(TAG, "Desconectado");
        } catch (MqttException e) {
            Log.e(TAG, "Error en MQTT.", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler = null;
        runnable = null;
        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mCallback);
            try {
                mButtonGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error en PeripheralIO API", e);
            }
        }
    }

    // Callback para envío asíncrono de pulsación de botón
    private GpioCallback mCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            Log.i(TAG, "Botón pulsado!");
            if (buttonstatus == 0.0) buttonstatus = 1.0;
            else buttonstatus = 0.0;
            final Data boton = new Data();
            boton.setVariable(idBoton);
            boton.setValue(buttonstatus);
            ArrayList<Data> message2 = new ArrayList<Data>() {{
                add(boton);
            }};
            UbiClient.getClient().sendData(message2, token);

            try {
                MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());
                Log.i(TAG, "Conectando al broker " + broker);
                client.connect();
                Log.i(TAG, "Conectado");
                Log.i(TAG, "Publicando mensaje: " + hello2);
                MqttMessage message = new MqttMessage(hello2.getBytes());
                message.setQos(qos);
                client.publish(topic2, message);
                Log.i(TAG, "Mensaje publicado");
                client.disconnect();
                Log.i(TAG, "Desconectado");
            } catch (MqttException e) {
                Log.e(TAG, "Error en MQTT.", e);
            }









            return true;
// Mantenemos el callback activo
        }
    }; // Envío síncrono (5 segundos) del valor del fotorresistor

    private class UpdateRunner implements Runnable {
        @Override
        public void run() {
            readLDR();
            Log.i(TAG, "Ejecución de acción periódica");
            handler.postDelayed(this, 5000);
        }
    }

    private void readLDR() {
        Data iluminacion = new Data();
        ArrayList<Data> message = new ArrayList<Data>();
        Random rand = new Random();
        float valor = rand.nextFloat() * 5.0f;
        iluminacion.setVariable(idIluminacion);
        iluminacion.setValue((double) valor);
        message.add(iluminacion);
        UbiClient.getClient().sendData(message, token);
    }
}