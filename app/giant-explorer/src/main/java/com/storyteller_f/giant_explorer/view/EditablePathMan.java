package com.storyteller_f.giant_explorer.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.storyteller_f.file_system.FileInstanceFactory;
import com.storyteller_f.file_system.instance.FileInstance;

public class EditablePathMan extends PathMan {
    public EditablePathMan(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditablePathMan(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public EditablePathMan(Context context) {
        super(context);
    }

    public EditablePathMan(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void drawPath(String path) {
        super.drawPath(path);
        addInput();
    }

    public void addInput() {
        final EditText editText = new EditText(getContext());
        editText.setSingleLine();
        editText.setHint("input path");
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
                String path = editText.getText().toString().trim();
                //todo 简化文件路径
                if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
                FileInstance fileInstance = FileInstanceFactory.getFileInstance(path, v.getContext());
                if (fileInstance.exists()) {
                    if (pathChangeListener != null) pathChangeListener.onSkipOnPathMan(path);
                    drawPath(path);
                } else {
                    Toast.makeText(getContext(), "输入的路径不存在", Toast.LENGTH_SHORT).show();
                }
            }
            return false;
        });

        getLinearLayout().addView(editText);
    }
}
