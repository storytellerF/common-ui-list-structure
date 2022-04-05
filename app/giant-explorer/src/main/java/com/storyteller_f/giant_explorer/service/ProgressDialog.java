package com.storyteller_f.giant_explorer.service;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.storyteller_f.giant_explorer.R;


public class ProgressDialog {
    private static final int handler_tip = 1;
    private static final int handler_state = 2;
    private static final int handler_detail = 3;
    private static final int handler_progress = 4;
    private static final int handler_left = 5;
    private final TextView task;
    private final TextView left;
    private Handler dialogHandler;
    private AlertDialog alertDialog;
    private ProgressBar progressBar;
    private TextView tip;
    private Listener listener;
    private TextView state;
    private TextView detail;
    private boolean state_task = false;

    public ProgressDialog(final Context context, String title, String message) {
        SpannableString spannableString = new SpannableString("");

        LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(context)
                .inflate(R.layout.layout_dialog_progress_message, null);
        linearLayout.setKeepScreenOn(true);
        progressBar = linearLayout.findViewById(R.id.progressBar);
        tip = linearLayout.findViewById(R.id.textView_tip);
        task = linearLayout.findViewById(R.id.textView_task);
        left = linearLayout.findViewById(R.id.textView_left);
        detail = linearLayout.findViewById(R.id.textView_detail);
        detail.setText(spannableString);
        state = linearLayout.findViewById(R.id.textView_state);
        dialogHandler = new DialogHandler(Looper.myLooper());
        alertDialog = new AlertDialog.Builder(context)
                .setView(linearLayout)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton("取消", null)
                .create();
        alertDialog.setOnShowListener(dialog -> alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!state_task) {
                    if (listener != null) {
                        new AlertDialog.Builder(context).setTitle("严重警告").setMessage("任务还没有完成，真的取消吗？已完成的操作不会逆转！！！！")
                                .setPositiveButton("确认", (dialog1, which) -> listener.onCancel()).show();

                    }
                } else {
                    dismiss();
                }
            }
        }));


    }

    /**
     * 通知进度对话框任务已经完成
     *
     * @param ui 是否在ui线程中修改
     */
    public void taskComplete(boolean ui) {
        state_task = true;
        if (ui) {
            setText();
        } else {
            alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).post(new Runnable() {
                @Override
                public void run() {
                    setText();
                }
            });
        }
    }

    /**
     * 修改按钮文字为“关闭”
     */
    private void setText() {
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setText("关闭");
    }

    /**
     * 对外的显示对话框的函数
     */
    public void show() {
        alertDialog.show();
    }

    /**
     * 对外的隐藏对话框的函数
     */
    private void dismiss() {
        alertDialog.dismiss();
    }

    public void setProgress(int progress, boolean ui) {
        if (ui) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressBar.setProgress(progress, true);
            } else progressBar.setProgress(progress);
        } else {
            Message message = new Message();
            Bundle bundle = new Bundle();
            bundle.putInt("data", progress);
            message.setData(bundle);
            message.what = handler_progress;
            message.obj = progressBar;
            dialogHandler.sendMessage(message);
        }
    }

    public void setLeft(String left) {
        Bundle bundle = new Bundle();
        bundle.putString("data", left);
        Message message = new Message();
        message.what = handler_left;
        message.setData(bundle);
        message.obj = this.left;
        dialogHandler.sendMessage(message);
    }

    public boolean isState_task() {
        return state_task;
    }

    public void setTipContent(String tipContent, boolean ui) {
        if (ui) {
            tip.setText(tipContent);
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("data", tipContent);
            Message message = new Message();
            message.what = handler_tip;
            message.setData(bundle);
            message.obj = tip;
            dialogHandler.sendMessage(message);
        }
    }

    public void setStateContent(String stateContent, boolean ui) {
        if (ui) {
            state.setText(stateContent);
        } else {
            Message message = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("data", stateContent);
            message.setData(bundle);
            message.what = handler_state;
            message.obj = state;
            dialogHandler.sendMessage(message);
        }
    }

    /**
     * @param detailContent 内容
     * @param ui            true,允许在ui线程上
     * @param color         颜色
     */
    public void appendDetail(String detailContent, boolean ui, int color) {
        if (ui) {
            int length = detail.getText().length();
            detail.append(detailContent);
            detail.append("\n");
            int length_last = detail.getText().length();
            if (color != 0) {
                SpannableStringBuilder spannableString = (SpannableStringBuilder) detail.getEditableText();
                spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor (detail.getContext(),color)), length, length_last, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else {
            Message message = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("data", detailContent);
            message.setData(bundle);
            message.what = handler_detail;
            message.obj = detail;
            message.arg1 = color;
            dialogHandler.sendMessage(message);
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setMessage(String s) {
        alertDialog.setMessage(s);
    }

    public void setTask(String format) {
        task.setText(format);
    }

    public int getProgress() {
        return progressBar.getProgress();
    }

    public interface Listener {
        void onCancel();
    }

    static class DialogHandler extends Handler {
        public DialogHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == handler_progress) {
                ProgressBar progressBar = (ProgressBar) msg.obj;
                Bundle bundle = msg.getData();
                int progress = bundle.getInt("data");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressBar.setProgress(progress, true);
                } else progressBar.setProgress(progress);
            } else if (msg.what == handler_detail) {
                TextView detail = (TextView) msg.obj;
                Bundle bundle = msg.getData();
                int length = detail.getText().length();
                String data = bundle.getString("data");
                int color = msg.arg1;
                if (data != null) {
                    detail.append(data);
                    int length_last = length + data.length();
                    detail.append("\n");
                    if (color != 0) {
                        SpannableStringBuilder spannableString = (SpannableStringBuilder) detail.getEditableText();
                        spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor( detail.getContext(),color)), length, length_last, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            } else {
                TextView detail = (TextView) msg.obj;
                Bundle bundle = msg.getData();
                detail.setText(bundle.getString("data"));
            }
            super.handleMessage(msg);
        }
    }
}
