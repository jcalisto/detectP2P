package inesc_id.pt.detectp2p.TripValidationManager;

import java.io.Serializable;

public class ValidatedCoords implements Serializable {

    double latitude;
    double longitude;
    float speed;

    public ValidatedCoords(Double latitude, Double longitude, Float speed) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
    }
}
