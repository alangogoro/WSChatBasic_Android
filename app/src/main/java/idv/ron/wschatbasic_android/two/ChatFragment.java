package idv.ron.wschatbasic_android.two;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;

import idv.ron.wschatbasic_android.R;

import static idv.ron.wschatbasic_android.two.CommonTwo.chatWebSocketClient;
import static idv.ron.wschatbasic_android.two.CommonTwo.loadUserName;
import static idv.ron.wschatbasic_android.two.CommonTwo.showToast;

/*
 * 此頁可以發送與接收訊息。
 * 當ChatWebSocketClient接收到聊天訊息時會發LocalBroadcast，
 * 此頁的BroadcastReceiver會接收到並在TextView呈現。
 * */
public class ChatFragment extends Fragment {
    private static final String TAG = "TAG_ChatFragment";
    private Activity activity;
    private LocalBroadcastManager broadcastManager;
    private TextView tvMessage;
    private EditText etMessage;
    private ScrollView scrollView;
    private String friend;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 取得前頁傳來的聊天對象
        Bundle bundle = getArguments();
        if (bundle != null) {
            friend = bundle.getString("friend");
        }
        activity.setTitle("Friend: " + friend);
        // 初始化LocalBroadcastManager並註冊BroadcastReceiver
        broadcastManager = LocalBroadcastManager.getInstance(activity);
        registerChatReceiver();
        CommonTwo.connectServer(activity, loadUserName(activity));
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvMessage = view.findViewById(R.id.tvMessage);
        etMessage = view.findViewById(R.id.etMessage);
        scrollView = view.findViewById(R.id.scrollView);
        view.findViewById(R.id.btSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = etMessage.getText().toString();
                if (message.trim().isEmpty()) {
                    showToast(activity, R.string.textMessageEmpty);
                    return;
                }
                String sender = loadUserName(activity);
                // 將欲傳送訊息轉成 Json 後送出
                ChatMessage chatMessage = new ChatMessage("chat", sender, friend, message);
                String chatMessageJson = new Gson().toJson(chatMessage);
                chatWebSocketClient.send(chatMessageJson);
                Log.d(TAG, "output: " + chatMessageJson);

                // 將欲傳送訊息顯示在TextView上
                tvMessage.append(sender + ": " + message + "\n");
                // 將輸入的訊息清空
                etMessage.setText(null);
                // 捲動至最新訊息
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    /**
     * 註冊廣播接收器攔截聊天資訊
     * 因為是在Fragment註冊，所以Fragment頁面未開時不會攔截廣播
     */
    /* 攔截 "chat" 廣播 */
    private void registerChatReceiver() {
        IntentFilter chatFilter = new IntentFilter("chat");
        broadcastManager.registerReceiver(chatReceiver, chatFilter);
    }

    // 接收到聊天訊息會在TextView呈現
    private BroadcastReceiver chatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            ChatMessage chatMessage = new Gson().fromJson(message, ChatMessage.class);
            String sender = chatMessage.getSender();
            // 接收到聊天訊息，若發送者與目前聊天對象相同，就將訊息顯示在TextView
            if (sender.equals(friend)) {
                tvMessage.append(sender + ": " + chatMessage.getMessage() + "\n");

                /* 若訊息超過高度，自動捲動至最底部 */
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
            Log.d(TAG, message);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Fragment頁面切換時解除註冊，但不需要關閉WebSocket，
        // 否則回到前頁好友列表，會因為斷線而無法顯示好友
        broadcastManager.unregisterReceiver(chatReceiver);
    }
}
