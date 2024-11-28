package com.example.monitoracaodistanciamento;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BluetoothSensor extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private BluetoothAdapter bluetoothAdapter;
    private MediaPlayer mediaPlayer;

    // Lançador para solicitação de permissões
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (result.containsValue(false)) {
                    // Alguma permissão foi negada
                    Toast.makeText(this, "Permissões necessárias negadas", Toast.LENGTH_SHORT).show();
                } else {
                    // Todas as permissões foram concedidas
                    checkAndEnableBluetooth();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bluetooth_sensor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound);

        updateConnectionStatus();

        checkPermissions();

        Button btnEnableBluetooth = findViewById(R.id.btnConect);
        btnEnableBluetooth.setOnClickListener(v ->{
            checkPermissions();
            openBluetoothSettings();
        });

        Button btnStopAlarm = findViewById(R.id.btnStopAlarm);
        btnStopAlarm.setOnClickListener(v ->{
            stopAlarm();
            Toast.makeText(this, "Alarme parado", Toast.LENGTH_SHORT).show();
        });

        // Registrar BroadcastReceiver para monitorar desconexões
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(broadcastReceiver, filter);
    }

    // BroadcastReceiver para detectar a desconexão
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                playAlertSound();
            }
        }
    };

    private void playAlertSound() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start(); // Inicia o som de alerta
        }
    }

    public void stopAlarm() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.prepareAsync(); // Prepara novamente para tocar no futuro
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void checkPermissions() {
        updateConnectionStatus();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Permissões necessárias para Android 12 (API 31) ou superior
            String[] permissions = {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

            if (!hasPermissions(this, permissions)) {
                // Solicita permissões ao usuário
                permissionLauncher.launch(permissions);
            } else {
                checkAndEnableBluetooth();
            }
        } else {
            // Para Android versões anteriores ao 12
            String[] permissions = {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

            if (!hasPermissions(this, permissions)) {
                // Solicita permissões ao usuário
                ActivityCompat.requestPermissions(this, permissions, 100);
            } else {
                checkAndEnableBluetooth();
            }
        }
    }

    private void updateConnectionStatus() {
        TextView lblStatusConect = findViewById(R.id.lblStatusConect);
        ImageView imgStatusConect = findViewById(R.id.imgStatusConect);

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            // Caso o Bluetooth esteja desativado ou não conectado
            lblStatusConect.setText(R.string.lblStatusDesconect);
            imgStatusConect.setImageResource(R.drawable.imgbthdesconect);
        } else {
            // Se o Bluetooth estiver conectado
            lblStatusConect.setText(R.string.lblStatusConect);
            imgStatusConect.setImageResource(R.drawable.imgbthconect);
        }
    }

    private void openBluetoothSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "Conecte-se a um dispositivo.", Toast.LENGTH_SHORT).show();
    }

    private void checkAndEnableBluetooth() {
        try {
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Dispositivo não suporta Bluetooth", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
            } else {
                Toast.makeText(this, "Bluetooth já está ativado", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAndEnableBluetooth();
            } else {
                Toast.makeText(this, "Permissões necessárias negadas", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updateConnectionStatus();

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth ativado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth não foi ativado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean hasPermissions(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}