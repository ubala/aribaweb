package app;

/**
    Fake implementation of the com.macroselfian.gps.Locator
    interface from the ZK maps example (http://www.zkoss.org/smalltalks/gwtZk/)
 */
public class Locator
{
    double latitude, longitude
    String description

    static List Locs = [
        [48.858227, 2.29442, "Eiffel Tower, Paris, Paris, IDF, France"],
        [37.81347953629422, -122.47777462005615, "Golden Gate Bridge, San Francisco, California"],
        [40.74823233074709, -73.98517370223999, "Empire State Building, New York City, New York"],
    ].collect { new Locator(latitude:it[0], longitude:it[1], description:it[2]) }

    static Locator locate (String str)
    {
        Locs.find { Locator l -> l.description.toLowerCase().contains(str.toLowerCase()) } ?:
          new Locator(latitude:0.0, longitude:0.0, description:"\"${str}\" -- not found")
    }
}
