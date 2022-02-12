package com.storyteller_f.ui_list.event;

import static androidx.fragment.app.FragmentManager.findFragment;

import android.view.View;

import androidx.fragment.app.Fragment;

import com.storyteller_f.ui_list.R;

import kotlin.jvm.functions.Function1;

public class ViewJava {
    public static <T> T findActionReceiverOrNull(View view, Class<T> actionReceive) {

        Fragment fragment;
        try {
            fragment = findFragment(view);
        } catch (Exception var7) {
            fragment = null;
        }

        while (fragment != null) {
            if (actionReceive.isInstance(fragment)) {
                return ((T) fragment);
            }
            fragment = fragment.getParentFragment();
        }
        return null;
    }

    public static <T, R> void findActionReceiverOrNull(View view, Class<T> actionReceive, Function1<T, R> function1) {

        Fragment fragment;
        try {
            fragment = findFragment(view);
        } catch (Exception var7) {
            fragment = null;
        }

        while (fragment != null) {
            if (actionReceive.isInstance(fragment)) {
                function1.invoke((T) fragment);
                return;
            }
            fragment = fragment.getParentFragment();
        }
    }

    public static <T, R> void let(T o, Function1<T, R> function) {
        if (o != null) {
            function.invoke(o);
        }
    }

    public static<T, R> void doWhenIs(Object o, Class<T> tClass, Function1<T, R> function) {
        if (tClass.isInstance(o)) {
            function.invoke((T) o);
        }
    }
}
