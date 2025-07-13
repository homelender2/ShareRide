package com.companyname.shareride;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class MyRidesFragment extends Fragment {

    private LinearLayout chatContainer;
    private EditText etMessage;
    private ImageButton btnSend;
    private List<ChatMessage> chatMessages;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_rides, container, false);

        initViews(view);
        setupChat();

        return view;
    }

    private void initViews(View view) {
        chatContainer = view.findViewById(R.id.chat_container);
        etMessage = view.findViewById(R.id.et_message);
        btnSend = view.findViewById(R.id.btn_send);
    }

    private void setupChat() {
        chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("Priya", "I'm at the bus stop, wearing blue jacket", false));
        chatMessages.add(new ChatMessage("You", "On my way, 2 mins!", true));
        chatMessages.add(new ChatMessage("Amit", "Auto driver is here, white auto", false));

        displayChatMessages();

        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                chatMessages.add(new ChatMessage("You", message, true));
                displayChatMessages();
                etMessage.setText("");
            }
        });
    }

    private void displayChatMessages() {
        chatContainer.removeAllViews();

        for (ChatMessage message : chatMessages) {
            View messageView = LayoutInflater.from(getContext()).inflate(R.layout.item_chat_message, chatContainer, false);
            TextView tvMessage = messageView.findViewById(R.id.tv_message);
            tvMessage.setText(message.getSender() + ": " + message.getMessage());

            if (message.isOwnMessage()) {
                tvMessage.setBackgroundResource(R.drawable.bg_chat_own);
            } else {
                tvMessage.setBackgroundResource(R.drawable.bg_chat_other);
            }

            chatContainer.addView(messageView);
        }
    }
}