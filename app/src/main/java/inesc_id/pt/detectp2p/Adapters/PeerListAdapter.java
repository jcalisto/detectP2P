package inesc_id.pt.detectp2p.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import inesc_id.pt.detectp2p.P2PManager.Bluetooth.BluetoothPeer;
import inesc_id.pt.detectp2p.R;

public class PeerListAdapter extends BaseAdapter {

    static class ViewHolder {
        private TextView tvPeerName;
        private TextView tvState;
        private TextView tvConnectionTime;

    }

    private Context context;
    private ArrayList<BluetoothPeer> peers;



    public PeerListAdapter(ArrayList<BluetoothPeer> peers, Context context) {
        this.context = context;
        this.peers = peers;
    }


    @Override
    public int getCount() {
        return peers.size();
    }

    @Override
    public Object getItem(int i) {
        return peers.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder mViewHolder = null;

        BluetoothPeer peer = peers.get(i);

        if(view == null) {

            mViewHolder = new ViewHolder();

            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.peer_item, null, true);

            mViewHolder.tvPeerName = view.findViewById(R.id.tvPeerName);
            mViewHolder.tvState = view.findViewById(R.id.tvState);
            mViewHolder.tvConnectionTime = view.findViewById(R.id.tvConnectionTime);

            view.setTag(mViewHolder);
        }
        else{
            mViewHolder = (ViewHolder) view.getTag();
        }

        mViewHolder.tvPeerName.setText(peer.device.getName());
        mViewHolder.tvState.setText(peer.state);

        if(peer.timestamp != 0) {
            long connectionTime = System.currentTimeMillis() - peer.timestamp;
            mViewHolder.tvConnectionTime.setText(Long.toString((connectionTime / 60000)));
            mViewHolder.tvState.setTextColor(Color.GREEN);
        }
        else {
            mViewHolder.tvState.setTextColor(Color.RED);
        }

        return view;
    }


    @Override
    public int getItemViewType(int position) {
        return 0;
    }

}


