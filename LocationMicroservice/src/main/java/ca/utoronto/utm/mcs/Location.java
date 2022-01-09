package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONObject;
import org.json.JSONException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public final class Location implements HttpHandler {

    private static Memory memory;
    private Database db;

    public Location(Memory mem, Database database) {
        memory = mem;
        db = database;
    }

    public void handle(HttpExchange r) {
        try {
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            } else if (r.getRequestMethod().equals("PATCH")) {
                handlePatch(r);
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

    public String parseURI(String uri) {

        String[] uriParams = uri.split("/", -1);

        if (uriParams.length > 3) {
            String unknownElement = uriParams[3];
            if (!unknownElement.trim().isEmpty()) {
                return "false";
            }
        }

        return uriParams[2];

    }

    /* Handles GET requests for Location */
    public void handleGet(HttpExchange r) throws IOException, JSONException {
        try {

            String uid = parseURI(r.getRequestURI().toString());

            if (uid == "false") {
                JSONObject resObject = new JSONObject();
                resObject.put("status", "IMPROPER FORMAT");
                sendStatus(r, resObject, 400);
                return;
            }

            if (uid.trim().isEmpty()) {
                JSONObject resObject = new JSONObject();
                resObject.put("status", "IMPROPER FORMAT");
                sendStatus(r, resObject, 400);
                return;
            }

            int response = db.getLocation(uid);

            if (response == -1) {
                JSONObject resObject = new JSONObject();
                resObject.put("status", "INTERNAL SERVER ERROR");
                sendStatus(r, resObject, 500);
                return;
            } else if (response == 1) {

                JSONObject resObject = new JSONObject();
                resObject.put("status", "DOES NOT EXIST");

                JSONObject data = new JSONObject();

                resObject.put("data", data);

                sendStatus(r, resObject, 404);
                return;

            } else {

                double longitude = memory.getLongitude();
                double latitude = memory.getLatitude();
                String streetAt = memory.getStreetAt();

                JSONObject resObject = new JSONObject();
                resObject.put("status", "OK");

                JSONObject data = new JSONObject()
                    .put("longitude", longitude)
                    .put("latitude", latitude)
                    .put("street_at", streetAt);

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

    /* Handles PATCH requests for Location */
    public void handlePatch(HttpExchange r) throws IOException, JSONException {
        try {

            String uid = parseURI(r.getRequestURI().toString());

            if (uid == "false") {
                JSONObject resObject = new JSONObject();
                resObject.put("status", "IMPROPER FORMAT");
                sendStatus(r, resObject, 400);
                return;
            }
            if (uid.trim().isEmpty()) {
                JSONObject resObject = new JSONObject();
                resObject.put("status", "IMPROPER FORMAT");
                sendStatus(r, resObject, 400);
                return;
            }

            String body = Utils.convert(r.getRequestBody());
            JSONObject deserialized = new JSONObject(body);

            double longitude;
            double latitude;
            String streetAt;

            if (deserialized.has("longitude") && deserialized.has("latitude") && deserialized.has("street_at")) {
                longitude = deserialized.getDouble("longitude");
                latitude = deserialized.getDouble("latitude");
                streetAt = deserialized.getString("street_at");

                if (streetAt.trim().isEmpty()) {
                    JSONObject resObject = new JSONObject();
                    resObject.put("status", "IMPROPER FORMAT");
                    sendStatus(r, resObject, 400);
                    return;
                }

            } else {
                JSONObject resObject = new JSONObject();
                resObject.put("status", "IMPROPER FORMAT");
                sendStatus(r, resObject, 400);
                return;
            }

            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                JSONObject resObject = new JSONObject();
                resObject.put("status", "IMPROPER FORMAT");
                sendStatus(r, resObject, 400);
                return;
            }

            int response = db.updateLocation(uid, longitude, latitude, streetAt);

            if (response == -1) {
                JSONObject resObject = new JSONObject();
                resObject.put("status", "INTERNAL SERVER ERROR");
                sendStatus(r, resObject, 500);
                return;
            } else if (response == 1) {

                JSONObject resObject = new JSONObject();
                resObject.put("status", "DOES NOT EXIST");
                sendStatus(r, resObject, 404);
                return;

            } else {
                JSONObject resObject = new JSONObject();
                resObject.put("status", "OK");
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
