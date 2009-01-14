package gallery.map

import ariba.ui.aribaweb.core.AWComponent
import ariba.ui.aribaweb.core.AWPage
import ariba.ui.aribaweb.util.AWChangeNotifier
import components.GMapPoint

class MapRace extends AWComponent
{
    double lat, lng;
    def courseDisplayaGroup, marker, segment
    def racerDisplayGroup, racer
    Race race = new Race()
    Course course = race.course
    boolean showChart
    
    public boolean isStateless() { return false }

    void addMarker () {
        course.addMarker(lat, lng);
    }

    void deleteSegment () {
        course.removeMarker(segment.end);
    }

    void markerClicked () {
        courseDisplayaGroup.setSelectedObject(course.segments.find { it.end == marker })
    }

    void markerDragged () {
        marker.latitude = lat;
        marker.longitude = lng;
    }

    void addRacer () {
        race.racers += new Racer(name:"Enter Name");
    }

    void deleteRacer () {
        race.racers.remove(racer);
    }

    GMapPoint racerPosition () { course.pointForDistance(racer.distance) }

    void start () {
        AWPage page = page()
        page.setPollInterval(2)
        page.setPollingInitiated(true)
        race.start(page.getChangeNotifier())
    }

    void stop () {
        page().setPollingInitiated(false)
        race.stop()
    }
}

class Race {
    Course course = new Course()
    List racers = []

    int timeAccerationFactor = 60;  // (1 min -> 1 hr)
    int time = 0;
    boolean running = false;

    void start (AWChangeNotifier notifier) {
        Thread.start {
            running = true;
            long lastTime = System.currentTimeMillis();
            while (running) {
                Thread.sleep(1000);
                long curTime = System.currentTimeMillis();
                long delta = (curTime - lastTime) * timeAccerationFactor / 1000;
                time += delta;
                racers.each { Racer r -> if (r.distance < course.length()) r.advance(delta) }
                lastTime = curTime;
                notifier.notifyChange()
            }
        }
    }

    void stop () {
        running = false;
    }

    void reset () {
        racers.each { it.distance = 0.0; }
        time = 0;
    }    
}

class Racer {
    String name;
    double speed;           // km/hr
    double distance = 0.0;  // travelled so far

    void advance (long secs) { distance += (speed/3600 * secs) } 
}

class Course {
    List<GMapPoint> markers = []
    List<Segment> segments = []

    void updateSegments () {
        segments = [];
        for (int i=1; i<markers.size(); i++) {
            segments += new Segment(number: i, start:markers[i-1], end:markers[i])
        }
    }

    def addMarker (lat, lng) { markers += new GMapPoint(lat, lng); updateSegments() }
    def removeMarker (marker) { markers.remove(marker); updateSegments() }

    double length () { segments.inject(0.0) { double tot, Segment seg -> tot + seg.distance() } }

    GMapPoint pointForDistance (double distance) {
        for (Segment seg : segments) {
            if (distance < seg.distance()) return seg.pointForDistance(distance);
            distance -= seg.distance();
        }
        return segments[-1].end;
    }
}

class Segment {
    int number
    GMapPoint start, end

    double distance () { GMapPoint.distance(start, end) }
    
    GMapPoint pointForDistance (double distance) { GMapPoint.pointForDistance(start, end, distance) }
}
