package inesc_id.pt.detectp2p.Activities.Fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import inesc_id.pt.detectp2p.Activities.HomeActivity;
import inesc_id.pt.detectp2p.Adapters.TripDigestListAdapter;
import inesc_id.pt.detectp2p.R;
import inesc_id.pt.detectp2p.TripDetection.PersistentTripStorage;
import inesc_id.pt.detectp2p.TripDetection.dataML.FullTrip;
import inesc_id.pt.detectp2p.TripDetection.dataML.FullTripDigest;

public class TripsFragment extends Fragment {

    ListView tripList;

    private  PersistentTripStorage persistentTripStorage;
    private ArrayList<FullTripDigest> tripDigestList = new ArrayList<>();
    private TripDigestListAdapter tripDigestAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main, container, false);
        tripList = (ListView) view.findViewById(R.id.tripsList);

        persistentTripStorage = new PersistentTripStorage(getActivity());

        tripDigestList = persistentTripStorage.getAllFullTripDigestsObjects();

        tripDigestAdapter = new TripDigestListAdapter(getActivity(), tripDigestList);

        tripList.setAdapter(tripDigestAdapter);

        tripList.setOnItemClickListener(itemClickListener);

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

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            FullTripDigest fullTripDigest = (FullTripDigest) adapterView.getItemAtPosition(i);

            FullTrip fullTrip = persistentTripStorage.getFullTripByDate(fullTripDigest.getTripID());

            ((HomeActivity) getActivity()).openTrip(fullTrip);
        }
    };
}
