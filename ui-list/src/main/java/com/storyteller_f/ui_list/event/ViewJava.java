package com.storyteller_f.ui_list.event;

import static androidx.fragment.app.FragmentManager.findFragment;

import android.view.View;

import androidx.fragment.app.Fragment;

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
}
