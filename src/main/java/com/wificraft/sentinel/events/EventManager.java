package com.wificraft.sentinel.events;

import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager implements Listener {
    private final Map<String, List<EventListener>> eventListeners;
    private final Map<String, EventPriority> eventPriorities;
    private final Map<String, EventFilter> eventFilters;

    public EventManager() {
        this.eventListeners = new ConcurrentHashMap<>();
        this.eventPriorities = new ConcurrentHashMap<>();
        this.eventFilters = new ConcurrentHashMap<>();
    }

    public void registerEvent(String eventName, EventListener listener, EventPriority priority, EventFilter filter) {
        eventListeners.computeIfAbsent(eventName, k -> new ArrayList<>()).add(listener);
        eventPriorities.put(eventName, priority);
        eventFilters.put(eventName, filter);
    }

    public void unregisterEvent(String eventName) {
        eventListeners.remove(eventName);
        eventPriorities.remove(eventName);
        eventFilters.remove(eventName);
    }

    public void fireEvent(Event event) {
        String eventName = event.getEventName();
        List<EventListener> listeners = eventListeners.get(eventName);
        
        if (listeners != null) {
            EventFilter filter = eventFilters.get(eventName);
            if (filter == null || filter.shouldProcess(event)) {
                EventPriority priority = eventPriorities.get(eventName);
                
                // Sort listeners by priority if needed
                if (priority != null) {
                    listeners.sort((l1, l2) -> {
                        int p1 = l1.getPriority().ordinal();
                        int p2 = l2.getPriority().ordinal();
                        return Integer.compare(p1, p2);
                    });
                }
                
                // Execute listeners
                for (EventListener listener : listeners) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        // Log error but continue execution
                        System.err.println("Error in event listener: " + e.getMessage());
                    }
                }
            }
        }
    }

    public void registerCustomEvent(String eventName, EventListener listener, EventPriority priority) {
        registerEvent(eventName, listener, priority, null);
    }

    public void registerFilteredEvent(String eventName, EventListener listener, EventPriority priority, EventFilter filter) {
        registerEvent(eventName, listener, priority, filter);
    }

    public enum EventPriority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST
    }

    public interface EventListener {
        EventPriority getPriority();
        void onEvent(Event event);
    }

    public interface EventFilter {
        boolean shouldProcess(Event event);
    }
}
