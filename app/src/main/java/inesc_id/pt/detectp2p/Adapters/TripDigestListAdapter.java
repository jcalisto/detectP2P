package inesc_id.pt.detectp2p.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;

import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.TripStateMachine.dataML.FullTripDigest;

public class TripDigestListAdapter extends BaseAdapter {

    ArrayList<FullTripDigest> tripDigestArrayList = new ArrayList<>();
    Context context;

    public TripDigestListAdapter(Context context, ArrayList<FullTripDigest> dataset){
        this.context = context;
        this.tripDigestArrayList = dataset;
    }

    @Override
    public int getCount() {
        return tripDigestArrayList.size();
    }

    @Override
    public Object getItem(int i) {
        return tripDigestArrayList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View rowView = inflater.inflate(R.layout.trip_digest_item, null, true);

        FullTripDigest data = tripDigestArrayList.get(i);

        TextView tvStartAddr = (TextView) rowView.findViewById(R.id.tvStartAddr);
        TextView tvEndAddr = (TextView) rowView.findViewById(R.id.tvEndAddr);
        TextView tvTimestamp = (TextView) rowView.findViewById(R.id.tvTimestamp);

        tvStartAddr.setText(data.getStartAddress());
        tvEndAddr.setText(data.getFinalAddress());
        tvTimestamp.setText("" + data.getInitTimestamp());

        return rowView;
    }
}
