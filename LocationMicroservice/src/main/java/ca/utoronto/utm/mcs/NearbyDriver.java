package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONObject;
import org.json.JSONException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.util.ArrayList;
import java.util.Map;

public final class NearbyDriver implements HttpHandler {

    private static Memory memory;
    private Database db;

    public NearbyDriver(Memory mem, Database database) {
        memory = mem;
        db = database;
    }

    public void handle(HttpExchange r) {
        try {
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            } else {
                JSONObject resObject = new JSONObject();
                resObject.put("status", "METHOD DOES NOT EXIST");
                sendStatus(r, resObject, 405);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Sends appropriate status as specified by parameters statusText and statusCode */
    public void sendStatus(HttpExchange r, JSONObject resObject, int statusCode) throws IOException, JSONException {

        try {

            String resBody = resObject.toString();
            r.sendResponseHeaders(statusCode, resBody.length());

            OutputStream os = r.getResponseBody();
            os.write(resBody.getBytes());
            os.close();

        }  catch (Exception e) {
            JSONObject errorObject = new JSONObject();
            errorObject.put("status", "INTERNAL SERVER ERROR");

            String resBody = errorObject.toString();
            r.sendResponseHeaders(500, resBody.length());

            OutputStream os = r.getResponseBody();
            os.write(resBody.getBytes());
            os.close();
        }
    }

    public String[] parseURI(String uri) {

        String[] uriParams = uri.split("/", -1);

        if (uriParams.length > 3) {
            String unknownElement = uriParams[3];
            if (!unknownElement.trim().isEmpty()) {
                return new String[]{"false"};
            }
        }

        String[] query = uriParams[2].split("\\?radius=", -1);

        if (query.length != 2) {
            return new String[]{"false"};
        }

        return query;
    }

    /* Handles GET requests for Location */
    public void handleGet(HttpExchange r) throws IOException, JSONException {
        try {
            String[] query = parseURI(r.getRequestURI().toString());
            JSONObject resObject = new JSONObject();

            if (query.length < 2) {
                resObject.put("status", "IMPROPER FORMAT");
                sendStatus(r, resObject, 400);
                return;
            }

            String uid = query[0];

            if (uid.trim().isEmpty()) {
                resObject.put("status", "IMPROPER FORMAT");
                sendStatus(r, resObject, 400);
                return;
            }

            double radius = Double.parseDouble(query[1]);

            int response = db.getNearbyDriver(uid, radius * 1000);

            if (response == -1) {
                resObject.put("status", "INTERNAL SERVER ERROR");
                sendStatus(r, resObject, 500);
                return;

            } else if (response == 2) {
                resObject.put("status", "USER IS A DRIVER");
                sendStatus(r, resObject, 400);
                return;

            } else if (response == 3) {
                resObject.put("status", "NO DRIVERS WITHIN RANGE");
                JSONObject data = new JSONObject();
                resObject.put("data", data);
                sendStatus(r, resObject, 404);
                return;

            } else if (response == 1) {
                resObject.put("status", "USER DOES NOT EXIST");
                JSONObject data = new JSONObject();
                resObject.put("data", data);
                sendStatus(r, resObject, 404);
                return;

            } else {
                resObject.put("status", "OK");
                JSONObject data = new JSONObject();
                ArrayList<Map<String, Object>> nearbyDrivers = memory.getNearbyDrivers();

                for (Map<String, Object> driver : nearbyDrivers) {
                    String driverUid = driver.get("uid").toString();
                    double longitude = Double.parseDouble(driver.get("longitude").toString());
                    double latitude = Double.parseDouble(driver.get("latitude").toString());
                    String streetAt = driver.get("street_at").toString();

                    JSONObject coords = new JSONObject()
                        .put("longitude", longitude)
                        .put("latitude", latitude)
                        .put("street_at", streetAt);

                    data.put(driverUid, coords);
                }

                resObject.put("data", data);

                sendStatus(r, resObject, 200);
                return;
            }

        } catch (Exception e) {
            JSONObject resObject = new JSONObject();
            resObject.put("status", "IMPROPER FORMAT");
            sendStatus(r, resObject, 400);
            return;
        }
    }
}
