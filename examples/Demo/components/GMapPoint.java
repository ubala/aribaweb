package components;

public class GMapPoint
{
    static double earthRadius = 6371; // km
    static double R(double deg) { return deg * (Math.PI / 180); }  // degToRadians
    static double D(double rad) { return rad * 180 / Math.PI; }  // radToDegree
    static double toBearing(double rad) { return D(rad) + 360 % 360; }  // radToDeg

    double latitude, longitude;

    public GMapPoint(double latitude, double longitude)
    {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude()
    {
        return latitude;
    }

    public void setLatitude(double latitude)
    {
        this.latitude = latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public void setLongitude(double longitude)
    {
        this.longitude = longitude;
    }

    public static double distance (GMapPoint start, GMapPoint end) {
        return Math.acos(Math.sin(R(start.latitude))*Math.sin(R(end.latitude)) +
                  Math.cos(R(start.latitude))*Math.cos(R(end.latitude)) *
                  Math.cos(R(end.longitude - start.longitude))) * earthRadius;
    }

    public static GMapPoint pointForDistance (GMapPoint start, GMapPoint end, double distance) {
        double lat1 = R(start.latitude), lat2 = R(end.latitude);
        double lon1 = R(start.longitude), dLon = R(end.longitude - start.longitude);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2) -
              Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
        double brng = Math.atan2(y, x);

        double mLat = Math.asin( Math.sin(lat1)*Math.cos(distance/earthRadius) +
                            Math.cos(lat1)*Math.sin(distance/earthRadius)*Math.cos(brng) );
        double mLon = lon1 + Math.atan2(Math.sin(brng)*Math.sin(distance/earthRadius)*Math.cos(lat1),
                                   Math.cos(distance/earthRadius)-Math.sin(lat1)*Math.sin(lat2));
        mLon = (mLon + Math.PI)%(2 * Math.PI) - Math.PI;  // normalise to -180...+180

        return new GMapPoint(D(mLat), D(mLon));
    }
}
