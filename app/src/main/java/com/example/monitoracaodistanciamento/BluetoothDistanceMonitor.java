package com.example.monitoracaodistanciamento;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothDistanceMonitor {
    private static final String TAG = "BluetoothDistanceMonitor";

    // UUID padrão para comunicação Bluetooth (SPP)
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice connectedDevice;
    private BluetoothSocket bluetoothSocket;

    private boolean isMonitoring;
    private Handler handler;

    private OnDistanceChangedListener distanceChangedListener;
    private OnDisconnectedListener disconnectedListener;

    // Intervalo de atualização (ms)
    private static final int MONITOR_INTERVAL = 1000;

    public BluetoothDistanceMonitor() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        handler = new Handler();
    }

    /**
     * Inicia a conexão com o dispositivo Bluetooth
     */
    public boolean connectToDevice(BluetoothDevice device) {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            connectedDevice = device;
            Log.d(TAG, "Conectado ao dispositivo: " + device.getName());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Erro ao conectar ao dispositivo: " + e.getMessage());
            closeConnection();
            return false;
        }
    }

    /**
     * Inicia o monitoramento de distância baseado no RSSI
     */
    public void startMonitoring() {
        if (connectedDevice == null || bluetoothSocket == null) {
            Log.e(TAG, "Nenhum dispositivo conectado para monitorar.");
            return;
        }

        isMonitoring = true;
        handler.post(monitorRunnable);
    }

    /**
     * Para o monitoramento de distância
     */
    public void stopMonitoring() {
        isMonitoring = false;
        handler.removeCallbacks(monitorRunnable);
    }

    /**
     * Runnable para monitorar o RSSI e calcular a distância
     */
    private final Runnable monitorRunnable = new Runnable() {
        @Override
        public void run() {
            if (isMonitoring && bluetoothSocket != null) {
                try {
                    int rssi = getRSSI();
                    double distance = calculateDistance(rssi);

                    if (distanceChangedListener != null) {
                        distanceChangedListener.onDistanceChanged(distance);
                    }

                    // Verificar se o dispositivo está fora de alcance
                    if (distance > 10) { // Exemplo: 10 metros como limite
                        if (disconnectedListener != null) {
                            disconnectedListener.onDisconnected();
                        }
                        stopMonitoring();
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Erro ao obter RSSI: " + e.getMessage());
                    stopMonitoring();
                }

                handler.postDelayed(this, MONITOR_INTERVAL);
            }
        }
    };

    /**
     * Obtém o RSSI do dispositivo Bluetooth
     */
    private int getRSSI() throws IOException {
        // Simulação de RSSI, pois Bluetooth clássico não expõe diretamente o RSSI em sockets
        // Utilize `device.getBondState()` e interfaces do `BluetoothDevice` quando aplicável
        return -60; // Valor simulado de RSSI em dBm
    }

    /**
     * Calcula a distância com base no RSSI
     * Fórmula padrão: d = 10 ^ ((TxPower - RSSI) / (10 * N))
     */
    private double calculateDistance(int rssi) {
        int txPower = -59; // Valor de TxPower estimado (dBm)
        double n = 2.0; // Constante ambiental (2 para espaços abertos, 3-4 para interiores)
        return Math.pow(10.0, (txPower - rssi) / (10.0 * n));
    }

    /**
     * Fecha a conexão Bluetooth
     */
    public void closeConnection() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
            connectedDevice = null;
        } catch (IOException e) {
            Log.e(TAG, "Erro ao fechar a conexão: " + e.getMessage());
        }
    }

    /**
     * Define o listener para mudanças de distância
     */
    public void setOnDistanceChangedListener(OnDistanceChangedListener listener) {
        this.distanceChangedListener = listener;
    }

    /**
     * Define o listener para desconexões
     */
    public void setOnDisconnectedListener(OnDisconnectedListener listener) {
        this.disconnectedListener = listener;
    }

    /**
     * Interface para mudanças de distância
     */
    public interface OnDistanceChangedListener {
        void onDistanceChanged(double distance);
    }

    /**
     * Interface para desconexões
     */
    public interface OnDisconnectedListener {
        void onDisconnected();
    }
}
