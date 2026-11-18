package com.hospital.util;

public class HospitalIdUtils {

    public static String normalizeHospitalId(String hospitalId) {
        if (hospitalId == null) {
            return null;
        }
        if (hospitalId.startsWith("HOSP")) {
            return "H" + hospitalId.substring(4);
        }
        return hospitalId;
    }
}
