package ariba.ui.aribaweb.util;

import ariba.util.core.GrowOnlyHashtable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AWNotificationCenter
{
    static GrowOnlyHashtable _observersByTopic = new GrowOnlyHashtable();

    public interface Observer
    {
        public void onNotification (String topic, Object data);
    }

    public static void addObserver (String topic, Observer observer)
    {
        List observers = (List)_observersByTopic.get(topic);
        if (observers == null) {
            observers = new CopyOnWriteArrayList();
            _observersByTopic.put(topic, observers);
        }
        observers.add(observer);
    }

    public static void notify (String topic, Object data)
    {
        List<Observer> observers = (List)_observersByTopic.get(topic);
        if (observers == null) return;
        for (Observer observer: observers) {
            observer.onNotification(topic, data);
        }
    }
}
