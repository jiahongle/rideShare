package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONObject;
import org.json.JSONException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public final class Route implements HttpHandler {

    private static Memory memory;
    private Database db;

    public Route(Memory mem, Database database) {
        memory = mem;
        db = database;
    }

    public void handle(HttpExchange r) {
        try {
            if (r.getRequestMethod().equals("POST")) {
                handlePost(r);
            } else if (r.getRequestMethod().equals("DELETE")) {
                handleDelete(r);
            } else {
                sendStatus(r, "METHOD DOES NOT EXIST", 405);
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Sends appropriate status as specified by parameters statusText and statusCode */
    public void sendStatus(HttpExchange r, String statusText, int statusCode) throws IOException, JSONException {

        try {
            JSONObject resObject = new JSONObject();
            resObject.put("status", statusText);

            String resBody = resObject.toString();
            r.sendResponseHeaders(statusCode, resBody.length());

            OutputStream os = r.getResponseBody();
            os.write(resBody.getBytes());
            os.close();
        }  catch (Exception e) {
            JSONObject resObject = new JSONObject();
            resObject.put("status", "INTERNAL SERVER ERROR");

            String resBody = resObject.toString();
            r.sendResponseHeaders(500, resBody.length());

            OutputStream os = r.getResponseBody();
            os.write(resBody.getBytes());
            os.close();
        }
    }

    /* Handles POST requests for Route */
    public void handlePost(HttpExchange r) throws IOException, JSONException {
        try {
            String body = Utils.convert(r.getRequestBody());
            JSONObject deserialized = new JSONObject(body);

            String roadName1;
            String roadName2;
            Boolean isTraffic;
            int time;

            if (deserialized.has("roadName1") && deserialized.has("roadName2") && deserialized.has("is_traffic") && deserialized.has("time")) {
                roadName1 = deserialized.getString("roadName1");
                roadName2 = deserialized.getString("roadName2");
                isTraffic = deserialized.getBoolean("is_traffic");
                time = deserialized.getInt("time");

                if (roadName1.trim().isEmpty() || roadName2.trim().isEmpty() || time < 0) {
                    sendStatus(r, "IMPROPER FORMAT", 400);
                    return;
                }
            } else {
                sendStatus(r, "IMPROPER FORMAT", 400);
                return;
            }

            int response = db.postRoute(roadName1, roadName2, isTraffic, time);

            if (response == -1) {
                sendStatus(r, "INTERNAL SERVER ERROR", 500);
                return;
            } else if (response == 1) {
                sendStatus(r, "ROUTE ALREADY EXISTS", 400);
                return;
            } else if (response == 2) {
                sendStatus(r, "DOES NOT EXIST", 404);
                return;
            } else {
                sendStatus(r, "OK", 200);
                return;
            }

        } catch (Exception e) {
            sendStatus(r, "IMPROPER FORMAT", 400);
            return;
        }
    }

    /* Handles DELETE requests for Route */
    public void handleDelete(HttpExchange r) throws IOException, JSONException {
        try {
            String body = Utils.convert(r.getRequestBody());
            JSONObject deserialized = new JSONObject(body);

            String roadName1;
            String roadName2;

            if (deserialized.has("roadName1") && deserialized.has("roadName2")) {
                roadName1 = deserialized.getString("roadName1");
                roadName2 = deserialized.getString("roadName2");

                if (roadName1.trim().isEmpty() || roadName2.trim().isEmpty()) {
                    sendStatus(r, "IMPROPER FORMAT", 400);
                    return;
                }
            } else {
                sendStatus(r, "IMPROPER FORMAT", 400);
                return;
            }

            int response = db.deleteRoute(roadName1, roadName2);

            if (response == -1) {
                sendStatus(r, "INTERNAL SERVER ERROR", 500);
                return;
            } else if (response == 1) {
                sendStatus(r, "ROUTE DOES NOT EXIST", 404);
                return;
            } else if (response == 2) {
                sendStatus(r, "ROAD(S) DO(ES) NOT EXIST", 404);
                return;
            } else {
                sendStatus(r, "OK", 200);
                return;
            }

        } catch (Exception e) {
            sendStatus(r, "IMPROPER FORMAT", 400);
            return;
        }
    }
}
