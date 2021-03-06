/*
 * Copyright (c) 2010-2015, Isode Limited, London, England.
 * All rights reserved.
 */

package com.isode.stroke.serializer;

import com.isode.stroke.elements.Presence;
import com.isode.stroke.serializer.xml.XMLElement;

public class PresenceSerializer extends GenericStanzaSerializer<Presence> {

public PresenceSerializer(PayloadSerializerCollection payloadSerializers) {
    this(payloadSerializers, null);
}

public PresenceSerializer(PayloadSerializerCollection payloadSerializers, String explicitNS) {
    super(Presence.class, "presence", payloadSerializers, explicitNS);
}

    @Override
    void setStanzaSpecificAttributesGeneric(Presence presence, XMLElement element) {
        switch (presence.getType()) {
		case Unavailable: element.setAttribute("type","unavailable"); break;
		case Probe: element.setAttribute("type","probe"); break;
		case Subscribe: element.setAttribute("type","subscribe"); break;
		case Subscribed: element.setAttribute("type","subscribed"); break;
		case Unsubscribe: element.setAttribute("type","unsubscribe"); break;
		case Unsubscribed: element.setAttribute("type","unsubscribed"); break;
		case Error: element.setAttribute("type","error"); break;
		case Available: break;
	}
    }


}
