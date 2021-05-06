package com.hdrescuer.hdrescuer.ui.ui.localsessions.services;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.util.Log;

import com.google.gson.JsonObject;
import com.hdrescuer.hdrescuer.common.MyApp;
import com.hdrescuer.hdrescuer.data.dbrepositories.E4BandRepository;
import com.hdrescuer.hdrescuer.data.dbrepositories.EHealthBoardRepository;
import com.hdrescuer.hdrescuer.data.dbrepositories.SessionsRepository;
import com.hdrescuer.hdrescuer.data.dbrepositories.TicWatchRepository;
import com.hdrescuer.hdrescuer.db.entity.EmpaticaEntity;
import com.hdrescuer.hdrescuer.db.entity.HealthBoardEntity;
import com.hdrescuer.hdrescuer.db.entity.SessionEntity;
import com.hdrescuer.hdrescuer.db.entity.TicWatchEntity;
import com.hdrescuer.hdrescuer.retrofit.AuthApiService;
import com.hdrescuer.hdrescuer.retrofit.AuthConectionClient;
import com.hdrescuer.hdrescuer.retrofit.request.Session;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Multipart;

public class UploadSessionService extends IntentService {

    private static final String ACTION_SEND = "ACTION_SEND";

    AuthApiService authApiService;
    AuthConectionClient authConectionClient;
    ResultReceiver receiver;
    int resultCode;

    //Repositorios de la BBDD
    SessionsRepository sessionsRepository;
    E4BandRepository e4BandRepository;
    TicWatchRepository ticWatchRepository;
    EHealthBoardRepository healthBoardRepository;

    SessionEntity sessionEntity;
    int id_session_local;
    String user_id;


    private String FILE_NAME_EMPATICA;
    private String FILE_NAME_TICWATCH;
    private String FILE_NAME_HEALTHBOARD;



    public UploadSessionService() {
        super("UploadSessionService");
        this.authConectionClient = AuthConectionClient.getInstance();
        this.authApiService = this.authConectionClient.getAuthApiService();

        this.sessionsRepository = new SessionsRepository(getApplication());
        this.e4BandRepository = new E4BandRepository(getApplication());
        this.ticWatchRepository = new TicWatchRepository(getApplication());
        this.healthBoardRepository = new EHealthBoardRepository(getApplication());

        this.resultCode = 9999;
    }



    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            switch (action){
                case "START_UPLOAD":
                    //Obtenemos los parámetros

                    this.user_id = intent.getStringExtra("user_id");
                    this.id_session_local = intent.getIntExtra("id_session_local",0);
                    this.receiver = intent.getParcelableExtra("receiver");
                    //Iniciamos los nombres de archivo
                    this.FILE_NAME_EMPATICA = "empatica_session_"+id_session_local+".csv";
                    this.FILE_NAME_TICWATCH = "ticwatch_session_"+id_session_local+".csv";
                    this.FILE_NAME_HEALTHBOARD = "healthboard_session_"+id_session_local+".csv";

                    //Obtenemos la sesión que nos hará falta para el resto de métodos
                    this.sessionEntity = this.sessionsRepository.getByIdSession(id_session_local);


                    //Iniciamos la descarga a csv
                    downloadToCSV();

                    //Una vez guardos los datos de la sesión. Subimos los tres ficheros/ o los que haya al servidor
                    //Primero realizamos la llamada  para que cree la sesión en el servidor

                    createSessionOnServer();

                    //Subimos los archivos
                    uploadCSVToServer();

                    break;

            }


        }
    }


    public void downloadToCSV(){

        this.resultCode = 9999;

        if(this.sessionEntity.e4band){
            createEmpaticaCSV();
        }

        if(this.sessionEntity.ticwatch){
            createTicWatchCSV();
        }

        if(this.sessionEntity.ehealthboard){
            createHealthBoardCSV();
        }


    }


    void createSessionOnServer(){

        //Llamada síncrona
        Call<String> call = authApiService.createSessionFromLocal(new Session(
                this.user_id,
                this.sessionEntity.timestamp_ini,
                this.sessionEntity.timestamp_fin,
                this.sessionEntity.total_time,
                this.sessionEntity.e4band,
                this.sessionEntity.ticwatch,
                this.sessionEntity.ehealthboard
        ));
        try{
            Response<String> response = call.execute();
            String session_id = response.body();

        }catch (Exception e ){

            //Si algo sale mal, borramos todos los archivos, para que no dejen huella
            deleteFiles();

            Bundle bundle =  new Bundle();
            bundle.putString("result", "Error al iniciar sesión");
            this.resultCode = 401;
            this.receiver.send(401, bundle);
            e.printStackTrace();
        }


    }



    void createEmpaticaCSV(){

        List<EmpaticaEntity> empaticaEntities = this.e4BandRepository.getByIdSession(id_session_local);


        //Creamos el archivo csv
        FileOutputStream fos = null;

        try {
            fos = MyApp.getContext().openFileOutput(FILE_NAME_EMPATICA, MODE_PRIVATE);


            fos.write("ID_SESSION_LOCAL,TIMESTAMP,E4_ACCX,E4_ACCY,E4_ACCZ,E4_BVP,E4_HR,E4_GSR,E4_IBI,E4_TEMP\n".getBytes());

            for(int i = 0; i< empaticaEntities.size(); i++){

                fos.write((Integer.toString(id_session_local)+",").getBytes());
                fos.write(empaticaEntities.get(i).timestamp.getBytes());
                fos.write((Integer.toString(empaticaEntities.get(i).e4_accx)+",").getBytes());
                fos.write((Integer.toString(empaticaEntities.get(i).e4_accy)+",").getBytes());
                fos.write((Integer.toString(empaticaEntities.get(i).e4_accz)+",").getBytes());
                fos.write((Float.toString(empaticaEntities.get(i).e4_bvp)+",").getBytes());
                fos.write((Integer.toString(empaticaEntities.get(i).e4_hr)+",").getBytes());
                fos.write((Float.toString(empaticaEntities.get(i).e4_gsr)+",").getBytes());
                fos.write((Float.toString(empaticaEntities.get(i).e4_ibi)+",").getBytes());
                fos.write((Float.toString(empaticaEntities.get(i).e4_temp)).getBytes());

                fos.write("\n".getBytes());

            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    void createTicWatchCSV(){

        List<TicWatchEntity>ticWatchEntities = this.ticWatchRepository.getByIdSession(id_session_local);


        //Creamos el archivo csv
        FileOutputStream fos = null;

        try {
            fos = MyApp.getContext().openFileOutput(FILE_NAME_TICWATCH, MODE_PRIVATE);


            fos.write("ID_SESSION_LOCAL,TIMESTAMP,TIC_ACCX,TIC_ACCY,TIC_ACCZ,TIC_ACCLX,TIC_ACCLY,TIC_ACCLZ,TIC_GIRX,TIC_GIRY,TIC_GIRZ,TIC_HRPPG,TIC_STEP\n".getBytes());

            for(int i = 0; i< ticWatchEntities.size(); i++){

                fos.write((Integer.toString(id_session_local)+",").getBytes());
                fos.write(ticWatchEntities.get(i).timestamp.getBytes());
                fos.write((Integer.toString(ticWatchEntities.get(i).tic_accx)+",").getBytes());
                fos.write((Integer.toString(ticWatchEntities.get(i).tic_accy)+",").getBytes());
                fos.write((Integer.toString(ticWatchEntities.get(i).tic_accz)+",").getBytes());
                fos.write((Integer.toString(ticWatchEntities.get(i).tic_acclx)+",").getBytes());
                fos.write((Integer.toString(ticWatchEntities.get(i).tic_accly)+",").getBytes());
                fos.write((Integer.toString(ticWatchEntities.get(i).tic_acclz)+",").getBytes());
                fos.write((Integer.toString(ticWatchEntities.get(i).tic_girx)+",").getBytes());
                fos.write((Integer.toString(ticWatchEntities.get(i).tic_giry)+",").getBytes());
                fos.write((Integer.toString(ticWatchEntities.get(i).tic_girz)+",").getBytes());
                fos.write((Float.toString(ticWatchEntities.get(i).tic_hrppg)+",").getBytes());
                fos.write((Integer.toString(ticWatchEntities.get(i).tic_step)).getBytes());

                fos.write("\n".getBytes());

            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


    void createHealthBoardCSV(){


        List<HealthBoardEntity>healthBoardEntities = this.healthBoardRepository.getByIdSession(id_session_local);


        //Creamos el archivo csv
        FileOutputStream fos = null;

        try {
            fos = MyApp.getContext().openFileOutput(FILE_NAME_HEALTHBOARD, MODE_PRIVATE);

            fos.write("ID_SESSION_LOCAL,TIMESTAMP,EHB_BPM,EHB_OX_BLOOD,EHB_AIR_FLOW\n".getBytes());

            for(int i = 0; i< healthBoardEntities.size(); i++){

                fos.write((Integer.toString(id_session_local)+",").getBytes());
                fos.write(healthBoardEntities.get(i).timestamp.getBytes());
                fos.write((Integer.toString(healthBoardEntities.get(i).ehb_bpm)+",").getBytes());
                fos.write((Integer.toString(healthBoardEntities.get(i).ehb_ox_blood)+",").getBytes());
                fos.write((Integer.toString(healthBoardEntities.get(i).ehb_air_flow)).getBytes());

                fos.write("\n".getBytes());

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


    void deleteFiles(){
        File f_emp = new File(FILE_NAME_EMPATICA);
        File f_tic = new File(FILE_NAME_TICWATCH);
        File f_hb = new File(FILE_NAME_HEALTHBOARD);

        f_emp.delete();
        f_tic.delete();
        f_hb.delete();

    }





    void uploadCSVToServer(){

        if(this.resultCode == 9999){

            Uri uri = null;
            try {
                uri = Uri.parse((MyApp.getContext().getFilesDir().getCanonicalPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.i("URI",uri.getPath());

            File f_tic = new File(uri.getPath()+"/"+FILE_NAME_TICWATCH);
            RequestBody filePart = RequestBody.create(MediaType.parse("text/csv"),f_tic);

            RequestBody descriptionPart = RequestBody.create(MultipartBody.FORM, "ticwatchCSV");
            MultipartBody.Part file = MultipartBody.Part.createFormData("ticwatchCSV",f_tic.getName(),filePart);

            Call<String> call = authApiService.uploadTicWatchCSV(descriptionPart,file);

            try{
                Response<String> response = call.execute();
                String result = response.body();

                Bundle bundle =  new Bundle();
                bundle.putString("result", result);
                this.receiver.send(1, bundle);

            }catch (Exception e ){

                //Si algo sale mal, borramos todos los archivos, para que no dejen huella
                deleteFiles();

                Bundle bundle =  new Bundle();
                bundle.putString("result", "Error al iniciar sesión");
                this.resultCode = 402;
                this.receiver.send(401, bundle);
                e.printStackTrace();
            }




        }


    }



    //Solo por si nos hace falta
   /* void loadCSV(){
        FileInputStream fis = null;

        try {
            fis = openFileInput(this.FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();

            String text = br.readLine();

            while(text != null){
                sb.append(text).append("\n");

            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(fis != null){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }*/





}