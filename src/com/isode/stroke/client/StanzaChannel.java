/*
 * Copyright (c) 2010-2014, Isode Limited, London, England.
 * All rights reserved.
 */
/*
 * Copyright (c) 2010, Remko Tron?on.
 * All rights reserved.
 */

package com.isode.stroke.client;

import java.util.List;

import com.isode.stroke.elements.Message;
import com.isode.stroke.elements.Presence;
import com.isode.stroke.elements.Stanza;
import com.isode.stroke.queries.IQChannel;
import com.isode.stroke.signals.Signal1;
import com.isode.stroke.tls.Certificate;

public abstract class StanzaChannel extends IQChannel {

    
    public abstract void sendMessage(Message message);

    public abstract void sendPresence(Presence presence);

    public abstract boolean isAvailable();

    public abstract boolean getStreamManagementEnabled();
    public abstract List<Certificate> getPeerCertificateChain();

    public final Signal1<Message> onMessageReceived = new Signal1<Message>();
    public final Signal1<Presence> onPresenceReceived = new Signal1<Presence>();
    public final Signal1<Boolean /* isAvailable */ > onAvailableChanged = new Signal1<Boolean>();
    public final Signal1<Stanza> onStanzaAcked = new Signal1<Stanza>();


}
