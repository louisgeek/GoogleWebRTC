package com.example.googlewebrtc.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.example.googlewebrtc.R;
import com.serenegiant.usb.DeviceFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by louisgeek on 2019/7/25.
 */
public class UVCCamerarHelper {

    public static List<UsbDevice> getDeviceList(final Context context, final List<DeviceFilter> filters) {
        final UsbManager mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        final HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        final List<UsbDevice> result = new ArrayList<UsbDevice>();
        if (deviceList != null) {
            for (final DeviceFilter filter : filters) {
                final Iterator<UsbDevice> iterator = deviceList.values().iterator();
                UsbDevice device;
                while (iterator.hasNext()) {
                    device = iterator.next();
                    if ((filter == null) || (filter.matches(device))) {
                        result.add(device);
                    }
                }
            }
        }
        return result;
    }

    public static boolean hasUVCCamera(Context context) {
        boolean hasUVCCamera = false;
        final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(context, R.xml.device_filter);
        List<UsbDevice> deviceList = getDeviceList(context, filter);
        if (deviceList.size() > 0) {
            hasUVCCamera = true;
        }
        return hasUVCCamera;
    }
}
