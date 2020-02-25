package net.ijoon.ijoonweatherstation.util;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.ijoon.ijoonweatherstation.R;

import java.util.ArrayList;

/**
 * Created by BG_IJOON on 2017-07-13.
 */

public class UserListAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private ArrayList<UserList> InfoArr;
    private ViewHolder holder;
    int mPosition;
    Context context;

    public UserListAdapter(Context c, ArrayList<UserList> array) {
        mInflater = LayoutInflater.from(c);
        this.context = c;
        InfoArr = array;
    }

    @Override
    public int getCount() {
        return InfoArr.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            holder = new ViewHolder();
            v = mInflater.inflate(R.layout.item_user_list, null);
            holder.userEmail = v.findViewById(R.id.tvEmail);
            holder.userName = v.findViewById(R.id.tvName);
            holder.en = v.findViewById(R.id.en);
            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }
        mPosition = position;
//        Log.d("yot132","InfoArr.get(position).getId() = " + InfoArr.get(position).getId());
        if(holder.userEmail.getText().toString().equals("") && InfoArr.size() == 1) {
            holder.en.setText("");
        }else {
            holder.en.setText("@");
        }
        holder.userEmail.setText(InfoArr.get(position).getId());
        holder.userName.setText(InfoArr.get(position).getName());

        return v;
    }

    public void addItem(UserList userList) {

        UserList item = new UserList();
        item.setId(userList.getId());
        item.setName(userList.getName());
          this.InfoArr.add(item);
          notifyDataSetChanged();
    }

    public ArrayList<UserList> getArrayList(){
        return InfoArr;
    }

    private class ViewHolder {
        TextView userEmail;
        TextView userName;
        TextView en;
    }

    public void clear() {
        InfoArr.clear();
        notifyDataSetChanged();
    }

}