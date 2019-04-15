package inesc_id.pt.detectp2p.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTripDigest;
import inesc_id.pt.detectp2p.Utils.DateHelper;
import inesc_id.pt.detectp2p.Utils.LocationUtils;

public class TripDigestListAdapter extends BaseAdapter {

    ArrayList<FullTripDigest> tripDigestArrayList = new ArrayList<>();
    Context context;

    static class ViewHolder {

        private TextView tvStartAddr;
        private TextView tvEndAddr;
        private TextView tvStartTS;
        private TextView tvEndTS;

    }

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

        ViewHolder mViewHolder = null;
        FullTripDigest data = tripDigestArrayList.get(i);

        if(view == null) {

            mViewHolder = new ViewHolder();

            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.trip_digest_item, null, true);

            mViewHolder.tvStartAddr = view.findViewById(R.id.tvStartAddr);
            mViewHolder.tvEndAddr = view.findViewById(R.id.tvEndAddr);
            mViewHolder.tvStartTS = view.findViewById(R.id.tvStartTS);
            mViewHolder.tvEndTS = view.findViewById(R.id.tvEndTS);

            view.setTag(mViewHolder);
        }
        else{
            mViewHolder = (ViewHolder) view.getTag();
        }

        // GET START/END ADDRESSES / LOCATION
        if (data.getStartAddress() != null){
            mViewHolder.tvStartAddr.setText(data.getStartAddress());
        }else{
            mViewHolder.tvStartAddr.setText(LocationUtils.getTextLatLng(data.getStartLocation()));
        }

        if (data.getFinalAddress() != null){
            mViewHolder.tvEndAddr.setText(data.getFinalAddress());
        }else{
            mViewHolder.tvEndAddr.setText(LocationUtils.getTextLatLng(data.getFinalLocation()));
        }

        mViewHolder.tvStartTS.setText(DateHelper.getHoursMinutesFromTSString(data.getInitTimestamp()));
        mViewHolder.tvEndTS.setText(DateHelper.getHoursMinutesFromTSString(data.getEndTimestamp()));

        Log.d("TripAdapter", "Start addr" + data.getStartAddress());
        Log.d("TripAdapter", "End addr" + data.getFinalAddress());
        Log.d("TripAdapter", "Start TS" + data.getEndTimestamp());
        Log.d("TripAdapter", "End TS" + data.getEndTimestamp());

        return view;
    }
}
