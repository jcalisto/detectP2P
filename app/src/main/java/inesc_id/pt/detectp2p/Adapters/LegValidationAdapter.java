package inesc_id.pt.detectp2p.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import inesc_id.pt.detectp2p.Activities.EvaluatedTripActivity;
import inesc_id.pt.detectp2p.Activities.MainActivity;
import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTrip;
import inesc_id.pt.detectp2p.ModeClassification.dataML.FullTripPart;
import inesc_id.pt.detectp2p.ModeClassification.dataML.Trip;
import inesc_id.pt.detectp2p.Utils.DateHelper;
import inesc_id.pt.detectp2p.Utils.TransportInfo;
import inesc_id.pt.detectp2p.ValidatedData.ValidatedDataManager;


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

    FullTripPart itemBeingChanged;

    private boolean initiatingSpinner = true;

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
        mViewHolder.tvDuration.setText(Long.toString(trip.getTimeTraveled()));
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

            Intent intent = new Intent(context, EvaluatedTripActivity.class);
            int tripIndex = (int) view.getTag();
            Trip trip = (Trip) fullTripToBeValidated.getTripList().get(tripIndex);
            intent.putExtra("TRIP", trip);
            context.startActivity(intent);

        }
    };

    @Override
    public int getItemViewType(int position) {
        return tripPartList.get(position).getFullTripPartType();
    }

}

