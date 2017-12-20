package ro.upt.sma.blechat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import ro.upt.sma.blechat.BleGattServerWrapper.ChatListener;


public class GattServerActivity extends AppCompatActivity {

  private static final String TAG = GattServerActivity.class.getSimpleName();

  private ChatRecyclerViewAdapter chatAdapter;

  private BleGattServerWrapper bleWrapper;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_gatt_server);

    RecyclerView rvChatList = findViewById(R.id.rv_chat_list);
    rvChatList.setLayoutManager(new LinearLayoutManager(this));
    this.chatAdapter = new ChatRecyclerViewAdapter();
    rvChatList.setAdapter(chatAdapter);

    this.bleWrapper = new BleGattServerWrapper(this);
    bleWrapper.registerListener(new ChatListener() {
      @Override
      public void onMessageAdded(String sender, String message) {
        Log.d(TAG, "onMessageAdded: " + message);

        addToChatList(sender, message);
      }

      @Override
      public void onError() {
        finish();
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    bleWrapper.unregisterListener();
  }

  private void addToChatList(final String name, final String message) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        chatAdapter.addMessage(name + ": " + message);
      }
    });
  }

}
