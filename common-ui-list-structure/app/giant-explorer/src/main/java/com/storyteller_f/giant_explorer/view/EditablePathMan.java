package com.storyteller_f.giant_explorer.view;

import static com.storyteller_f.giant_explorer.control.FileListKt.getFileInstance;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.storyteller_f.file_system.instance.FileInstance;
import com.storyteller_f.multi_core.StoppableTask;

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
            FileInstance fileInstance = getFileInstance(path, v.getContext(), pathChangeListener.root(), StoppableTask.Blocking.INSTANCE);
            if (fileInstance.exists()) {
                if (pathChangeListener != null) pathChangeListener.onSkipOnPathMan(path);
                drawPath(path);
            }
        } else {
            Toast.makeText(getContext(), "输入的路径不存在", Toast.LENGTH_SHORT).show();
        }
    }
}
