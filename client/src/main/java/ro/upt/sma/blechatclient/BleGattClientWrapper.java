package ro.upt.sma.blechatclient;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.content.Context.BLUETOOTH_SERVICE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import java.nio.charset.Charset;
import java.util.UUID;

public class BleGattClientWrapper {

  private static final String TAG = BleGattClientWrapper.class.getSimpleName();

  interface MessageListener {

    enum ConnectionStatus {
      CONNECTED, DISCONNECTED
    }

    void onMessageAdded(String message);

    void onMessageSent(String message);

    void onConnectionStateChanged(ConnectionStatus connectionStatus);

    void onError(String message);

  }

  public static final UUID CHAT_SERVICE =
      UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
  public static final UUID CHAT_ROOM_CHARACTERISTIC =
      UUID.fromString("0000000a-0000-1000-8000-00805f9b34fb");
  public static final UUID CHAT_POST_CHARACTERISTIC =
      UUID.fromString("0000000b-0000-1000-8000-00805f9b34fb");

  /* Mandatory Client Characteristic Config Descriptor */
  public static UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

  private final Context context;

  private BluetoothManager bluetoothManager;

  private BluetoothGatt gatt;

  public BleGattClientWrapper(Context context) {
    this.context = context;
    this.bluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
  }

  public void sendMessage(String message, MessageListener messageListener) {
    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
      messageListener.onError("Bluetooth not enable or available.");
      return;
    }

    BluetoothDevice bluetoothDevice = gatt.getDevice();

    if (bluetoothDevice == null) {
      messageListener.onError("Bluetooth device not available");
      return;
    }

    if (gatt == null
        && gatt.getConnectionState(bluetoothDevice) != BluetoothAdapter.STATE_CONNECTED) {
      messageListener.onError("Bluetooth device not connected");
      return;
    }

    BluetoothGattCharacteristic characteristic = gatt.getService(CHAT_SERVICE)
        .getCharacteristic(CHAT_POST_CHARACTERISTIC);

    characteristic.setValue(message.getBytes(Charset.defaultCharset()));

    boolean success = gatt.writeCharacteristic(characteristic);

    if (success) {
      messageListener.onMessageSent(message);
    } else {
      messageListener.onError("Message was not sent");
    }
  }

  public void registerMessageListener(String address, final MessageListener messageListener) {
    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
      messageListener.onError("Bluetooth not enable or available.");
      return;
    }

    BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);

    if (bluetoothDevice == null) {
      messageListener.onError("Bluetooth device not available");
      return;
    }

    this.gatt = bluetoothDevice.connectGatt(context, false, new BluetoothGattCallback() {
      @Override
      public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);

        if (newState == STATE_CONNECTED) {
          // TODO 1: once connection was established then trigger discover service

          // TODO 2: inform message listener about CONNECTED status

        } else {
          // TODO 3: inform message listener about DISCONNECTED status
        }
      }

      @Override
      public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        // TODO 4: get a handle for CHAT_ROOM_CHARACTERISTIC
        BluetoothGattCharacteristic characteristic = null;
        //
        gatt.setCharacteristicNotification(characteristic, true);


        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CONFIG);
        // TODO 5: write descriptor for same characteristic to enable notifications
      }

      @Override
      public void onCharacteristicChanged(
          BluetoothGatt gatt,
          BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        messageListener.onMessageAdded(
            new String(characteristic.getValue(), Charset.defaultCharset()));
      }
    });
  }

  public void unregisterListener() {
    if (gatt != null) {
      gatt.disconnect();
    }
  }

}
