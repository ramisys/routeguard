package com.example.routeguard.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.routeguard.R;
import com.example.routeguard.data.local.RouteGuardDatabase;

import java.util.concurrent.Executors;

public class ErrorLogFragment extends Fragment {

    private ErrorLogAdapter adapter;
    private RouteGuardDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_error_log, container, false);

        database = RouteGuardDatabase.getInstance(requireContext());
        
        RecyclerView rv = view.findViewById(R.id.rvErrorLogs);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ErrorLogAdapter();
        rv.setAdapter(adapter);

        database.errorDao().getAllErrors().observe(getViewLifecycleOwner(), logs -> {
            if (logs != null) {
                adapter.setLogs(logs);
            }
        });

        view.findViewById(R.id.btnClearLogs).setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                database.errorDao().clearAll();
            });
        });

        return view;
    }
}