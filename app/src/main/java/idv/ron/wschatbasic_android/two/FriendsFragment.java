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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import idv.ron.wschatbasic_android.R;

import static idv.ron.wschatbasic_android.two.CommonTwo.loadUserName;
import static idv.ron.wschatbasic_android.two.CommonTwo.showToast;

/*
 * 將web socket server上的所有user以RecyclerView列出。
 * 當server上的user連線或斷線時，ChatWebSocketClient都會發LocalBroadcast，
 * 此頁的BroadcastReceiver會接收到並在RecyclerView呈現。
 * */
public class FriendsFragment extends Fragment {
    private static final String TAG = "TAG_FriendsFragment";
    private Activity activity;
    private RecyclerView rvFriends;
    private String user;
    private List<String> friendList;
    private LocalBroadcastManager broadcastManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        if (activity == null) {
            return;
        }
        // 初始化聊天清單
        friendList = new ArrayList<>();
        // 取得user name
        user = loadUserName(activity);
        // 初始化LocalBroadcastManager並註冊BroadcastReceiver
        broadcastManager = LocalBroadcastManager.getInstance(activity);
        registerFriendStateReceiver();

        /* 開始連線工作 */
        CommonTwo.connectServer(activity, user);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 將標題設定成user name
        activity.setTitle("Friends (I am " + user + ")");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 初始化RecyclerView
        rvFriends = view.findViewById(R.id.rvFriends);
        rvFriends.setLayoutManager(new LinearLayoutManager(activity));
        rvFriends.setAdapter(new FriendsFragment.FriendAdapter(activity));
    }

    // 攔截user連線或斷線的Broadcast
    private void registerFriendStateReceiver() {
        IntentFilter openFilter = new IntentFilter("open");  // 有人連線
        IntentFilter closeFilter = new IntentFilter("close");// 有人離線
        broadcastManager.registerReceiver(friendStateReceiver, openFilter);
        broadcastManager.registerReceiver(friendStateReceiver, closeFilter);
    }

    // 攔截user連線或斷線的Broadcast，並在RecyclerView呈現
    private BroadcastReceiver friendStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            /* 取得訊息內容 */
            String message = intent.getStringExtra("message");
            StateMessage stateMessage = new Gson().fromJson(message, StateMessage.class);
            String type = stateMessage.getType();
            String friend = stateMessage.getUser();
            switch (type) {
                // 有user連線
                case "open":
                    // 上線的是好友而非自己就顯示該好友 UserName
                    if (!friend.equals(user)) {
                        showToast(activity, friend + " is online");
                    }
                    break;
                // 有user斷線
                case "close":
                    // 斷線的是好友而非自己就顯示該好友 UserName
                    if (!friend.equals(user)) {
                        showToast(activity, friend + " is offline");
                    }
                    break;
            }
            // 取得 Server 上的所有 user（此處不分好友，只要在線上都算）
            friendList = new ArrayList<>(stateMessage.getUsers());
            // 將自己從聊天清單中移除，否則會看到自己在聊天清單上
            friendList.remove(user);

            // 重刷好友清單
            if (rvFriends.getAdapter() != null) {
                rvFriends.getAdapter().notifyDataSetChanged();
            }
            Log.d(TAG, message);
        }
    };

    private class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {
        Context context;

        FriendAdapter(Context context) {
            this.context = context;
        }

        class FriendViewHolder extends RecyclerView.ViewHolder {
            TextView tvFriendName;

            FriendViewHolder(View itemView) {
                super(itemView);
                tvFriendName = itemView.findViewById(R.id.tvFrinedName);
            }
        }

        @Override
        public int getItemCount() {
            return friendList.size();
        }

        @NonNull
        @Override
        public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View itemView = layoutInflater.inflate(R.layout.friend_item, parent, false);
            return new FriendViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
            final String friend = friendList.get(position);
            holder.tvFriendName.setText(friend);
            // 點選聊天清單上的user即開啟聊天頁面
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle bundle = new Bundle();
                    bundle.putString("friend", friend);
                    Navigation.findNavController(rvFriends)
                            .navigate(R.id.action_friendsFragment_to_chatFragment, bundle);
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Fragment頁面切換時解除註冊，同時關閉WebSocket
        broadcastManager.unregisterReceiver(friendStateReceiver);
        CommonTwo.disconnectServer();
    }
}
