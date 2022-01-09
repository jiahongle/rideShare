package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONObject;
import org.json.JSONException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public final class User implements HttpHandler {

    private static Memory memory;
    private Database db;

    public User(Memory mem, Database database) {
        memory = mem;
        db = database;
    }

    public void handle(HttpExchange r) {
        try {
            if (r.getRequestMethod().equals("PUT")) {
                handlePut(r);
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

    /* Handles POST requests for User */
    public void handlePut(HttpExchange r) throws IOException, JSONException {
        try {
            String body = Utils.convert(r.getRequestBody());
            JSONObject deserialized = new JSONObject(body);

            String uid;
            Boolean isDriver;

            if (deserialized.has("uid") && deserialized.has("is_driver")) {
                uid = deserialized.getString("uid");
                isDriver = deserialized.getBoolean("is_driver");

                if (uid.trim().isEmpty()) {
                    sendStatus(r, "IMPROPER FORMAT", 400);
                    return;
                }
            } else {
                sendStatus(r, "IMPROPER FORMAT", 400);
                return;
            }

            int response = db.putUser(uid, isDriver);
            if (response == -1) {
                sendStatus(r, "INTERNAL SERVER ERROR", 500);
                return;
            } else if (response == 1) {
                sendStatus(r, "MODIFIED", 200);
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

    /* Handle DELETE requests for User */
    public void handleDelete(HttpExchange r) throws IOException, JSONException {
        try {
            String body = Utils.convert(r.getRequestBody());
            JSONObject deserialized = new JSONObject(body);

            String uid;

            if (deserialized.has("uid")) {
                uid = deserialized.getString("uid");

                if (uid.trim().isEmpty()) {
                    sendStatus(r, "IMPROPER FORMAT", 400);
                    return;
                }
            } else {
                sendStatus(r, "IMPROPER FORMAT", 400);
                return;
            }

            int response = db.deleteUser(uid);
            if (response == -1) {
                sendStatus(r, "INTERNAL SERVER ERROR", 500);
                return;
            } else if (response == 1) {
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

}
