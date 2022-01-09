package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public final class Navigation implements HttpHandler {

    private static Memory memory;
    private Database db;

    public Navigation(Memory mem, Database database) {
        memory = mem;
        db = database;
    }

    public void handle(HttpExchange r) {
        try {
            if (r.getRequestMethod().equals("POST")) {
                handlePost(r);
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

    /* Handles POST requests for Navigation */
    public void handlePost(HttpExchange r) throws IOException, JSONException {
        try {
            String body = Utils.convert(r.getRequestBody());
            JSONObject deserialized = new JSONObject(body);

            String driverUid;
            String passengerUid;

            if (deserialized.has("driveruid") && deserialized.has("passengeruid")) {
                driverUid = deserialized.getString("driveruid");
                passengerUid = deserialized.getString("passengeruid");

                if (driverUid.trim().isEmpty() || passengerUid.trim().isEmpty()) {
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

            int response = db.postNavigation(driverUid, passengerUid);

            if (response == -1) {
                JSONObject resObject = new JSONObject();
                resObject.put("status", "INTERNAL SERVER ERROR");
                sendStatus(r, resObject, 500);
                return;
            } else if (response == 1) {

                JSONObject resObject = new JSONObject();
                resObject.put("status", "DRIVER AND/OR PASSENGER DOES NOT EXIST");

                JSONObject data = new JSONObject();

                resObject.put("Data", data);

                sendStatus(r, resObject, 404);
                return;

            } else if (response == 2) {

                JSONObject resObject = new JSONObject();
                resObject.put("status", "ROUTE DOES NOT EXIST");

                JSONObject data = new JSONObject();

                resObject.put("Data", data);

                sendStatus(r, resObject, 404);
                return;

            } else {

                JSONObject resObject = new JSONObject();
                resObject.put("status", "OK");

                JSONObject data = new JSONObject();
                ArrayList<Map<String, Object>> navigation = memory.getNavigation();
                int totalTime = memory.getTotalNavigationTime();
                JSONArray routeArray = new JSONArray();

                for (Map<String, Object> road : navigation) {
                    String roadName = road.get("road_name").toString();
                    int time = Integer.parseInt(road.get("time").toString());
                    Boolean isTraffic = Boolean.parseBoolean(road.get("is_traffic").toString());

                    JSONObject roadInfo = new JSONObject()
                        .put("street", roadName)
                        .put("time", time)
                        .put("is_traffic", isTraffic);

                    routeArray.put(roadInfo);
                }

                data.put("total_time", totalTime);
                data.put("route", routeArray);
                resObject.put("Data", data);

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
