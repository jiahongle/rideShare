package ca.utoronto.utm.mcs;

import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URL;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.net.MalformedURLException;
import org.json.JSONException;

import java.lang.reflect.Field;
/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */

     // Allows PATCH requests
     public void setRequestMethod(final HttpURLConnection conn, final String value) {
        try {

            final Object target = conn;
            final Field f = HttpURLConnection.class.getDeclaredField("method");
            f.setAccessible(true);
            f.set(target, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new AssertionError(e);
        }
     }

    /* POST tests */
    public int getResponseStatus(URL url, String method, String body) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)url.openConnection();

            if (method != "GET") {
                connection.setDoOutput(true);
                connection.setRequestProperty("Accept", "application/json");
                if (method == "POST" || method == "DELETE" || method == "PUT") {
                    connection.setRequestMethod(method);
                } else if (method == "PATCH") {
                    setRequestMethod(connection, "PATCH");
                }
                OutputStream os = connection.getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(os, "utf-8");
                writer.write(body);
                writer.flush();
                writer.close();
                os.close();
            } else {
                connection.setRequestMethod(method);
            }
            connection.connect();

            return connection.getResponseCode();
        } catch (Exception e) {
            return -1;
        } finally {
            if (connection != null)
            connection.disconnect();
        }
    }

    @Test
    public void testUserPutOK() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("uid", "0001")
            .put("is_driver", false);

        int status = getResponseStatus(
            new URL("http://localhost:8000/user"),
            "PUT",
            reqBody.toString());
        assert(status == 200);
    }

    @Test
    public void testUserPutUpdate() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("uid", "0002")
            .put("is_driver", false);

        int status = getResponseStatus(
            new URL("http://localhost:8000/user"),
            "PUT",
            reqBody.toString());

        if (status == 200) {
            reqBody = new JSONObject();
            reqBody.put("uid", "0002");
            reqBody.put("is_driver", true);

            status = getResponseStatus(
                new URL("http://localhost:8000/user"),
                "PUT",
                reqBody.toString());

            assertTrue(status == 200);
            return;
        }
        assertTrue(false);
    }

    @Test
    public void testUserPutImproperFormat() throws MalformedURLException, JSONException {

        JSONObject reqBody = new JSONObject();
        reqBody.put("uid", "0004");
        reqBody.put("is_driver", 213);

        int status = getResponseStatus(
            new URL("http://localhost:8000/user"),
            "PUT",
            reqBody.toString());

        assertTrue(status == 400);
    }

    /* DELETE tests */
    @Test
    public void testUserDeleteOK() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("uid", "0003")
            .put("is_driver", false);

        int status = getResponseStatus(
            new URL("http://localhost:8000/user"),
            "PUT",
            reqBody.toString());

        if (status == 200) {
            reqBody = new JSONObject();
            reqBody.put("uid", "0003");

            status = getResponseStatus(
                new URL("http://localhost:8000/user"),
                "DELETE",
                reqBody.toString());

            assertTrue(status == 200);
            return;
        }

        assert(false);
    }

    @Test
    public void testUserDeleteNotExists() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("uid", "0005");

        int status = getResponseStatus(
            new URL("http://localhost:8000/user"),
            "DELETE",
            reqBody.toString());

        assertTrue(status == 404);

    }

    @Test
    public void testUserDeleteImproperFormat() throws MalformedURLException, JSONException {

        JSONObject reqBody = new JSONObject()
            .put("id", 1231);

        int status = getResponseStatus(
            new URL("http://localhost:8000/user"),
            "DELETE",
            reqBody.toString());

        assertTrue(status == 400);
    }

    @Test
    public void testGetLocationOK() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("uid", "0006")
            .put("is_driver", false);

        int status = getResponseStatus(
            new URL("http://localhost:8000/user"),
            "PUT",
            reqBody.toString());

        if (status == 200) {
            status = getResponseStatus(
                new URL("http://localhost:8000/location/0006/"),
                "GET",
                null
            );
            assertTrue(status == 200);
            return;
        }
        assertTrue(false);
    }

    // @Test
    // public void testGetLocationDoesNotExist() throws MalformedURLException {
    //     int status = getResponseStatus(
    //         new URL("http://localhost:8000/location/1001/"),
    //         "GET",
    //         null
    //     );
    //     assertTrue(status == 404);
    // }

    @Test
    public void testGetLocationImproperFormat() throws MalformedURLException {
        int status = getResponseStatus(
            new URL("http://localhost:8000/location/abc/def"),
            "GET",
            null
        );
        assertTrue(status == 400);
    }

    @Test
    public void testGetLocationImproperFormat2() throws MalformedURLException {
        int status = getResponseStatus(
            new URL("http://localhost:8000/location/abc/de4f"),
            "GET",
            null
        );
        assertTrue(status == 400);
    }

    @Test
    public void testUpdateLocationOK() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("uid", "0008")
            .put("is_driver", false);

        int status = getResponseStatus(
            new URL("http://localhost:8000/user"),
            "PUT",
            reqBody.toString());

        if (status == 200) {

            reqBody = new JSONObject()
                .put("longitude", 79.001)
                .put("latitude", 70.001)
                .put("street_at", "Queen St.");

            status = getResponseStatus(
                new URL("http://localhost:8000/location/0008"),
                "PATCH",
                reqBody.toString()
            );

            assertTrue(status == 200);
            return;
        }
        assertTrue(false);
    }

    @Test
    public void testUpdateLocationNotExists() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("longitude", 79.001)
            .put("latitude", 70.001)
            .put("street_at", "Queen St.");

        int status = getResponseStatus(
            new URL("http://localhost:8000/location/0009"),
            "PATCH",
            reqBody.toString()
        );
        assertTrue(status == 404);
    }

    @Test
    public void testPatchLocationImproperFormat() throws MalformedURLException {
        int status = getResponseStatus(
            new URL("http://localhost:8000/location/abc/def"),
            "PATCH",
            new JSONObject().toString()
        );
        assertTrue(status == 400);
    }

    @Test
    public void testGetNearbyDriverOK() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
        .put("uid", "0010")
        .put("is_driver", false);

        int status = getResponseStatus(
            new URL("http://localhost:8000/user"),
            "PUT",
            reqBody.toString());

        assertTrue(status == 200);

        reqBody = new JSONObject()
        .put("uid", "0011")
        .put("is_driver", true);

        status = getResponseStatus(
            new URL("http://localhost:8000/user"),
            "PUT",
            reqBody.toString());

        assertTrue(status == 200);

        reqBody = new JSONObject()
        .put("uid", "0012")
        .put("is_driver", true);

        status = getResponseStatus(
            new URL("http://localhost:8000/user"),
            "PUT",
            reqBody.toString());

        assertTrue(status == 200);

        status = getResponseStatus(
            new URL("http://localhost:8000/NearbyDriver/0010?radius=50"),
            "GET",
            null
        );

        assertTrue(status == 200);
    }

    @Test
    public void testGetNearbyDriverNotExists() throws MalformedURLException, JSONException {

        int status = getResponseStatus(
            new URL("http://localhost:8000/NearbyDriver/0013?radius=50"),
            "GET",
            null
        );

        assertTrue(status == 404);
    }

    @Test
    public void testGetNearbyDriverImproperFormat() throws MalformedURLException {

        int status = getResponseStatus(
            new URL("http://localhost:8000/NearbyDriver/0013///"),
            "GET",
            null
        );

        assertTrue(status == 400);
    }

    @Test
    public void testPutRoadOK() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("RoadName", "Random Rd.")
            .put("HasTraffic", false);

        int status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());
        assert(status == 200);
    }

    @Test
    public void testPutRoadUpdate() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("RoadName", "Queen St.")
            .put("HasTraffic", false);

        int status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        if (status == 200) {
            reqBody = new JSONObject()
                .put("RoadName", "Queen St.")
                .put("HasTraffic", true);

            status = getResponseStatus(
                new URL("http://localhost:8000/Road"),
                "PUT",
                reqBody.toString());

            assertTrue(status == 200);
            return;
        }
        assertTrue(false);
    }

    @Test
    public void testPutRoadImproperFormat() throws MalformedURLException, JSONException {

        JSONObject reqBody = new JSONObject()
            .put("RoadName", 123123)
            .put("HasTraffic", "sdasdasd");

        int status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assertTrue(status == 400);
    }

    @Test
    public void testPostRouteOK() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 1")
            .put("HasTraffic", false);

        int status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 2")
            .put("HasTraffic", false);

        status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("roadName1", "Random Rd. 1")
            .put("roadName2", "Random Rd. 2")
            .put("is_traffic", false)
            .put("time", 60);

        status = getResponseStatus(
            new URL("http://localhost:8000/has_route"),
            "POST",
            reqBody.toString());

        assert(status == 200);
    }

    @Test
    public void testPostRouteExists() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 3")
            .put("HasTraffic", false);

        int status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 4")
            .put("HasTraffic", false);

        status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("roadName1", "Random Rd. 3")
            .put("roadName2", "Random Rd. 4")
            .put("is_traffic", false)
            .put("time", 60);

        status = getResponseStatus(
            new URL("http://localhost:8000/has_route"),
            "POST",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("roadName1", "Random Rd. 3")
            .put("roadName2", "Random Rd. 4")
            .put("is_traffic", false)
            .put("time", 60);

        status = getResponseStatus(
            new URL("http://localhost:8000/has_route"),
            "POST",
            reqBody.toString());

        assert(status == 400);
    }

    @Test
    public void testPostRouteRoadNotExists() throws MalformedURLException, JSONException {

        JSONObject reqBody = new JSONObject()
            .put("roadName1", "Random Rd. 5")
            .put("roadName2", "Random Rd. 6")
            .put("is_traffic", false)
            .put("time", 60);

        int status = getResponseStatus(
            new URL("http://localhost:8000/has_route"),
            "POST",
            reqBody.toString());

        assert(status == 404);
    }

    @Test
    public void testPostRouteImproperFormat() throws MalformedURLException, JSONException {

        JSONObject reqBody = new JSONObject()
            .put("roadName1", 2213)
            .put("roadName2", 123123)
            .put("is_traffic", 213123)
            .put("time", 60);

        int status = getResponseStatus(
            new URL("http://localhost:8000/has_route"),
            "POST",
            reqBody.toString());

        assert(status == 400);
    }

    @Test
    public void testDeleteRouteOK() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 7")
            .put("HasTraffic", true);

        int status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 8")
            .put("HasTraffic", false);

        status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("roadName1", "Random Rd. 7")
            .put("roadName2", "Random Rd. 8")
            .put("is_traffic", false)
            .put("time", 15);

        status = getResponseStatus(
            new URL("http://localhost:8000/has_route"),
            "POST",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("roadName1", "Random Rd. 7")
            .put("roadName2", "Random Rd. 8");

        status = getResponseStatus(
            new URL("http://localhost:8000/route"),
            "DELETE",
            reqBody.toString());

        assert(status == 200);
    }

    @Test
    public void testDeleteRouteNotExists() throws MalformedURLException, JSONException {

        JSONObject reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 9")
            .put("HasTraffic", true);

        int status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 10")
            .put("HasTraffic", false);

        status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("roadName1", "Random Rd. 9")
            .put("roadName2", "Random Rd. 10");

        status = getResponseStatus(
            new URL("http://localhost:8000/route"),
            "DELETE",
            reqBody.toString());

        assert(status == 404);
    }

    @Test
    public void testDELETERouteRoadNotExists() throws MalformedURLException, JSONException {

        JSONObject reqBody = new JSONObject()
            .put("roadName1", "stuff")
            .put("roadName2", "hey");

        int status = getResponseStatus(
            new URL("http://localhost:8000/route"),
            "DELETE",
            reqBody.toString());

        assert(status == 404);
    }

    @Test
    public void testDeleteRouteImproperFormat() throws MalformedURLException, JSONException {

        JSONObject reqBody = new JSONObject()
            .put("roadName1", 2213);

        int status = getResponseStatus(
            new URL("http://localhost:8000/has_route"),
            "POST",
            reqBody.toString());

        assert(status == 400);
    }

    @Test
    public void testPostNavigationOK() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("uid", "0020")
            .put("is_driver", true);

        int status = getResponseStatus(
        new URL("http://localhost:8000/user"),
            "PUT",
            reqBody.toString());

        assertTrue(status == 200);

        reqBody = new JSONObject()
            .put("longitude", 79.001)
            .put("latitude", 70.001)
            .put("street_at", "Random Rd. 11");

        status = getResponseStatus(
            new URL("http://localhost:8000/location/0020"),
            "PATCH",
            reqBody.toString()
        );

        assertTrue(status == 200);

        reqBody = new JSONObject()
            .put("uid", "0021")
            .put("is_driver", false);

        status = getResponseStatus(
            new URL("http://localhost:8000/user"),
            "PUT",
            reqBody.toString()
        );

        assertTrue(status == 200);

        reqBody = new JSONObject()
            .put("longitude", 79.001)
            .put("latitude", 70.001)
            .put("street_at", "Random Rd. 12");

        status = getResponseStatus(
            new URL("http://localhost:8000/location/0021"),
            "PATCH",
            reqBody.toString()
        );

        assertTrue(status == 200);

        reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 11")
            .put("HasTraffic", false);

        status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 12")
            .put("HasTraffic", false);

        status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 13")
            .put("HasTraffic", false);

        status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 14")
            .put("HasTraffic", false);

        status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 15")
            .put("HasTraffic", false);

        status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("RoadName", "Random Rd. 16")
            .put("HasTraffic", false);

        status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("roadName1", "Random Rd. 11")
            .put("roadName2", "Random Rd. 12")
            .put("is_traffic", false)
            .put("time", 60);

        status = getResponseStatus(
            new URL("http://localhost:8000/has_route"),
            "POST",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("roadName1", "Random Rd. 11")
            .put("roadName2", "Random Rd. 13")
            .put("is_traffic", false)
            .put("time", 2);

        status = getResponseStatus(
            new URL("http://localhost:8000/has_route"),
            "POST",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("roadName1", "Random Rd. 13")
            .put("roadName2", "Random Rd. 14")
            .put("is_traffic", false)
            .put("time", 2);

        status = getResponseStatus(
            new URL("http://localhost:8000/has_route"),
            "POST",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("roadName1", "Random Rd. 14")
            .put("roadName2", "Random Rd. 15")
            .put("is_traffic", false)
            .put("time", 2);

        status = getResponseStatus(
            new URL("http://localhost:8000/has_route"),
            "POST",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("roadName1", "Random Rd. 15")
            .put("roadName2", "Random Rd. 16")
            .put("is_traffic", false)
            .put("time", 2);

        status = getResponseStatus(
            new URL("http://localhost:8000/has_route"),
            "POST",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("roadName1", "Random Rd. 16")
            .put("roadName2", "Random Rd. 12")
            .put("is_traffic", false)
            .put("time", 2);

        status = getResponseStatus(
            new URL("http://localhost:8000/has_route"),
            "POST",
            reqBody.toString());

        assert(status == 200);

        reqBody = new JSONObject()
            .put("driveruid", "0020")
            .put("passengeruid", "0021");

        status = getResponseStatus(
            new URL("http://localhost:8000/Navigation"),
            "POST",
            reqBody.toString());

        assert(status == 200);
    }

    @Test
    public void testPostNavigationDriverNotExists() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("driveruid", "0023")
            .put("passengeruid", "0024");

        int status = getResponseStatus(
        new URL("http://localhost:8000/Navigation"),
            "POST",
            reqBody.toString());

        assert(status == 404);
    }

    @Test
    public void testPostNavigationRouteNotExists() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("uid", "0025")
            .put("is_driver", true);

        int status = getResponseStatus(
        new URL("http://localhost:8000/user"),
            "PUT",
            reqBody.toString());

        assertTrue(status == 200);

        reqBody = new JSONObject()
            .put("longitude", 79.001)
            .put("latitude", 70.001)
            .put("street_at", "Road 17");

        status = getResponseStatus(
            new URL("http://localhost:8000/location/0025"),
            "PATCH",
            reqBody.toString()
        );

        assertTrue(status == 200);

        reqBody = new JSONObject()
            .put("uid", "0026")
            .put("is_driver", false);

        status = getResponseStatus(
            new URL("http://localhost:8000/user"),
            "PUT",
            reqBody.toString()
        );

        assertTrue(status == 200);

        reqBody = new JSONObject()
            .put("longitude", 79.001)
            .put("latitude", 70.001)
            .put("street_at", "Road 18");

        status = getResponseStatus(
            new URL("http://localhost:8000/location/0026"),
            "PATCH",
            reqBody.toString()
        );

        assertTrue(status == 200);

        reqBody = new JSONObject()
            .put("RoadName", "Road 17")
            .put("HasTraffic", true);

        status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString()
        );

        assertTrue(status == 200);

        reqBody = new JSONObject()
            .put("RoadName", "Road 18")
            .put("HasTraffic", false);

        status = getResponseStatus(
            new URL("http://localhost:8000/Road"),
            "PUT",
            reqBody.toString()
        );

        assertTrue(status == 200);

        reqBody = new JSONObject()
            .put("driveruid", "0025")
            .put("passengeruid", "0026");

        status = getResponseStatus(
            new URL("http://localhost:8000/Navigation"),
            "POST",
            reqBody.toString());

        assert(status == 404);
    }

    @Test
    public void testPostNavigationImproperFormat() throws MalformedURLException, JSONException {
        JSONObject reqBody = new JSONObject()
            .put("driveruid", 324234);

        int status = getResponseStatus(
        new URL("http://localhost:8000/Navigation"),
            "POST",
            reqBody.toString());

        assert(status == 400);
    }
}
