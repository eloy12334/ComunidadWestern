package com.example.comunidadwestern;

import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

public final class AppValidation {
    private static final Pattern EMAIL=Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private AppValidation() { }
    public static boolean isValidEmail(String email){return email!=null&&EMAIL.matcher(email.trim()).matches();}
    public static boolean isValidHousing(String housing){return housing!=null&&housing.trim().matches("[A-Za-z0-9-]{1,10}");}
    public static boolean isValidRegistration(String name,String housing,String email,String password,String confirm){
        return name!=null&&!name.trim().isEmpty()&&isValidHousing(housing)&&isValidEmail(email)&&password!=null&&password.length()>=6&&password.equals(confirm);
    }
    public static boolean isValidSale(String title,String description,String price){
        if(title==null||title.trim().isEmpty()||title.trim().length()>60||description==null||description.trim().isEmpty()||description.trim().length()>300||price==null)return false;
        try{return Double.parseDouble(price.trim().replace(",","."))>=0;}catch(NumberFormatException e){return false;}
    }
    public static boolean canSendAlert(long previous,long now){return now>=previous&&now-previous>=15000;}
    public static String generateAlertId(){return UUID.randomUUID().toString();}
    public static boolean isNewAlertId(String id,Collection<String> processed){return id!=null&&!id.trim().isEmpty()&&processed!=null&&!processed.contains(id);}
}
