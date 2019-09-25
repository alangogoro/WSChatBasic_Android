package idv.ron.wschatbasic_android.all;


import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import idv.ron.wschatbasic_android.R;

public class AllChatFragment extends Fragment {
    private static final String TAG = "TAG_AllChatFragment";

    private static final String SERVER_URI =
            "ws://10.0.2.2:8080/WSChatBasic_Web/AllChatServer/";

    private Activity activity;
    private MyWebSocketClient myWebSocketClient;
    private TextView tvMessage;
    private EditText etMessage;
    private Button btSend, btConnect, btDisconnect;
    private ScrollView scrollView;
    private String userName;
    private URI uri;

    /* 透過繼承 WebSocketClient 並改寫其方法
     * 完成聊天互動的設定
     *
     * ※ gradule 裡面必須要有裝 JavaWebSocket
     */
    class MyWebSocketClient extends WebSocketClient {

        MyWebSocketClient(URI serverURI) {
            // Draft_17是連接協議，就是標準的RFC 6455（JSR256）
            super(serverURI, new Draft_17());
        }


        @Override/* Client 與 Server 連線成功時呼叫 */
        public void onOpen(ServerHandshake handshakeData) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    changeConnectStatus(true);
                }
            });
            String text = String.format(Locale.getDefault(),
                    "onOpen: Http status code = %d; status message = %s",
                    handshakeData.getHttpStatus(),
                    handshakeData.getHttpStatusMessage());

            Log.d(TAG, text);
        }


        @Override/* 當有新訊息時呼叫 */
        public void onMessage(final String message) {
            Log.d(TAG, "onMessage: " + message);

            /* 接收訊息，『必須開新的執行緒去收』
             * 但是對 UI 的操作需要主執行緒執行
             * 所以此處寫 runOnUiThread 來避免錯誤 */
            //      .runOnUiThread()
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(message, JsonObject.class);
                    String userName = jsonObject.get("userName").getAsString();
                    String message = jsonObject.get("message").getAsString();
                    String text = userName + ": " + message + "\n";
                    tvMessage.append(text);
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            });

        }


        @Override/* 當連線中斷時呼叫 */
        public void onClose(int code, String reason, boolean remote) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    changeConnectStatus(false);
                }
            });
            String text = String.format(Locale.getDefault(),
                    "code = %d, reason = %s, remote = %b",
                    code, reason, remote);
            Log.d(TAG, "onClose: " + text);
        }


        @Override/* 當發生錯誤時呼叫 */
        public void onError(Exception ex) {
            Log.d(TAG, "onError: exception = " + ex.toString());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activity.setTitle(R.string.textAllChat);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_all_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvMessage = view.findViewById(R.id.tvMessage);
        etMessage = view.findViewById(R.id.etMessage);
        btSend = view.findViewById(R.id.btSend);
        btConnect = view.findViewById(R.id.btConnect);
        btDisconnect = view.findViewById(R.id.btDisconnect);
        scrollView = view.findViewById(R.id.scrollView);

        /* 從 bundle 取得 userName */
        Bundle bundle = getArguments();
        if (bundle != null) {
            userName = bundle.getString("userName");
            if (userName != null) {
                try {
                    uri = new URI(SERVER_URI + userName);
                } catch (URISyntaxException e) {
                    Log.e(TAG, e.toString());
                }
                connectServer();
            }
        }

        /* 送出聊天訊息 */
        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = etMessage.getText().toString();
                if (message.trim().isEmpty()) {
                    showToast(R.string.textMessageEmpty);
                    return;
                }

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("userName", userName);
                jsonObject.addProperty("message", message);
                /* 送出訊息（對 Server 輸出Json字串） */
                myWebSocketClient.send(jsonObject.toString());

                Log.d(TAG, "Output: " + jsonObject.toString());
            }
        });

        btConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectServer();
            }
        });

        btDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myWebSocketClient != null) {
                    myWebSocketClient.close();
                    myWebSocketClient = null;
                }
            }
        });

    }

    /* 依照連線狀況改變按鈕enable狀態 */
    private void changeConnectStatus(boolean isConnected) {
        if (isConnected) {
            btSend.setEnabled(true);
            btConnect.setEnabled(false);
            btDisconnect.setEnabled(true);
            showToast(R.string.textConnect);
        } else {
            btSend.setEnabled(false);
            btConnect.setEnabled(true);
            btDisconnect.setEnabled(false);
            showToast(R.string.textDisconnect);
        }

    }

    /*
     * 建立 WebSocket 連線
     */
    private void connectServer() {
        if (myWebSocketClient == null) {
            myWebSocketClient = new MyWebSocketClient(uri);
            myWebSocketClient.connect();
        }
    }

    /*
     * 中斷 WebSocket 連線
     */
    private void disconnectServer() {
        if (myWebSocketClient != null) {
            myWebSocketClient.close();
            myWebSocketClient = null;
        }
    }

    private void showToast(int messageId) {
        Toast.makeText(activity, messageId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectServer();
    }
}
