package com.storyteller_f.giant_explorer.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.storyteller_f.multi_core.StoppableTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Decode {
    public static int decodeFourByteNumber(byte[] bytes, int index) {
        return (bytes[index] & 255) * 16777216 + (bytes[index + 1] & 255) * 65536 + (bytes[index + 2] & 255) * 256 + (bytes[index + 3] & 255);
    }

    public static int decodeSignedByte(int num) {
        num &= 255;
        if (num < 128) {
            return num + 127;
        } else {
            String str = Integer.toString(num, 2);
            char[] array = new char[8];

            for (int j = 0; j < str.length(); ++j) {
                array[j] = str.charAt(7 - j);
            }

            return Integer.parseInt(new String(array), 2);
        }
    }

    private static String getInteger(String input) {
        return input.substring(1, input.indexOf(101));
    }

    private static String getString(String input) {
        int index = input.indexOf(58) + 1;
        int length = Integer.parseInt(input.substring(0, index - 1));
        return input.substring(index, index + length);
    }

    private static List<Object> getList(String string, StoppableTask worker) {
        List<Object> list = new ArrayList<Object>() {
            private static final long serialVersionUID = 1093559083643494037L;

            @NonNull
            public String toString() {
                StringBuilder buffer = new StringBuilder();
                buffer.append('l');

                for (int i = 0; i < this.size(); ++i) {
                    if (worker.needStop()) return "stop";
                    Object obj = this.get(i);
                    if (obj instanceof String) {
                        String string = (String) obj;
                        buffer.append(string.length()).append(':').append(string);
                    } else if (obj instanceof Long) {
                        buffer.append('i').append(obj).append('e');
                    } else if (obj instanceof List || obj instanceof BEncodedDictionary) {
                        buffer.append(obj);
                    }
                }

                buffer.append('e');
                return buffer.toString();
            }
        };

        for (char ch = string.charAt(0); ch != 'e'; ch = string.charAt(0)) {
            if (worker.needStop()) return null;
            String value;
            if (Character.isDigit(ch)) {
                if (ch == '0') {
                    list.add("");
                    string = string.substring(2);
                } else {
                    value = getString(string);
                    string = string.substring(string.indexOf(value, string.indexOf(58)) + value.length());
                    list.add(value);
                }
            } else if (ch == 'd') {
                BEncodedDictionary dictionary = parse(string.substring(1), worker);
                if (dictionary == null) return null;
                list.add(dictionary);
                value = dictionary.toString();
                string = string.substring(value.length());
            } else if (ch == 'l') {
                List<Object> aList = getList(string.substring(1), worker);
                if (aList == null) return null;
                list.add(aList);
                String s = aList.toString();
                if (s.equals("stop")) return null;
                string = string.substring(s.length());
            } else if (ch == 'i') {
                value = getInteger(string);
                string = string.substring(value.length() + 2);
                list.add(Long.valueOf(value));
            }
        }

        return list;
    }

    @Nullable
    private static String parse(String string, BEncodedDictionary dictionary, StoppableTask worker) {
        String key = getString(string);
        string = string.substring(string.indexOf(key) + key.length());
        char ch = string.charAt(0);
        String value;
        if (Character.isDigit(ch)) {
            if (ch == '0') {
                dictionary.put(key, "");
                string = string.substring(2);
            } else {
                value = getString(string);
                string = string.substring(string.indexOf(value, string.indexOf(58)) + value.length());
                dictionary.put(key, value);
            }
        } else if (ch == 'd') {
            BEncodedDictionary aDictionary = parse(string.substring(1), worker);
            if (aDictionary == null) return null;
            dictionary.put(key, aDictionary);
            string = string.substring(aDictionary.toString().length());
        } else if (ch == 'l') {
            List<Object> list = getList(string.substring(1), worker);
            if (list == null) return null;
            dictionary.put(key, list);
            string = string.substring(list.toString().length());
        } else if (ch == 'i') {
            value = getInteger(string);
            string = string.substring(value.length() + 2);
            dictionary.put(key, Long.valueOf(value));
        }

        return string;
    }

    @Nullable
    private static BEncodedDictionary parse(String string, StoppableTask worker) {
        if (string == null) return null;
        BEncodedDictionary dictionary = new BEncodedDictionary();
        while (string.charAt(0) != 'e') {
            if (worker.needStop()) return null;
            string = parse(string, dictionary, worker);
            if (string == null) return null;
        }

        return dictionary;
    }

    public static BEncodedDictionary bDecode(String string, StoppableTask worker) {
        if (string.charAt(0) != 'd') {
            throw new IllegalArgumentException("The string must begin with a dictionary");
        } else {
            return parse(string.substring(1), worker);
        }
    }

    public static BEncodedDictionary bDecode(InputStream inputStream, StoppableTask worker) throws IOException {
        String read = read(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1)), worker);
        if (read == null) return null;
        return bDecode(read, worker);
    }

    private static String read(Reader reader, StoppableTask worker) throws IOException {
        StringBuilder buffer = new StringBuilder();
        int ch = reader.read();
        while (ch != -1) {
            if (worker.needStop()) return null;
            buffer.append((char) ch);
            ch = reader.read();
        }
        reader.close();
        return buffer.toString();
    }
}
