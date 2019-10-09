package inesc_id.pt.detectp2p.Adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;

import inesc_id.pt.detectp2p.Activities.HomeActivity;
import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.TripDetection.dataML.FullTrip;
import inesc_id.pt.detectp2p.TripDetection.dataML.FullTripPart;
import inesc_id.pt.detectp2p.TripDetection.dataML.Trip;
import inesc_id.pt.detectp2p.Utils.DateHelper;
import inesc_id.pt.detectp2p.Utils.TransportInfo;
import inesc_id.pt.detectp2p.TripValidationManager.ValidatedDataManager;


public class LegValidationAdapter extends BaseAdapter {

    static class ViewHolder {
        private TextView tvStartTS;
        private TextView tvMode;
        private TextView tvDuration;
        private Spinner spinner;
        private ImageView imageView;
        private Button btEvaluate;
    }

    private FullTrip fullTripToBeValidated;
    private Context context;
    private ArrayList<FullTripPart> tripPartList;
    private Activity activity;

    FullTripPart itemBeingChanged;

    private boolean initiatingSpinner = true;

    public LegValidationAdapter(FullTrip fullTripToBeValidated, Context context, Activity activity) {
        this.fullTripToBeValidated = fullTripToBeValidated;
        this.tripPartList = fullTripToBeValidated.getTripList();
        this.context = context;
        this.activity = activity;

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

        final Trip trip = (Trip) fullTripToBeValidated.getTripList().get(i);


        if(view == null) {

            mViewHolder = new ViewHolder();

            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.leg_classified_item, null, true);

            mViewHolder.tvStartTS = view.findViewById(R.id.tvStartTS);
            mViewHolder.tvMode = view.findViewById(R.id.tvMode);
            mViewHolder.tvDuration = view.findViewById(R.id.tvDuration);
            mViewHolder.imageView = view.findViewById(R.id.checkbox);
            mViewHolder.btEvaluate = view.findViewById(R.id.btEvaluate);
            mViewHolder.btEvaluate.setTag(i);

            view.setTag(mViewHolder);
        }
        else{
            mViewHolder = (ViewHolder) view.getTag();
        }

        Log.d("LegAdapter", "Start TS: " + trip.getInitTimestamp());
        Log.d("LegAdapter", "MODE: " + trip.getSugestedModeOfTransport());

        mViewHolder.tvStartTS.setText(DateHelper.getHoursMinutesFromTSString(trip.getInitTimestamp()));
        mViewHolder.tvMode.setText(TransportInfo.codes.modalities[trip.getSugestedModeOfTransport()]);

        Long durationLong = (trip.getEndTimestamp() - trip.getInitTimestamp()) / 60000;
        BigDecimal bd = new BigDecimal(durationLong + 1);
        BigDecimal rounded = bd.setScale(1, BigDecimal.ROUND_CEILING);
        String duration =  String.valueOf(rounded.longValue());

        mViewHolder.tvDuration.setText(duration);
        mViewHolder.imageView.setColorFilter(Color.GRAY);


        int validatedMode = ValidatedDataManager.getInstance().getValidatedModeForTrip(trip);
        if(validatedMode != 0){
            mViewHolder.imageView.setColorFilter(Color.GREEN);
        }

        mViewHolder.btEvaluate.setOnClickListener(buttonListener);

        return view;
    }

    View.OnClickListener buttonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            int tripIndex = (int) view.getTag();
            Trip trip = (Trip) fullTripToBeValidated.getTripList().get(tripIndex);
            ((HomeActivity) activity).openLeg(trip);

        }
    };

    @Override
    public int getItemViewType(int position) {
        return tripPartList.get(position).getFullTripPartType();
    }

}

