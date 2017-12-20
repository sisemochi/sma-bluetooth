package ro.upt.sma.blechatclient;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.LinkedList;
import java.util.List;
import ro.upt.sma.blechatclient.ChatRecyclerViewAdapter.ChatEntryViewHolder;

public class ChatRecyclerViewAdapter extends RecyclerView.Adapter<ChatEntryViewHolder> {

  private final List<String> messages = new LinkedList<>();

  public ChatRecyclerViewAdapter() {
  }

  public void addMessage(String message) {
    messages.add(0, message);
    notifyItemInserted(0);
  }

  @Override
  public ChatEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_chat, parent, false);

    return new ChatEntryViewHolder(view);
  }

  @Override
  public void onBindViewHolder(ChatEntryViewHolder holder, int position) {
    holder.tvContent.setText(messages.get(position));
  }

  @Override
  public int getItemCount() {
    return messages.size();
  }

  class ChatEntryViewHolder extends ViewHolder {

    TextView tvContent;

    public ChatEntryViewHolder(View itemView) {
      super(itemView);
      this.tvContent = itemView.findViewById(R.id.tv_chat_item_content);
    }

  }

}
