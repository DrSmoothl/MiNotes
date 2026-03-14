package net.micode.notes.infrastructure.system;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import net.micode.notes.domain.service.ContactNameResolver;

import java.util.HashMap;
import java.util.Map;

public final class AndroidContactNameResolver implements ContactNameResolver {
    private static final String TAG = "AndroidContactNameResolver";
    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
            + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
            + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id FROM phone_lookup WHERE min_match = '+')";

    private final Context appContext;
    private final Map<String, String> contactCache = new HashMap<String, String>();

    public AndroidContactNameResolver(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public String resolve(String phoneNumber) {
        if (contactCache.containsKey(phoneNumber)) {
            return contactCache.get(phoneNumber);
        }
        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));
        Cursor cursor = appContext.getContentResolver().query(
                Data.CONTENT_URI,
                new String[] { Phone.DISPLAY_NAME },
                selection,
                new String[] { phoneNumber },
                null);
        if (cursor == null) {
            Log.d(TAG, "No contact matched with number: " + phoneNumber);
            return null;
        }
        try {
            if (!cursor.moveToFirst()) {
                Log.d(TAG, "No contact matched with number: " + phoneNumber);
                return null;
            }
            String name = cursor.getString(0);
            contactCache.put(phoneNumber, name);
            return name;
        } catch (IndexOutOfBoundsException exception) {
            Log.e(TAG, "Cursor get string error", exception);
            return null;
        } finally {
            cursor.close();
        }
    }
}