package pl.crimeandlaw.network;

import com.google.gson.*;

/** Reject collections and coercions before Gson can recursively unwrap an input field. */
public final class ActionFields {
    private ActionFields(){}
    public static String text(JsonObject object,String key,String fallback){
        JsonElement value=object.get(key);if(value==null||value.isJsonNull())return fallback;
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())throw invalid();
        return value.getAsString();
    }
    public static int integer(JsonObject object,String key,int fallback){
        JsonElement value=object.get(key);if(value==null)return fallback;
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw invalid();
        try{return value.getAsBigDecimal().intValueExact();}
        catch(ArithmeticException|NumberFormatException error){throw invalid();}
    }
    public static boolean flag(JsonObject object,String key,boolean fallback){
        JsonElement value=object.get(key);if(value==null)return fallback;
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean())throw invalid();
        return value.getAsBoolean();
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Nieprawidłowy typ danych żądania.");}
}
