package james.alarmio.data;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;
import java.util.Locale;

import io.reactivex.annotations.Nullable;
import james.alarmio.R;

public class AlarmData implements Parcelable {

    private static final String PREF_NAME = "alarmName%d";
    private static final String PREF_TIME = "alarmTime%d";
    private static final String PREF_ENABLED = "alarmEnabled%d";
    private static final String PREF_DAY = "alarmDay%d-%d";
    private static final String PREF_VIBRATE = "alarmVibrate%d";
    private static final String PREF_RINGTONE = "alarmRingtone%d";
    private static final String PREF_RINGTONE_ENABLED = "alarmRingtoneEnabled%d";

    private int id;
    public String name;
    public Calendar time;
    public boolean isEnabled = true;
    public boolean[] days = new boolean[7];
    public boolean isVibrate = true;
    public Uri ringtone;
    public boolean isRingtone = true;

    public AlarmData(int id, Calendar time) {
        this.id = id;
        this.time = time;
        ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    }

    public AlarmData(int id, Context context, SharedPreferences prefs) {
        this.id = id;
        name = prefs.getString(String.format(Locale.getDefault(), PREF_NAME, id), getName(context));
        time = Calendar.getInstance();
        time.setTimeInMillis(prefs.getLong(String.format(Locale.getDefault(), PREF_TIME, id), 0));
        isEnabled = prefs.getBoolean(String.format(Locale.getDefault(), PREF_ENABLED, id), isEnabled);
        for (int i = 0; i < 7; i++) {
            days[i] = prefs.getBoolean(String.format(Locale.getDefault(), PREF_DAY, id, i), false);
        }
        isVibrate = prefs.getBoolean(String.format(Locale.getDefault(), PREF_VIBRATE, id), isVibrate);
        try {
            ringtone = Uri.parse(prefs.getString(String.format(Locale.getDefault(), PREF_RINGTONE, id), ""));
        } catch (Exception e) {
            ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }

        isRingtone = prefs.getBoolean(String.format(Locale.getDefault(), PREF_RINGTONE_ENABLED, id), isRingtone);
    }

    public String getName(Context context) {
        if (name != null)
            return name;
        else return context.getString(R.string.title_alarm, id + 1);
    }

    public boolean isSingleTime() {
        for (boolean day : days) {
            if (day)
                return true;
        }

        return false;
    }

    public Ringtone getRingtone(Context context) {
        return RingtoneManager.getRingtone(context, ringtone);
    }

    public String getRingtoneName(Context context) {
        if (isRingtone)
            return getRingtone(context).getTitle(context);
        else return context.getString(R.string.title_none);
    }

    public void setName(SharedPreferences prefs, String name) {
        this.name = name;
        prefs.edit().putString(String.format(Locale.getDefault(), PREF_NAME, id), name).apply();
    }

    public void setTime(SharedPreferences prefs, long timeMillis) {
        time.setTimeInMillis(timeMillis);
        prefs.edit().putLong(String.format(Locale.getDefault(), PREF_TIME, id), timeMillis).apply();
    }

    public void setEnabled(SharedPreferences prefs, boolean isEnabled) {
        this.isEnabled = isEnabled;
        prefs.edit().putBoolean(String.format(Locale.getDefault(), PREF_ENABLED, id), isEnabled).apply();
    }

    public void setDays(SharedPreferences prefs, boolean[] days) {
        this.days = days;

        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < 7; i++) {
            editor.putBoolean(String.format(Locale.getDefault(), PREF_DAY, id, i), days[i]);
        }
        editor.apply();
    }

    public void setVibrate(SharedPreferences prefs, boolean isVibrate) {
        this.isVibrate = isVibrate;
        prefs.edit().putBoolean(String.format(Locale.getDefault(), PREF_VIBRATE, id), isVibrate).apply();
    }

    public void setRingtone(SharedPreferences prefs, Uri ringtone) {
        this.ringtone = ringtone;
        isRingtone = true;
        prefs.edit()
                .putString(String.format(Locale.getDefault(), PREF_RINGTONE, id), ringtone.toString())
                .putBoolean(String.format(Locale.getDefault(), PREF_RINGTONE_ENABLED, id), true)
                .apply();
    }

    public void clearRingtone(SharedPreferences prefs) {
        isRingtone = false;
        prefs.edit().putBoolean(String.format(Locale.getDefault(), PREF_RINGTONE_ENABLED, id), false).apply();
    }

    @Nullable
    public Calendar getNext() {
        if (isEnabled && isSingleTime()) {
            Calendar now = Calendar.getInstance();
            Calendar next = Calendar.getInstance();
            next.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
            next.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
            next.set(Calendar.SECOND, 0);
            if (now.after(next))
                next.add(Calendar.DATE, 1);

            for (int i = 0; i < 7; i++) {
                switch (next.get(Calendar.DAY_OF_WEEK)) {
                    case Calendar.SUNDAY:
                        if (!days[0])
                            next.add(Calendar.DATE, 1);
                        break;
                    case Calendar.MONDAY:
                        if (!days[1])
                            next.add(Calendar.DATE, 1);
                        break;
                    case Calendar.TUESDAY:
                        if (!days[2])
                            next.add(Calendar.DATE, 1);
                        break;
                    case Calendar.WEDNESDAY:
                        if (!days[3])
                            next.add(Calendar.DATE, 1);
                        break;
                    case Calendar.THURSDAY:
                        if (!days[4])
                            next.add(Calendar.DATE, 1);
                        break;
                    case Calendar.FRIDAY:
                        if (!days[5])
                            next.add(Calendar.DATE, 1);
                        break;
                    case Calendar.SATURDAY:
                        if (!days[6])
                            next.add(Calendar.DATE, 1);
                        break;
                }
            }

            return next;
        }

        return null;
    }

    public void set(Context context, AlarmManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            manager.setExact(AlarmManager.RTC_WAKEUP, getNext().getTimeInMillis(), getIntent(context));
        else manager.set(AlarmManager.RTC_WAKEUP, getNext().getTimeInMillis(), getIntent(context));
    }

    public void cancel(Context context, AlarmManager manager) {
        manager.cancel(getIntent(context));
    }

    private PendingIntent getIntent(Context context) {
        return PendingIntent.getBroadcast(context, id, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected AlarmData(Parcel in) {
        id = in.readInt();
        name = in.readString();
        time = Calendar.getInstance();
        time.setTimeInMillis(in.readLong());
        isEnabled = in.readByte() != 0;
        days = in.createBooleanArray();
        isVibrate = in.readByte() != 0;
        ringtone = in.readParcelable(Uri.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeLong(time.getTimeInMillis());
        dest.writeByte((byte) (isEnabled ? 1 : 0));
        dest.writeBooleanArray(days);
        dest.writeByte((byte) (isVibrate ? 1 : 0));
        dest.writeParcelable(ringtone, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AlarmData> CREATOR = new Creator<AlarmData>() {
        @Override
        public AlarmData createFromParcel(Parcel in) {
            return new AlarmData(in);
        }

        @Override
        public AlarmData[] newArray(int size) {
            return new AlarmData[size];
        }
    };
}
