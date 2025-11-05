package com.example.bangbillija.util;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.function.Consumer;

public class SimpleTextWatcher implements TextWatcher {

    private final Consumer<String> consumer;

    public SimpleTextWatcher(Consumer<String> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // no-op
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // no-op
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (consumer != null) {
            consumer.accept(s == null ? "" : s.toString());
        }
    }
}
