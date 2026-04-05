package fr.geming400.localisationhelper;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

public final class Utils {
    private Utils() {}

    public static boolean hasLocationPermissions(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
               || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks if the given {@code string} is strictly an int (aka is not float)
     * @param string the string to check
     * @return if the string is an int
     */
    public static boolean isInt(String string) {
        // If the int is equal to the float then it's an integer
        //
        // ex:
        // string = "5":
        // Integer.valueOf("5").floatValue() == Float.parseFloat("5")
        // 5.floatValue() == 5f
        // 5f == 5f
        //
        // string = "5.8":
        // Integer.valueOf("5.8").floatValue() == Float.parseFloat("5.8")
        // 5.floatValue() == 5.8f
        // 5f != 5.8f

        try {
            return Integer.valueOf(string).floatValue() == Float.parseFloat(string);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
