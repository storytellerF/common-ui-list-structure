package com.storyteller_f.ui_list.event;

import static androidx.fragment.app.FragmentManager.findFragment;

import android.view.View;

import androidx.fragment.app.Fragment;

import kotlin.jvm.functions.Function1;

@SuppressWarnings("unused")
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
                return actionReceive.cast(fragment);
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
                function1.invoke(actionReceive.cast(fragment));
                return;
            }
            fragment = fragment.getParentFragment();
        }
    }

    public static <T, R> R let(T o, Function1<T, R> function) {
        if (o != null) {
            return function.invoke(o);
        }
        return null;
    }

    public static <T, R> R doWhenIs(Object o, Class<T> clazz, Function1<T, R> function) {
        if (clazz.isInstance(o)) {
            return function.invoke(clazz.cast(o));
        }
        return null;
    }
}
