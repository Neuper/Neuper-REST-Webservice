package at.htlle.locationservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

@RestController
class LocationController {
    private final List<Location> knownLocations = new ArrayList<>();

    public LocationController() {
        knownLocations.add(new Location("Leoben", 47.383333, 15.1));
        knownLocations.add(new Location("Bruck", 47.416667, 15.266667));
        knownLocations.add(new Location("Kapfenberg", 47.433333, 15.316667));
        knownLocations.add(new Location("Mariazell", 47.769722, 15.316667));
        knownLocations.add(new Location("Graz", 47.066667, 15.45));
        knownLocations.add(new Location("Vienna", 48.2082, 16.3738));
        knownLocations.add(new Location("Linz", 48.3064, 14.2858));
        knownLocations.add(new Location("Graz", 47.0707, 15.4395));
        knownLocations.add(new Location("Salzburg", 47.8095, 13.0550));
        knownLocations.add(new Location("Innsbruck", 47.2682, 11.3923));
        knownLocations.add(new Location("Klagenfurt", 46.6249, 14.3050));
        knownLocations.add(new Location("Villach", 46.6111, 13.8558));
        knownLocations.add(new Location("Wels", 48.1575, 14.0289));
        knownLocations.add(new Location("St. Pölten", 48.2047, 15.6256));
        knownLocations.add(new Location("Dornbirn", 47.4125, 9.7417));
        knownLocations.add(new Location("Wiener Neustadt", 47.8151, 16.2465));
        knownLocations.add(new Location("Bregenz", 47.5031, 9.7471));
        knownLocations.add(new Location("Eisenstadt", 47.8450, 16.5336));
        knownLocations.add(new Location("Leonding", 48.2606, 14.2406));
        knownLocations.add(new Location("Traun", 48.2203, 14.2333));
        knownLocations.add(new Location("Amstetten", 48.1219, 14.8747));
        knownLocations.add(new Location("Klosterneuburg", 48.3053, 16.3256));
        knownLocations.add(new Location("Schwechat", 48.1381, 16.4708));
        knownLocations.add(new Location("Ternitz", 47.7275, 16.0361));
        knownLocations.add(new Location("Baden bei Wien", 48.0069, 16.2308));
    }

    @GetMapping("/location")
    public Location getLocation(@RequestParam(value = "name") String name) {
        for (Location location : knownLocations) {
            if (location.getName().equalsIgnoreCase(name)) {
                return location;
            }
        }
        return null;
    }

    @GetMapping("/nearestlocation")
    public Location getNearestLocation(@RequestParam(value = "latitude") double latitude, @RequestParam(value = "longitude") double longitude) {
        Location currentLocation = new Location("Current Location", latitude, longitude);
        return knownLocations.stream().min(Comparator.comparingDouble(location -> location.distanceTo(currentLocation))).orElse(null);
    }

    @GetMapping("/altitude")
    public ResponseEntity<String> altitude(@RequestParam(value = "latitude") double latitude, @RequestParam(value = "longitude") double longitude) {
        File file = new File("src/main/resources/srtm_40_03.asc");

        SrtmFile srtmFile = new SrtmFile(file);

        Location nearestLocation = Collections.min(knownLocations, Comparator.comparingDouble(l -> l.distanceTo(new Location("", latitude, longitude))));
        Double azimuth = nearestLocation.directionTo(new Location("", latitude, longitude));
        String direction = "";
        if (azimuth >= 337.5 || azimuth < 22.5) {
            direction = "Nördlich";
        } else if (azimuth >= 22.5 && azimuth < 67.5) {
            direction = "Nordöstlich";
        } else if (azimuth >= 67.5 && azimuth < 112.5) {
            direction = "Östlich";
        } else if (azimuth >= 112.5 && azimuth < 157.5) {
            direction = "Südöstlich";
        } else if (azimuth >= 157.5 && azimuth < 202.5) {
            direction = "Südlich";
        } else if (azimuth >= 202.5 && azimuth < 247.5) {
            direction = "Südwestlich";
        } else if (azimuth >= 247.5 && azimuth < 292.5) {
            direction = "Westlich";
        } else if (azimuth >= 292.5 && azimuth < 337.5) {
            direction = "Nordwestlich";
        } else {
            direction = "Unbekannte Richtung";
        }
        DecimalFormat decimalFormat = new DecimalFormat("0.0");
        String name = direction + " von " + nearestLocation.getName() + " " + decimalFormat.format(nearestLocation.distanceTo(new Location("", latitude, longitude))) + " km";
        Location location = new Location(name, latitude, longitude);

        Optional<Double> altitude = srtmFile.getAltitudeForLocation(location);
        String json = "{\"loc\":{\"name\":\"" + name + "\",\"latitude\":\"" + latitude + "\",\"longitude\":\"" + longitude + "\"},\"altitude\":\"" + altitude.get() + "\"}";
        return ResponseEntity.ok().body(json);
    }

    @GetMapping("/elevationprofile")
    public ResponseEntity<List<Double>> getElevationProfileBetween2Places(@RequestParam(value = "latitude_first") double latitude_first_place, @RequestParam(value = "longitude_first") double longitude_first_place, @RequestParam(value = "latitude_second") double latitude_second_place, @RequestParam(value = "longitude_second") double longitude_second_place, @RequestParam(value = "elevationprofilepoints") int elevation_profile_points) {
        //First Location
        Location first_location = new Location("First Place",latitude_first_place, longitude_first_place);
        Location second_location = new Location("Second Place",latitude_second_place, longitude_second_place);

        List<Location> intermediateLocations = first_location.calculateIntermediatelocations(second_location, elevation_profile_points);

        List<Double> elevations = new ArrayList<>();
        for (Location intermediateLocation : intermediateLocations) {
            File file = new File("src/main/resources/srtm_40_03.asc");
            SrtmFile srtmFile = new SrtmFile(file);
            Optional<Double> altitude = srtmFile.getAltitudeForLocation(intermediateLocation); //Here is starting the 'bottleneck'
            elevations.add(altitude.get());
        }

        System.out.println(elevations);
        return ResponseEntity.ok(elevations);
    }
}