
package com.hdrescuer.hdrescuer.retrofit.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Clase RequesLogin. Para la solicitud de login en el servidor
 * @author Domingo López
 */
public class RequestLogin {

    @SerializedName("email")
    @Expose
    private String email;
    @SerializedName("password")
    @Expose
    private String password;

    /**
     * Constructor vacío
     * @author Domingo Lopez
     * 
     */
    public RequestLogin() {
    }

    /**
     * Constructor con paráemtros
     * @author Domingo Lopez
     * @param password
     * @param email
     */
    public RequestLogin(String email, String password) {
        super();
        this.email = email;
        this.password = password;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
