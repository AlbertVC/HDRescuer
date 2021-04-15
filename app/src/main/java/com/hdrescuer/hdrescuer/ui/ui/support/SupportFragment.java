package com.hdrescuer.hdrescuer.ui.ui.support;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.hdrescuer.hdrescuer.R;

public class SupportFragment extends Fragment implements View.OnClickListener {

    TextView Freepik;
    TextView Flaticons;
    TextView Github;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_support, container, false);

        findViews(root);

        return root;
    }


    void findViews(View view){
        this.Flaticons = view.findViewById(R.id.tvFlaticons);
        this.Freepik = view.findViewById(R.id.tvFreepik);
        this.Github = view.findViewById(R.id.tvgithub);

        this.Freepik.setOnClickListener(this);
        this.Flaticons.setOnClickListener(this);
        this.Github.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        Uri uri;
        Intent intent;
        switch (view.getId()){
            case R.id.tvFlaticons:
                uri = Uri.parse("https://www.flaticon.es/");
                break;

            case R.id.tvFreepik:
                uri = Uri.parse("https://www.freepik.es/");
                break;

            default:
                uri = Uri.parse("https://github.com/DomingoLopez");
                break;

        }
        intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }






}