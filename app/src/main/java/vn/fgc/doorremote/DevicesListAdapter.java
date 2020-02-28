package vn.fgc.doorremote;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DevicesListAdapter extends BaseAdapter {
    private static LayoutInflater inflater = null;
    private Context context;
    private List<ListDataWrapper> data;
    private OnListItemButtonClickListener onInfoClickListener, onDeleteClickListener;
    private boolean withButtons;

    public DevicesListAdapter(Context context, @Nullable List<ListDataWrapper> data, boolean withButtons) {
        if (data != null) {
            this.data = new ArrayList<>(data);
        } else {
            this.data = new ArrayList<>();
        }
        this.context = context;
        this.withButtons = withButtons;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    private boolean contains(ListDataWrapper wrapper) {
        for (ListDataWrapper item : data) {
            if (item.sameAs(wrapper)) {
                return true;
            }
        }
        return false;
    }

    public void addDevice(ListDataWrapper wrapper) {
        if (!contains(wrapper)) {
            this.data.add(wrapper);
        }
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public ListDataWrapper getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null)
            view = inflater.inflate(R.layout.layout_list_item, null);
        ListDataWrapper device = data.get(position);
        ((TextView) view.findViewById(R.id.text_device_name)).setText(device.getName());
        ((TextView) view.findViewById(R.id.text_device_mac)).setText(device.getAddress());

        if (device.isRecognized()) {
            ((ImageView) view.findViewById(R.id.image_device_icon)).setImageResource(R.drawable.ic_smart_helmet);
            view.findViewById(R.id.button_info).setVisibility(View.VISIBLE);
        } else {
            ((ImageView) view.findViewById(R.id.image_device_icon)).setImageResource(R.drawable.ic_devices);
            view.findViewById(R.id.button_info).setVisibility(View.GONE);
        }
        view.findViewById(R.id.button_delete).setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onEvent(device);
            }
        });
        view.findViewById(R.id.button_info).setOnClickListener(v -> {
            if (onInfoClickListener != null) {
                onInfoClickListener.onEvent(device);
            }
        });
        if (!withButtons) {
            view.findViewById(R.id.button_info).setVisibility(View.GONE);
            view.findViewById(R.id.button_delete).setVisibility(View.GONE);
        }
        return view;
    }

    List<ListDataWrapper> getData() {
        return this.data;
    }

    void setData(List<ListDataWrapper> data) {
        this.data = data;
    }

    public void clear() {
        this.data.clear();
    }

    public void setOnInfoClickListener(OnListItemButtonClickListener onInfoClickListener) {
        this.onInfoClickListener = onInfoClickListener;
    }

    public void setOnDeleteClickListener(OnListItemButtonClickListener onDeleteClickListener) {
        this.onDeleteClickListener = onDeleteClickListener;
    }
}
