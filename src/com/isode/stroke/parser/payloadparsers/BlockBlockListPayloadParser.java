/*  Copyright (c) 2016, Isode Limited, London, England.
 *  All rights reserved.
 *
 *  Acquisition and use of this software and related materials for any
 *  purpose requires a written license agreement from Isode Limited,
 *  or a written license from an organisation licensed by Isode Limited
 *  to grant such a license.
 *
 */
package com.isode.stroke.parser.payloadparsers;

import com.isode.stroke.elements.BlockListPayload;

/**
 * {@link BlockParser} for pay loads of type {@link BlockListPayload}
 */
public class BlockBlockListPayloadParser extends BlockParser<BlockListPayload> {

    /**
     * Constructor
     */
    public BlockBlockListPayloadParser() {
        super(new BlockListPayload());
    }

}
