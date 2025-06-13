package com.kudig.kwitansidigital.ui.privacy_policy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.kudig.kwitansidigital.databinding.FragmentAboutBinding;
import com.kudig.kwitansidigital.databinding.FragmentPrivacyPolicyBinding;

public class Privacy_PolicyFragment extends Fragment {

    private FragmentPrivacyPolicyBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Privacy_PolicyViewModel Privacy_PolicyViewModel =
                new ViewModelProvider(this).get(Privacy_PolicyViewModel.class);

        binding = FragmentPrivacyPolicyBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textAbout;
        Privacy_PolicyViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}