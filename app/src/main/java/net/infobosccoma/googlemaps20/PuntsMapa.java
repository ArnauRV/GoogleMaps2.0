package net.infobosccoma.googlemaps20;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Arnau on 06/03/2015.
 */
public class PuntsMapa implements Parcelable {
    private int _id;
    private String name, city;
    private Float latitude,longitude;

    public PuntsMapa(int _id, String name, Float latitude, Float longitude, String city) {
        this._id = _id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.city = city;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Float getLatitude() {
        return latitude;
    }

    public void setLatitude(Float latitude) {
        this.latitude = latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    public void setLongitude(Float longitude) {
        this.longitude = longitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_id);
        dest.writeString(name);
        dest.writeFloat(latitude);
        dest.writeFloat(longitude);
        dest.writeString(city);
    }

    public static final Parcelable.Creator<PuntsMapa> CREATOR = new Parcelable.Creator<PuntsMapa>() {
        public PuntsMapa createFromParcel(Parcel in) {
            return new PuntsMapa(in);
        }

        public PuntsMapa[] newArray(int size) {
            return new PuntsMapa[size];
        }
    };

    private PuntsMapa(Parcel dest) {
        _id = dest.readInt   ();
        name  = dest.readString();
        latitude   = dest.readFloat();
        longitude   = dest.readFloat();
        city   = dest.readString();
    }
}
