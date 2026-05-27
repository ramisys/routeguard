package com.example.routeguard.data.local;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class Converters {
    @TypeConverter
    public static double[] fromString(String value) {
        Type listType = new TypeToken<double[]>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromArray(double[] array) {
        return new Gson().toJson(array);
    }
}