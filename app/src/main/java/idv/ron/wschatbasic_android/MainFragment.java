package idv.ron.wschatbasic_android;


import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import idv.ron.wschatbasic_android.two.CommonTwo;

public class MainFragment extends Fragment {
    private Activity activity;
    private EditText etUserName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activity.setTitle(R.string.app_name);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etUserName = view.findViewById(R.id.etUserName);
        view.findViewById(R.id.btAllChat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("userName", etUserName.getText().toString());
                Navigation.findNavController(etUserName)
                        .navigate(R.id.action_mainFragment_to_allChatFragment, bundle);
            }
        });

        view.findViewById(R.id.btTwoChat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* 存檔 UserName 到偏好設定檔
                 * 所以任何一頁都可以得到資訊 */
                CommonTwo.saveUserName(activity, etUserName.getText().toString());

                Navigation.findNavController(etUserName)
                        .navigate(R.id.action_mainFragment_to_friendsFragment);
            }
        });

    }
}
