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
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTrip;
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTripPart;
import inesc_id.pt.detectp2p.ModeClassification.dataML.Trip;
import inesc_id.pt.detectp2p.Utils.DateHelper;
import inesc_id.pt.detectp2p.Utils.TransportInfo;


public class LegValidationAdapter extends BaseAdapter {

    static class ViewHolder {
        private TextView tvStartTS;
        private TextView tvMode;
    }

    private FullTrip fullTripToBeValidated;
    private Context context;
    private ArrayList<FullTripPart> tripPartList;

    FullTripPart itemBeingChanged;

    public LegValidationAdapter(FullTrip fullTripToBeValidated, Context context) {
        this.fullTripToBeValidated = fullTripToBeValidated;
        this.tripPartList = fullTripToBeValidated.getTripList();
        this.context = context;

        Log.d("LegValidationAdapter", "Trip part count = " + fullTripToBeValidated.getTripList().size());
    }


    @Override
    public int getCount() {
        return tripPartList.size();
    }

    @Override
    public Object getItem(int i) {
        return tripPartList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder mViewHolder = null;

        Trip trip = (Trip) fullTripToBeValidated.getTripList().get(i);


        if(view == null) {

            mViewHolder = new ViewHolder();

            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.leg_classified_item, null, true);

            mViewHolder.tvStartTS = view.findViewById(R.id.tvStartTS);
            mViewHolder.tvMode = view.findViewById(R.id.tvMode);

            view.setTag(mViewHolder);
        }
        else{
            mViewHolder = (ViewHolder) view.getTag();
        }

        Log.d("LegAdapter", "Start TS: " + trip.getInitTimestamp());
        Log.d("LegAdapter", "MODE: " + trip.getSugestedModeOfTransport());

        mViewHolder.tvStartTS.setText(DateHelper.getHoursMinutesFromTSString(trip.getInitTimestamp()));
        mViewHolder.tvMode.setText(TransportInfo.codes.modalities[trip.getSugestedModeOfTransport()]);

        return view;
    }

    @Override
    public int getItemViewType(int position) {
        return tripPartList.get(position).getFullTripPartType();
    }

}

