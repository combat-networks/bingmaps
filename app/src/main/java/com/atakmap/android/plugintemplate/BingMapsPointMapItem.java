/*
Copyright (c) 2022, Nic Cellular
All rights reserved.

This source code is licensed under the BSD-style license found in the
LICENSE file in the root directory of this source tree. package com.atakmap.android.plugintemplate;
*/
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.coremap.maps.coords.GeoPoint;

public class BingMapsPointMapItem extends PointMapItem {

    public BingMapsPointMapItem(GeoPoint point, String uid) {
        super(point, uid);
    }
}
