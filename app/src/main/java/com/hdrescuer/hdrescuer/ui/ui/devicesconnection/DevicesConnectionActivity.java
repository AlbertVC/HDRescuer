package com.hdrescuer.hdrescuer.ui.ui.devicesconnection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.EmpaticaDevice;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaStatusDelegate;
import com.hdrescuer.hdrescuer.R;
import com.hdrescuer.hdrescuer.common.Constants;
import com.hdrescuer.hdrescuer.data.E4BandViewModel;
import com.hdrescuer.hdrescuer.ui.ui.devicesconnection.devicesconnectionmonitoring.DevicesMonitoringFragment;

import java.util.Calendar;
import java.util.Date;

public class DevicesConnectionActivity extends AppCompatActivity implements View.OnClickListener, EmpaStatusDelegate {

    //ViewModel
    E4BandViewModel e4BandViewModel;

    TextView tvUsernameMonitoring;
    TextView tvDateMonitoring;
    ImageView btn_back;

    //Botones de conexión
    Button btnE4BandConnect;
    Button btnWatchConnect;

    //Botón de start monitoring
    Button btnStartMonitoring;

    int user_id;
    String user_name;
    Date currentDate;

    //Atributos para la conexión con la E4BAND
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;
    private EmpaDeviceManager deviceManager = null;

    EmpaStatus E4BandStatus = EmpaStatus.DISCONNECTED;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices_connection);


        //Ocultamos el Toolbar del MainActivity para el login
        getSupportActionBar().hide();

        //Obtenemos el id del usuario
        Intent i = getIntent();
        this. user_id = i.getIntExtra("id", 0);
        this.user_name = i.getStringExtra("username");

        //Obtenemos la fecha:hora actual
        this.currentDate = Calendar.getInstance().getTime();

        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new E4BandViewModel(getApplication(),user_id);
            }
        };

        this.e4BandViewModel = new ViewModelProvider(this,factory).get(E4BandViewModel.class);

        findViews();
        events();
        loadUserData();
        initEmpaticaDeviceManager();
    }


    private void findViews() {

        this.tvUsernameMonitoring = findViewById(R.id.tvUserNameToolbarMonitoring);
        this.btn_back = findViewById(R.id.btn_back_new_monitoring);
        this.tvDateMonitoring = findViewById(R.id.tv_date_monitoring);
        this.btnStartMonitoring = findViewById(R.id.btn_start_monitoring);

        //Botones para la conexión de los dispositivos

        //E4BAND
        this.btnE4BandConnect = findViewById(R.id.btn_connect_e4);
        this.btnE4BandConnect.setOnClickListener(this);

        //WATCH. Hacerlo no clickable
        this.btnWatchConnect = findViewById(R.id.btn_connect_watch);
    }

    private void events() {
        this.btn_back.setOnClickListener(this);
        this.btnStartMonitoring.setOnClickListener(this);
    }
    private void loadUserData() {

        this.tvUsernameMonitoring.setText(this.user_name);
        this.tvDateMonitoring.setText(this.currentDate.toString());

        this.btnE4BandConnect.setBackgroundColor(this.btnE4BandConnect.getContext().getResources().getColor(R.color.e4disconnected));
        this.btnE4BandConnect.setText("Desconectado");

    }

    /**
     * Método para comprobar si los dispositivos están conectados. Si no lo están, iniciamos los servicios de conexión.
     * Una vez establecida la conexión para cada dispositivo que queramos (Al menos debe haber un dispositivo conectado)
     * al pulsar en el Botón de Start Monitoring iniciaremos el activity con tabs correspondiente y los servicios en
     * background y en distintas hebras que recibirán los datos.
     *
     * La idea es actualizar un Viewmodel con los datos y en cada cambio de los datos hacer la petición http correspondiente
     * para mandarlos, estableciendo un sistema de etiquetas para diferenciar los distintos dispositivos.
     */


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_ACCESS_COARSE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted, yay!
                    initEmpaticaDeviceManager();
                } else {
                    // Permission denied, boo!
                    final boolean needRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                    new AlertDialog.Builder(this)
                            .setTitle("Permisos requeridos")
                            .setMessage("Sin estos permisos no podemos localizar los dispositivos bluetooth, Permite el acceso a estos permisos para utilizar la aplicación")
                            .setPositiveButton("Volver a intentarlo", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // try again
                                    if (needRationale) {
                                        // the "never ask again" flash is not set, try again with permission request
                                        initEmpaticaDeviceManager();
                                    } else {
                                        // the "never ask again" flag is set so the permission requests is disabled, try open app settings to enable the permission
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivity(intent);
                                    }
                                }
                            })
                            .setNegativeButton("Salir ", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // without permission exit is the only way
                                    finish();
                                }
                            })
                            .show();
                }
                break;
        }
    }


    private void initEmpaticaDeviceManager() {
        // Android 6 (API level 23) now require ACCESS_COARSE_LOCATION permission to use BLE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);
        } else {
            if (TextUtils.isEmpty(Constants.EMPATICA_API_KEY)) {
                new AlertDialog.Builder(this)
                        .setTitle("Warning")
                        .setMessage("Please insert your API KEY")
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // without permission exit is the only way
                                finish();
                            }
                        })
                        .show();
                return;
            }

            // Creamos el deviceManager y hacemos que el ViewModel que vamos a compartir con el fragment de monitorización obtenga los datos
            deviceManager = new EmpaDeviceManager(getApplicationContext(), this.e4BandViewModel, this);

            // Initialize the Device Manager using your API key. You need to have Internet access at this point.
            deviceManager.authenticateWithAPIKey(Constants.EMPATICA_API_KEY);

        }
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_back_new_monitoring:

                finish();

                break;

            case R.id.btn_start_monitoring:
                Log.i("ENTRO","ENTRO");
                //Iniciaríamos el fragment para la monitorización en Tabs
                DevicesMonitoringFragment fragment = new DevicesMonitoringFragment();

                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                fragmentTransaction.add(R.id.fragment_monitoring_show, fragment);
                fragmentTransaction.commit();

                break;
        }
    }


    @Override
    public void didUpdateStatus(EmpaStatus status) {
        // Update the UI
        Log.i("STATUS",status.name());

        // The device manager is ready for use
        if (status == EmpaStatus.READY) {
            Log.i("ESTADO:",status.toString());
            Log.i("STATUS",status.name());

            // Start scanning
            try {
                deviceManager.startScanning();
                // The device manager has established a connection
            } catch (Exception e) {
                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setCancelable(true)
                        .setMessage("Device manager is unable to download label file and may reject connecting to your device.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }

            //deviceManager.startScanning();


            // The device manager has established a connection
        } else if (status == EmpaStatus.CONNECTED) {
            Log.i("ESTADO:",status.toString());

            this.btnE4BandConnect.setText("Conectado");
            this.btnE4BandConnect.setBackgroundColor(this.btnE4BandConnect.getContext().getResources().getColor(R.color.e4connected));



            // The device manager disconnected from a device
        } else if (status == EmpaStatus.DISCONNECTED) {
            Log.i("ESTADO:",status.toString());
            this.btnE4BandConnect.setText("Desconectado");
            this.btnE4BandConnect.setBackgroundColor(this.btnE4BandConnect.getContext().getResources().getColor(R.color.e4disconnected));

        }
    }

    @Override
    public void didEstablishConnection() {

    }

    @Override
    public void didUpdateSensorStatus(int status, EmpaSensorType type) {

    }

    @Override
    public void didDiscoverDevice(EmpaticaDevice bluetoothDevice, String deviceLabel, int rssi, boolean allowed) {

        // Check if the discovered device can be used with your API key. If allowed is always false,
        // the device is not linked with your API key. Please check your developer area at
        // https://www.empatica.com/connect/developer.php
        if (allowed) {
            // Stop scanning. The first allowed device will do.
            deviceManager.stopScanning();
            try {
                // Connect to the device
                deviceManager.connectDevice(bluetoothDevice);
                Log.i("ENTRO","DISCOVER");
            } catch (ConnectionNotAllowedException e) {
                // This should happen only if you try to connect when allowed == false.
                Toast.makeText(DevicesConnectionActivity.this, "Sorry, you can't connect to this device", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void didFailedScanning(int errorCode) {

    }

    @Override
    public void didRequestEnableBluetooth() {
        // Request the user to enable Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    public void bluetoothStateChanged() {

    }

    @Override
    public void didUpdateOnWristStatus(int status) {

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The user chose not to enable Bluetooth
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            // You should deal with this
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



    @Override
    protected void onPause() {
        super.onPause();
        if (deviceManager != null) {
            deviceManager.disconnect();

        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deviceManager != null) {
            Log.d("E4Service", "Disconnecting");
            deviceManager.disconnect();
        }
    }




}