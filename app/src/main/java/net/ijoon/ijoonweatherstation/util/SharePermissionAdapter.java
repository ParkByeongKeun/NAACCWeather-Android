package net.ijoon.ijoonweatherstation.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import net.ijoon.ijoonweatherstation.R;
import java.util.List;

/**
 * Created by BG_IJOON on 2017-07-13.
 */

public class SharePermissionAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private List<String> InfoArr;
    private ViewHolder holder;
    int mPosition;
    Context context;
    String ownerEmail;
    String usrEmail;

    public SharePermissionAdapter(Context c, List<String> array, String ownerEmail, String usrEmail) {
        mInflater = LayoutInflater.from(c);
        this.context = c;
        InfoArr = array;
        this.ownerEmail = ownerEmail;
        this.usrEmail = usrEmail;
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
            v = mInflater.inflate(R.layout.myrow, null);
            holder.userEmail = v.findViewById(R.id.textView1);
            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }
        mPosition = position;
        holder.userEmail.setTextColor(context.getResources().getColor(R.color.black));
        if(InfoArr.get(position).equals(ownerEmail)) {
            holder.userEmail.setText(InfoArr.get(position) + " (owner)");
            holder.userEmail.setTextColor(context.getResources().getColor(R.color.color_blue));
            if(InfoArr.get(position).equals(usrEmail)) {
                holder.userEmail.setText("> " + InfoArr.get(position) + " (owner)");
            }
        }else if(InfoArr.get(position).equals(usrEmail)) {
            holder.userEmail.setText("> " + InfoArr.get(position));
        }else {
            holder.userEmail.setText(InfoArr.get(position));
        }
        return v;
    }

    public void addItem(String user) {
          this.InfoArr.add(user);
          notifyDataSetChanged();
    }

    public List<String> getArrayList(){
        return InfoArr;
    }

    private class ViewHolder {
        TextView userEmail;
    }

    public void clear() {
        InfoArr.clear();
        notifyDataSetChanged();
    }
}