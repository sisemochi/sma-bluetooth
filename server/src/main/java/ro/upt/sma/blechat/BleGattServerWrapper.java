package ro.upt.sma.blechat;

import static android.content.Context.BLUETOOTH_SERVICE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * The BLE communication skeleton was provided by an Android Things example:
 * https://github.com/androidthings/sample-bluetooth-le-gattserver
 */

public class BleGattServerWrapper {

  private ChatListener chatListener;

  interface ChatListener {

    void onMessageAdded(String sender, String message);

    void onError();

  }

  private static final String TAG = BleGattServerWrapper.class.getSimpleName();

  private final Context context;

  private BluetoothManager bluetoothManager;
  private BluetoothGattServer bluetoothGattServer;
  private BluetoothLeAdvertiser bluetoothLeAdvertiser;

  private Set<BluetoothDevice> registeredDevices = new HashSet<>();

  public BleGattServerWrapper(Context context) {
    this.context = context;
    this.bluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
  }

  public void registerListener(ChatListener chatListener) {
    this.chatListener = chatListener;

    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

    if (!checkBluetoothSupport(bluetoothAdapter)) {
      chatListener.onError();
    }

    // Register for system Bluetooth events
    IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    context.registerReceiver(mBluetoothReceiver, filter);
    if (!bluetoothAdapter.isEnabled()) {
      Log.d(TAG, "Bluetooth is currently disabled...enabling");
      bluetoothAdapter.enable();
    } else {
      Log.d(TAG, "Bluetooth enabled...starting services");
      startAdvertising();
      startServer();
    }
  }

  public void unregisterListener() {
    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
    if (bluetoothAdapter.isEnabled()) {
      stopServer();
      stopAdvertising();
    }

    context.unregisterReceiver(mBluetoothReceiver);
  }


  /**
   * Verify the level of Bluetooth support provided by the hardware.
   *
   * @param bluetoothAdapter System {@link BluetoothAdapter}.
   * @return true if Bluetooth is properly supported, false otherwise.
   */
  private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {

    if (bluetoothAdapter == null) {
      Log.w(TAG, "Bluetooth is not supported");
      return false;
    }

    if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
      Log.w(TAG, "Bluetooth LE is not supported");
      return false;
    }

    return true;
  }

  /**
   * Listens for Bluetooth adapter events to enable/disable
   * advertising and server functionality.
   */
  private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

      switch (state) {
        case BluetoothAdapter.STATE_ON:
          startAdvertising();
          startServer();
          break;
        case BluetoothAdapter.STATE_OFF:
          stopServer();
          stopAdvertising();
          break;
        default:
          // Do nothing
      }

    }
  };

  /**
   * Begin advertising over Bluetooth that this device is connectable
   * and supports the Current Time Service.
   */
  private void startAdvertising() {
    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
    bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
    if (bluetoothLeAdvertiser == null) {
      Log.w(TAG, "Failed to create advertiser");
      return;
    }

    AdvertiseSettings settings = new AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setConnectable(true)
        .setTimeout(0)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .build();

    AdvertiseData data = new AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .setIncludeTxPowerLevel(false)
        .addServiceUuid(new ParcelUuid(ChatProfile.CHAT_SERVICE))
        .build();

    bluetoothLeAdvertiser
        .startAdvertising(settings, data, mAdvertiseCallback);
  }

  /**
   * Stop Bluetooth advertisements.
   */
  private void stopAdvertising() {
    if (bluetoothLeAdvertiser == null) {
      return;
    }

    bluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
  }

  /**
   * Initialize the GATT server instance with the services/characteristics
   * from the Time Profile.
   */
  private void startServer() {
    bluetoothGattServer = bluetoothManager.openGattServer(context, mGattServerCallback);
    if (bluetoothGattServer == null) {
      Log.w(TAG, "Unable to create GATT server");
      return;
    }

    bluetoothGattServer.addService(ChatProfile.createChatService());
  }

  /**
   * Shut down the GATT server.
   */
  private void stopServer() {
    if (bluetoothGattServer == null) {
      return;
    }

    bluetoothGattServer.close();
  }

  /**
   * Callback to receive information about the advertisement process.
   */
  private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
      Log.i(TAG, "LE Advertise Started.");
    }

    @Override
    public void onStartFailure(int errorCode) {
      Log.w(TAG, "LE Advertise Failed: " + errorCode);
    }
  };

  /**
   * Send a time service notification to any devices that are subscribed
   * to the characteristic.
   */
  private void notifyRegisteredDevices(byte[] receivedMessage) {
    if (registeredDevices.isEmpty()) {
      Log.i(TAG, "No subscribers registered");
      return;
    }

    Log.i(TAG, "Sending update to " + registeredDevices.size() + " subscribers");
    for (BluetoothDevice device : registeredDevices) {
      BluetoothGattCharacteristic timeCharacteristic = bluetoothGattServer
          .getService(ChatProfile.CHAT_SERVICE)
          .getCharacteristic(ChatProfile.CHAT_ROOM_CHARACTERISTIC);
      timeCharacteristic.setValue(receivedMessage);
      bluetoothGattServer.notifyCharacteristicChanged(device, timeCharacteristic, false);
    }
  }

  /**
   * Callback to handle incoming requests to the GATT server.
   * All read/write requests for characteristics and descriptors are handled here.
   */
  private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
        //Remove device from any active subscriptions
        registeredDevices.remove(device);
      }
    }

    @Override
    public void onCharacteristicWriteRequest(
        BluetoothDevice device,
        int requestId,
        BluetoothGattCharacteristic characteristic,
        boolean preparedWrite,
        boolean responseNeeded,
        int offset,
        byte[] value) {
      if (ChatProfile.CHAT_POST_CHARACTERISTIC.equals(characteristic.getUuid())) {
        notifyRegisteredDevices(value);
        chatListener
            .onMessageAdded(device.getAddress(), new String(value, Charset.defaultCharset()));
      } else {
        // Invalid characteristic
        Log.w(TAG, "Invalid Characteristic write: " + characteristic.getUuid());
        bluetoothGattServer.sendResponse(device,
            requestId,
            BluetoothGatt.GATT_FAILURE,
            0,
            null);
      }
    }

    @Override
    public void onCharacteristicReadRequest(
        BluetoothDevice device,
        int requestId,
        int offset,
        BluetoothGattCharacteristic characteristic
    ) {
      // TODO: Send all messages
      if (ChatProfile.CHAT_ROOM_CHARACTERISTIC.equals(characteristic.getUuid())) {
        bluetoothGattServer.sendResponse(device,
            requestId,
            BluetoothGatt.GATT_SUCCESS,
            0,
            new byte[]{});
      } else {
        // Invalid characteristic
        Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
        bluetoothGattServer.sendResponse(device,
            requestId,
            BluetoothGatt.GATT_FAILURE,
            0,
            null);
      }
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
        BluetoothGattDescriptor descriptor) {
      if (ChatProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
        Log.d(TAG, "Config descriptor read");
        byte[] returnValue;
        if (registeredDevices.contains(device)) {
          returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        } else {
          returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        }
        bluetoothGattServer.sendResponse(device,
            requestId,
            BluetoothGatt.GATT_FAILURE,
            0,
            returnValue);
      } else {
        Log.w(TAG, "Unknown descriptor read request");
        bluetoothGattServer.sendResponse(device,
            requestId,
            BluetoothGatt.GATT_FAILURE,
            0,
            null);
      }
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
        BluetoothGattDescriptor descriptor,
        boolean preparedWrite, boolean responseNeeded,
        int offset, byte[] value) {
      if (ChatProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
        if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
          Log.d(TAG, "Subscribe device to notifications: " + device);
          registeredDevices.add(device);
        } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
          Log.d(TAG, "Unsubscribe device from notifications: " + device);
          registeredDevices.remove(device);
        }

        if (responseNeeded) {
          bluetoothGattServer.sendResponse(device,
              requestId,
              BluetoothGatt.GATT_SUCCESS,
              0,
              null);
        }
      } else {
        Log.w(TAG, "Unknown descriptor write request");
        if (responseNeeded) {
          bluetoothGattServer.sendResponse(device,
              requestId,
              BluetoothGatt.GATT_FAILURE,
              0,
              null);
        }
      }
    }
  };

}
