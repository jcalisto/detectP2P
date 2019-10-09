package inesc_id.pt.detectp2p.P2PManager;

import android.content.Context;
import android.os.Handler;

import java.util.ArrayList;

import inesc_id.pt.detectp2p.P2PManager.DataModels.ModeInfo;
import inesc_id.pt.detectp2p.TransportModeDeterminer;

public class P2PTripDetection {

    private static int TRIP_DETECTION_TIMER = 90 * 1000;

    private P2PTripDetection instance;

    private Context context;

    Handler tripDetectionHandler;

    public P2PTripDetection(){
        tripDetectionHandler = new Handler();
        tripDetectionHandler.post(tripDetectionP2p);
    }

    public synchronized  P2PTripDetection getInstance(){
        if(instance != null){
            return instance;
        }
        return new P2PTripDetection();
    }

    private Runnable tripDetectionP2p = new Runnable() {
        @Override
        public void run() {
            TransportModeDeterminer manager = TransportModeDeterminer.getInstance();
            int state = manager.getCurrentState();

            //GetLastPredictions - Returns the list of predictions from valid peers
            //GetLastPredictions - Only predictions received in the last 90 seconds.
            ArrayList<ModeInfo> lastPredictions = manager.getLastPredictions();

            switch (state) {
                case TransportModeDeterminer.possibleStates.STATIONARY:
                    if(lastPredictions.size() > 0){
                        manager.updateState(TransportModeDeterminer.possibleStates.TRAVELLING);
                    }
                case TransportModeDeterminer.possibleStates.TRAVELLING:
                    if(lastPredictions.size() == 0){
                        manager.updateState(TransportModeDeterminer.possibleStates.WAITING);
                    }
                case TransportModeDeterminer.possibleStates.WAITING:
                    if(lastPredictions.size() == 0){
                        manager.updateState(TransportModeDeterminer.possibleStates.STATIONARY);
                    }
            }
            tripDetectionHandler.postDelayed(tripDetectionP2p, 2000);
        }
    };

    public void setContext(Context context){
        this.context = context;
    }

}
