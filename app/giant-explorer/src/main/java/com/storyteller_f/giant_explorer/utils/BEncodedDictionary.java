package com.storyteller_f.giant_explorer.utils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BEncodedDictionary {
    private final ArrayList<Entry> list = new ArrayList<>();

    BEncodedDictionary() {
    }

    public void put(Object key, Object value) {
        this.list.add(new Entry(key, value));
    }

    public Object get(Object key) {
        for (int i = 0; i < this.list.size(); ++i) {
            Entry entry = this.list.get(i);
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }

        return null;
    }

    @NonNull
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append('d');

        for (int i = 0; i < this.list.size(); ++i) {
            Entry entry = this.list.get(i);
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            buffer.append(key.length()).append(':').append(key);
            if (value instanceof String) {
                String string = (String) value;
                buffer.append(string.length()).append(':').append(string);
            } else if (value instanceof Long) {
                buffer.append('i').append(value).append('e');
            } else if (value instanceof List || value instanceof BEncodedDictionary) {
                buffer.append(value);
            }
        }

        buffer.append('e');
        return buffer.toString();

    }

    public Iterator<Entry> entryIterator() {
        return this.list.iterator();
    }

    public boolean containsKey(Object key) {
        for (int i = 0; i < this.list.size(); ++i) {
            Entry entry = this.list.get(i);
            if (entry.getKey().equals(key)) {
                return true;
            }
        }

        return false;
    }

    public boolean containsValue(Object value) {
        for (int i = 0; i < this.list.size(); ++i) {
            Entry entry = this.list.get(i);
            if (entry.getValue().equals(value)) {
                return true;
            }
        }

        return false;
    }

    private static class Entry implements java.util.Map.Entry<Object, Object> {
        private final Object key;
        private Object value;

        private Entry(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        public Object getKey() {
            return this.key;
        }

        public Object getValue() {
            return this.value;
        }

        public Object setValue(Object value) {
            Object tempValue = this.value;
            this.value = value;
            return tempValue;
        }
    }
}

