package ro.upt.sma.blechat;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import java.util.UUID;

public class ChatProfile {

  public static final UUID CHAT_SERVICE =
      UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
  public static final UUID CHAT_ROOM_CHARACTERISTIC =
      UUID.fromString("0000000a-0000-1000-8000-00805f9b34fb");
  public static final UUID CHAT_POST_CHARACTERISTIC =
      UUID.fromString("0000000b-0000-1000-8000-00805f9b34fb");

  /* Mandatory Client Characteristic Config Descriptor */
  public static UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


  public static BluetoothGattService createChatService() {
    BluetoothGattService service = new BluetoothGattService(CHAT_SERVICE,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);

    BluetoothGattCharacteristic roomCharacteristic = new BluetoothGattCharacteristic(
        CHAT_ROOM_CHARACTERISTIC,
        BluetoothGattCharacteristic.PROPERTY_READ
            | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        BluetoothGattCharacteristic.PERMISSION_READ);

    BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(CLIENT_CONFIG,
        BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
    roomCharacteristic.addDescriptor(configDescriptor);

    BluetoothGattCharacteristic postCharacteristic = new BluetoothGattCharacteristic(
        CHAT_POST_CHARACTERISTIC,
        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
        BluetoothGattCharacteristic.PERMISSION_WRITE);

    service.addCharacteristic(roomCharacteristic);
    service.addCharacteristic(postCharacteristic);

    return service;
  }
}
