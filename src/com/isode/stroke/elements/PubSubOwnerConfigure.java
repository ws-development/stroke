/*
* Copyright (c) 2014, Isode Limited, London, England.
* All rights reserved.
*/
/*
* Copyright (c) 2014, Remko Tronçon.
* All rights reserved.
*/

package com.isode.stroke.elements;

import com.isode.stroke.elements.Form;
import com.isode.stroke.elements.PubSubOwnerPayload;

public class PubSubOwnerConfigure extends PubSubOwnerPayload {

public PubSubOwnerConfigure() {
}

public Form getData() {
	return data_;
}

public void setData(Form data) {
	data_ = data;
}

public String getNode() {
	return node_;
}

public void setNode(String node) {
	node_ = node;
}

private Form data_;
private String node_;

}
