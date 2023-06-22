package com.storyteller_f.giant_explorer.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

public class EditablePathMan extends PathMan {
    public EditablePathMan(Context context) {
        super(context);
    }

    public EditablePathMan(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditablePathMan(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public EditablePathMan(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void drawPath(String path) {
        super.drawPath(path);
        addInput(path);
    }

    public void addInput(String path) {
        final EditText editText = new EditText(getContext());
        editText.setSingleLine();
        editText.setTextSize(20);
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                editText.setText(getPath(getLinearLayout().getChildCount() - 2));
                int width = editText.getWidth();
                editText.setTag(width);
                editText.setWidth(getWidth());
            } else {
                editText.setText("");
                editText.setWidth((Integer) editText.getTag());
            }
            scroll();
        });
        editText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                redirect(v, editText.getText().toString().trim());
            }
            return false;
        });

        getLinearLayout().addView(editText);
    }

    private void redirect(View v, String input) {
        String path = input;
        //如果用户输入了错误的路径，进行裁切
        int length = path.length();
        if (path.endsWith("/") && length != 1) path = path.substring(0, length - 1);
        if (pathChangeListener != null) {
            pathChangeListener.onSkipOnPathMan(path);
            drawPath(path);
        }
    }
}
