package fr.geming400.localisationhelper.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import contacts.core.entities.Contact;
import contacts.core.entities.Phone;
import contacts.core.entities.RawContact;
import fr.geming400.localisationhelper.LogTags;

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


    public static String formatNumber(@Nullable String number, @NonNull String defaultNum) {
        if (number == null)
             return null;

        String res = PhoneNumberUtils.formatNumber(number, Locale.getDefault().getISO3Language());
        return res == null ? defaultNum : res;
    }
    @Nullable
    public static String formatNumber(@Nullable String number) {
        return formatNumber(number, "INVALID NUMBER");
    }

    @NonNull
    public static String formatNumber(Phone phone) {
        String res = PhoneNumberUtils.formatNumber(
                phone.getNormalizedNumber() == null ? "" : phone.getNormalizedNumber(),
                Locale.getDefault().getISO3Language()
        );

        return res == null
                ? (phone.getNumber() == null ? "Unknown number" : phone.getNumber())
                : res;
    }

    public static boolean isNumberValid(@Nullable String number) {
        if (number == null)
            return false;

        return PhoneNumberUtils.formatNumber(number, Locale.getDefault().getISO3Language()) == null;
    }
    public static boolean isNumberValid(@Nullable Phone phone) {
        if (phone == null)
            return false;

        return PhoneNumberUtils.formatNumber(phone.getNormalizedNumber(), Locale.getDefault().getISO3Language()) != null;
    }

    public static boolean hasAnyValidNumber(@Nullable Contact contact) {
        if (contact == null)
            return false;

        for (RawContact rawContact : contact.getRawContacts()) {
            for (Phone phone : rawContact.getPhones()) {
                if (isNumberValid(phone))
                    return true;
            }
        }

        return false;
    }

    public static boolean areAllNumbersValid(@Nullable Contact contact) {
        if (contact == null)
            return false;

        for (RawContact rawContact : contact.getRawContacts()) {
            for (Phone phone : rawContact.getPhones()) {
                if (!isNumberValid(phone))
                    return false;
            }
        }

        return true;
    }

    @Nullable
    public static Phone getFirstValidPhone(@Nullable Contact contact) {
        if (contact == null)
            return null;

        for (RawContact rawContact : contact.getRawContacts()) {
            for (Phone phone : rawContact.getPhones()) {
                if (isNumberValid(phone))
                    return phone;
            }
        }

        return null;
    }

    @Unmodifiable
    public static Collection<Phone> getPhones(@Nullable Contact contact, boolean mustBeValid, boolean ignoreCopies) {
        if (contact == null)
            return Set.of();

        Set<Phone> phones = new HashSet<>();
        for (RawContact rawContact : contact.getRawContacts()) {
            for (Phone phone : rawContact.getPhones()) {
                boolean isPhoneDuplicate = phones
                        .stream()
                        .anyMatch(phoneElem -> Objects.equals(phoneElem.getNormalizedNumber(), phone.getNormalizedNumber()));
                if (ignoreCopies && isPhoneDuplicate)
                    continue;

                if (mustBeValid) {
                    if (isNumberValid(phone)) {
                        phones.add(phone);
                    }
                } else {
                    phones.add(phone);
                }
            }
        }

        return phones;
    }

    public static void sendSMS(@NonNull Context context, @NonNull String sender, String body) {
        Log.d(LogTags.SMS_SENDER, "Sending sms to" + sender + "with body: " + body);

        var smsManager = context.getSystemService(SmsManager.class);
        smsManager.sendMultipartTextMessage(sender, null, smsManager.divideMessage(body), null, null);
    }
}
