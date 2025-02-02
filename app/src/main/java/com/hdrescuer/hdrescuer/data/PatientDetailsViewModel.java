package com.hdrescuer.hdrescuer.data;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.hdrescuer.hdrescuer.db.entity.UserEntity;
import com.hdrescuer.hdrescuer.retrofit.response.UserDetails;

/**
 * ViewModel para los detalles del usuario
 * @author Domingo Lopez
 */
public class PatientDetailsViewModel extends AndroidViewModel implements ViewModelProvider.Factory {


    public PatientDetailsRepository patientDetailsRepository;
    int user_id;

    /**
     * Constructor Factory del ViewModel
     * @author Domingo Lopez
     * @param application
     * @param id
     */
    public PatientDetailsViewModel(@NonNull Application application, int id) {
        super(application);
        this.user_id = id;
        this.patientDetailsRepository = new PatientDetailsRepository(id);

    }

    /**
     * Obtiene los datos del usuario desde el respositorio
     * @author Domingo Lopez
     * @return MutableLiveData
     */
    public MutableLiveData<UserDetails> getPatient() {

        return this.patientDetailsRepository.getPatientDetails();
    }

    /**
     * Método que llama al repositorio para actualizar los datos
     * @author Domingo Lopez
     * @param userEntity
     */
    public void getPatientDetails(UserEntity userEntity){
        this.patientDetailsRepository.updatePatient(userEntity);
    }

    /**
     * Método que refresca los datos del usuario en caso de que haya actualizaciones en los detalles
     * @author Domingo Lopez
     */
    public void refreshPatientDetails(){
        this.patientDetailsRepository.refreshPatientDetails(this.user_id);
    }



    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return null;
    }

}