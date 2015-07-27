/*
 * Copyright (c) 2011 Tobias Markmann
 * Licensed under the simplified BSD license.
 * See Documentation/Licenses/BSD-simplified.txt for more information.
 */
/*
 * Copyright (c) 2015 Tarun Gupta.
 * Licensed under the simplified BSD license.
 * See Documentation/Licenses/BSD-simplified.txt for more information.
 */

package com.isode.stroke.network;

import com.isode.stroke.signals.Signal1;

public abstract class NATTraversalForwardPortRequest {

	public abstract void start();
	public abstract void stop();

	public final Signal1<NATPortMapping> onResult = new Signal1<NATPortMapping>();
}