package inesc_id.pt.detectp2p.Utils;

public class TransportInfo {

    public static int getModeCodeFromString(String mode){
        for(int i = 0; i<codes.modalities.length;i++){
            String m = codes.modalities[i];
            if(m.equals(mode)){
                return i;
            }
        }
        return 9;   //Default car
    }

    public interface codes {

        String[] modalities = {"vehicle","bicycle","onfoot",
                "still","unknown","tilting",
                "inexistant","walking", "running",
                "car","train","tram",
                "subway","ferry","plane", "bus", "electricBike","bikeSharing",
                "microScooter", "skate", "motorcycle", "moped", "carPassenger",
                "taxi", "rideHailing", "carSharing", "carpooling",
                "busLongDistance", "highSpeedTrain", "other", "otherPublic",
                "otherActive", "otherPrivate", "intercity", "wheelChair", "cargoBike", "carSharingPassenger", "electricWheelchair" };

        String[] correctedModalities = {"default", "walking", "bicycle", "car", "train", "tram", "bus", "motorcycle", "subway"};



        int vehicle = 0;
        int bicycle = 1;
        int onfoot = 2;
        int still = 3;
        int unknown = 4;
        int tilting = 5;
        int inexistent = 6;
        int walking = 7;
        int running = 8;
        int car = 9;
        int train = 10;
        int tram = 11;
        int subway = 12;
        int ferry = 13;
        int plane = 14;
        int bus = 15;
        int electricBike = 16;
        int bikeSharing = 17;
        int microScooter = 18;
        int skate = 19;
        int motorcycle = 20;
        int moped = 21;
        int carPassenger = 22;
        int taxi = 23;
        int rideHailing = 24;
        int carSharing = 25;
        int carpooling = 26;
        int busLongDistance = 27;
        int highSpeedTrain = 28;
        int other = 29;
        int otherPublic = 30;
        int otherActive = 31;
        int otherPrivate = 32;
        int intercityTrain = 33;
        int wheelChair = 34;
        int cargoBike = 35;
        int carSharingPassenger = 36;
        int electricWheelchair = 37;
    }
}
