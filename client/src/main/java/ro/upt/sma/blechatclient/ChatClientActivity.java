package ro.upt.sma.blechatclient;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import ro.upt.sma.blechatclient.BleGattClientWrapper.MessageListener;

public class ChatClientActivity extends AppCompatActivity implements MessageListener {

  private static final String TAG = ChatClientActivity.class.getSimpleName();

  private EditText etAddress;
  private Button btAction;
  private TextView tvConnectionStatus;
  private EditText etChatMessage;
  private Button btSendMessage;

  private BleGattClientWrapper bleWrapper;

  private ChatRecyclerViewAdapter chatListAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_chat);

    this.etAddress = findViewById(R.id.et_chat_address);
    this.tvConnectionStatus = findViewById(R.id.tv_chat_connection_status);
    this.btAction = findViewById(R.id.bt_chat_connectivity_action);

    this.etChatMessage = findViewById(R.id.et_chat_message);
    this.btSendMessage = findViewById(R.id.bt_chat_message_send);

    RecyclerView rvChatList = findViewById(R.id.rv_chat_room_list);
    rvChatList.setLayoutManager(new LinearLayoutManager(this));
    this.chatListAdapter = new ChatRecyclerViewAdapter();
    rvChatList.setAdapter(chatListAdapter);

    this.bleWrapper = new BleGattClientWrapper(this);
  }

  @Override
  protected void onResume() {
    super.onResume();

    btAction.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        if (!etAddress.getText().toString().isEmpty()) {
          view.setEnabled(false);
          bleWrapper.registerMessageListener(
              etAddress.getText().toString().toUpperCase(),
              ChatClientActivity.this);
        }
      }
    });

    btSendMessage.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        if (!etChatMessage.getText().toString().isEmpty()) {
          bleWrapper.sendMessage(etChatMessage.getText().toString(), ChatClientActivity.this);
        }
      }
    });
  }

  @Override
  protected void onPause() {
    super.onPause();

    bleWrapper.unregisterListener();
  }

  @Override
  public void onMessageAdded(final String message) {
    Log.d(TAG, "onMessageAdded: " + message);

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        chatListAdapter.addMessage(message);
      }
    });
  }

  @Override
  public void onMessageSent(String message) {
    Log.d(TAG, "onMessageSent: " + message);
  }

  @Override
  public void onConnectionStateChanged(final ConnectionStatus connectionStatus) {
    Log.d(TAG, "onConnectionStateChanged: " + connectionStatus);

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (connectionStatus == ConnectionStatus.CONNECTED) {
          btAction.setEnabled(false);
          tvConnectionStatus.setText(R.string.connected);
          btSendMessage.setEnabled(true);
        } else {
          btAction.setEnabled(true);
          tvConnectionStatus.setText(R.string.disconnected);
          btSendMessage.setEnabled(false);
        }
      }
    });
  }

  @Override
  public void onError(final String message) {
    Log.e(TAG, "onError: " + message);

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
      }
    });
  }

}
