package inesc_id.pt.detectp2p.Activities.Fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import inesc_id.pt.detectp2p.Activities.HomeActivity;
import inesc_id.pt.detectp2p.Adapters.LegValidationAdapter;
import inesc_id.pt.detectp2p.Adapters.TripDigestListAdapter;
import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.TripDetection.PersistentTripStorage;
import inesc_id.pt.detectp2p.TripDetection.dataML.FullTrip;
import inesc_id.pt.detectp2p.TripDetection.dataML.FullTripDigest;

public class LegsFragment extends Fragment {

    FullTrip trip;
    ListView legList;

    LegValidationAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.trip_classified_popup, container, false);


        legList = (ListView) view.findViewById(R.id.legList);

        if(trip != null){
            adapter = new LegValidationAdapter(trip, getActivity(), getActivity());

            legList.setAdapter(adapter);

            legList.setOnItemClickListener(itemClickListener);
        }



        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public void setTrip(FullTrip trip){
        this.trip = trip;
    }

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        }
    };

}
