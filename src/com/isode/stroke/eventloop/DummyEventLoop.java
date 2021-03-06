/*
 * Copyright (c) 2012 Isode Limited, London, England.
 * All rights reserved.
 */
/*
 * Copyright (c) 2010 Remko Tronçon
 * All rights reserved.
 */

package com.isode.stroke.eventloop;

import java.util.Vector;

/**
 * Dummy event loop that can be used for tests, etc. This class is thread-safe
 * as it uses {@link Vector} which is thread-safe.
 */
public class DummyEventLoop extends EventLoop {
    private Vector<Event> events_ = new Vector<Event>();

    /**
     * Constructor
     */
    public DummyEventLoop() {
    }

    /**
     * Process pending events. This method is thread-safe as it is intended to
     * be always called from the same thread for an instance of
     * {@link DummyEventLoop}
     */
    public void processEvents() {
        while (!events_.isEmpty()) {
            handleEvent(events_.get(0));
            events_.remove(0);
        }
    }

    /**
     * @return TRUE if there are any pending events.
     */
    public boolean hasEvents() {
        return !events_.isEmpty();
    }

    public void post(Event event) {
        events_.add(event);
    }
}
