package com.j4ev3.communication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

//http://stackoverflow.com/questions/11261507/bluecove-laptop-and-an-android-tablet-with-bluetooth
public class BluetoothCommunicator implements ICommunicator {
    //http://bluecove.org/bluecove/apidocs/javax/bluetooth/UUID.html
    private static final UUID SPP_UUID = new UUID("0000110100001000800000805F9B34FB", false);
    private final String address;
    private StreamConnection conn;
    private InputStream is;
    private OutputStream os;
    private byte[] response;
    public BluetoothCommunicator(String BTAddress) {
        address = BTAddress;
    }
    
    @Override
    public void open() throws IOException {
        LocalDevice localDevice = LocalDevice.getLocalDevice();
        DiscoveryAgent agent = localDevice.getDiscoveryAgent();
        RemoteDevice[] rems = agent.retrieveDevices(DiscoveryAgent.PREKNOWN);
        RemoteDevice remote = null;
        for (RemoteDevice dev : rems) {
            String adr = dev.getBluetoothAddress();
            if (adr.equalsIgnoreCase(address)) {
                remote = dev;
                break;
            }
        }
        if (remote == null) {
            throw new IOException("Device was not found");
        }
        String url = agent.selectService(SPP_UUID, ServiceRecord.NOAUTHENTICATE_NOENCRYPT, true);
        conn = (StreamConnection) Connector.open(url, Connector.READ_WRITE);
        is = conn.openInputStream();
        os = conn.openOutputStream();
    }

    @Override
    public void close() {
        try {
            conn.close();
            is.close();
            os.close();
        } catch (IOException ex) {
            
        }
    }

    @Override
    public void write(byte[] data, int timeout) throws RuntimeException, IOException {
        os.write(data);
        os.flush();
        byte cmd = data[4];
        if ((cmd & 0x80) == 0x80) {
            //NO_REPLY
            response = null;
        } else {
            //REPLY
            int available = is.available();
            int counter = 0;
            int step = 50;
            while (available == 0 && counter < timeout) {
                try {
                    Thread.sleep(step);
                } catch (InterruptedException ex) { }
                available = is.available();
                counter += step;
            }
            response = new byte[available];
            is.read(response);
        }
    }

    @Override
    public byte[] read(int length, int timeout) throws RuntimeException {
        return response;
    }

}