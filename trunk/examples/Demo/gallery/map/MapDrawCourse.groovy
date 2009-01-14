package gallery.map

import ariba.ui.aribaweb.core.AWComponent

class MapDrawCourse extends AWComponent
{
    List markers = []
    def lng, lat
    def displayGroup, marker

    public boolean isStateless() { return false }

    void addMarker () {
        markers += [latitude:lat, longitude: lng]
    }

    void deleteMarker () {
        markers.remove(marker)
    }

    void markerClicked () {
        displayGroup.setSelectedObject(marker)
    }

    void markerDragged () {
        marker.latitude = lat;
        marker.longitude = lng;
    }
}
