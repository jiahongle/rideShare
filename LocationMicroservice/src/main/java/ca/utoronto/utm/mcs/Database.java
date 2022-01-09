package ca.utoronto.utm.mcs;

import static org.neo4j.driver.Values.parameters;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

public final class Database {
    private Driver driver;
    private String uriDb;
    private Memory memory;

    public Database(Memory mem) {
        // neo4j is the name of our neo4j docker container
        uriDb = "bolt://neo4j:7687";
        // uriDb = "bolt://localhost:7687";
        driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j", "123456"));
        memory = mem;
    }

    /*
     * This method expects request body parameters:
     *
     * @parameters uid - a String representing uid in the location database
     * is_driver - boolean value which defines whether user is a driver
     *
     * @returns -1 : 500 internal server error 0 : 200 ok 1 : 200 user modified
     */
    public int putUser(String uid, Boolean isDriver) {
        try (Session session = driver.session()) {

            Result result = session.writeTransaction(tx -> tx.run(
                    "MERGE (n: user {uid: $x}) ON CREATE SET n.is_driver = $y, n.longitude = 0, n.latitude = 0, n.street_at = '' ON MATCH SET n.is_driver = $y",
                    parameters("x", uid, "y", isDriver)));
            SummaryCounters stats = result.consume().counters();
            if (stats.nodesCreated() != 0) {
                session.close();
                return 0;
            }
            session.close();
            return 1;
        } catch (Exception e) {
            return -1;
        }
    }

    /*
     * This method expects request body parameters:
     *
     * @parameters uid - a String representing uid in the location database
     *
     * @returns -1 : 500 internal server error 0 : 200 ok 1 : 404 user doesn't exist
     */
    public int deleteUser(String uid) {
        try (Session session = driver.session()) {

            Result result = session.writeTransaction(tx -> tx.run(
                "MATCH (n: user {uid: $x}) DELETE n",
                parameters("x", uid)
            ));
            SummaryCounters stats = result.consume().counters();
            if (stats.nodesDeleted() != 0) {
                session.close();
                return 0;
            }
            session.close();
            return 1;
        } catch (Exception e) {
            return -1;
        }
    }

    /*
     * This method expects request body parameters:
     *
     * @parameters uid - a String representing uid in the location database
     *
     * @returns -1 : 500 internal server error 0 : 200 ok 1 : 404 user doesn't exist
     */
    public int getLocation(String uid) {
        try (Session session = driver.session()) {
            Transaction tx = session.beginTransaction();
            Result matchUser = tx.run(
                "MATCH (n: user {uid: $x}) RETURN n",
                parameters("x", uid)
            );

            if (!matchUser.hasNext()) {
                session.close();
                return 1;
            }

            Record userRecord = matchUser.single();
            Node userNode = userRecord.get("n").asNode();
            String streetAt = userNode.get("street_at").asString();
            double longitude = userNode.get("longitude").asDouble();
            double latitude = userNode.get("latitude").asDouble();

            memory.setLongitude(longitude);
            memory.setLatitude(latitude);
            memory.setStreetAt(streetAt);

            session.close();
            return 0;
        } catch (Exception e) {
            return -1;
        }
    }

    /*
     * This method expects request body parameters:
     *
     * @parameters uid - a String representing uid in the location database
     * longitude - new longitude of user
     * latitude - new latitude of user
     *j
     * @returns -1 : 500 internal server error 0 : 200 ok 1 : 404 user doesn't exist
     */
    public int updateLocation(String uid, double longitude, double latitude, String streetAt) {
        try (Session session = driver.session()) {
            Result result = session.writeTransaction(tx -> tx.run(
                "MATCH (n: user {uid: $x}) SET n.longitude = $y, n.latitude = $z, n.street_at = $w",
                parameters("x", uid, "y", longitude, "z", latitude, "w", streetAt)
            ));
            SummaryCounters stats = result.consume().counters();
            if (stats.containsUpdates()) {
                session.close();
                return 0;
            }
            session.close();
            return 1;
        } catch (Exception e) {
            return -1;
        }
    }

    /*
     * This method expects request body parameters:
     *
     * @parameters uid - a String representing uid in the location database
     * radius - the radius of this ride search in meters
     * @returns -1 : 500 internal server error 0 : 200 ok 1 : 404 user doesn't exist 2: 400 if uid is a driver 3 : 404 drivers not found
     */
    public int getNearbyDriver(String uid, double radius) {
        try (Session session = driver.session()) {
            Transaction tx = session.beginTransaction();
            Result userExists = tx.run(
                "MATCH (n: user {uid: $x}) RETURN n",
                parameters("x", uid)
            );
            if (!userExists.hasNext()) {
                session.close();
                return 1;
            }
            Record userRecord = userExists.next();
            Node userNode = userRecord.get("n").asNode();

            // user is driver
            if (userNode.get("is_driver").asBoolean()) {
                session.close();
                return 2;
            }
            Result result = tx.run(
                "MATCH (a:user {uid: $x}) "
                + "WITH a "
                + "MATCH (b:user {is_driver: true}) WHERE id(b)<>id(a) "
                + "WITH a, b, distance(point({ latitude: a.latitude, longitude:a.longitude }), point({ latitude: b.latitude, longitude:b.longitude })) as dist "
                + "WHERE dist<=$y "
                + "RETURN b ",
                parameters("x", uid, "y", radius)
            );
            // driver not found
            if (!result.hasNext()) {
                session.close();
                return 3;
            }

            ArrayList<Map<String, Object>> driverNodes = new ArrayList<Map<String, Object>>();

            Record driverRecord;
            while (result.hasNext()) {
                driverRecord = result.next();
                Node driverNode = driverRecord.get("b").asNode();
                driverNodes.add(driverNode.asMap());
            }
            memory.setNearbyDrivers(driverNodes);

            session.close();
            return 0;
        } catch (Exception e) {
            return -1;
        }
    }

    /*
     * This method expects request body parameters:
     *
     * @parameters roadName - a String representing the name of the road in the location database
     * hasTraffic - boolean value which defines whether the road is busy
     *
     * @returns -1 : 500 internal server error 0 : 200 ok 1 : 200 modify existing road
     */
    public int putRoad(String roadName, Boolean hasTraffic) {
        try (Session session = driver.session()) {
            Result result = session.writeTransaction(tx -> tx.run(
                    "MERGE (n: road {name: $x}) ON CREATE SET n.is_traffic = $y ON MATCH SET n.is_traffic = $y",
                    parameters("x", roadName, "y", hasTraffic)));
            SummaryCounters stats = result.consume().counters();
            if (stats.nodesCreated() != 0) {
                session.close();
                return 0;
            }
            session.close();
            return 1;
        } catch (Exception e) {
            return -1;
        }
    }

    /*
     * This method expects request body parameters:
     *
     * @parameters roadName1 - name of the road
     * roadName2 - name of the other road reachable from roadName1
     * hasTraffic - boolean value which defines whether the route is busy
     * time - time travel from roadName1 to roadName2 in minutes
     *
     * @returns -1 : 500 internal server error 0 : 200 ok 1 : 400 route exists 2 : 404 roads do not exist
     */
    public int postRoute(String roadName1, String roadName2, Boolean hasTraffic, int travelTime) {
        try (Session session = driver.session()) {
            Transaction tx = session.beginTransaction();
            Result matchRoad1 = tx.run("MATCH (n: road {name:$x}) return n", parameters("x", roadName1));
            Result matchRoad2 = tx.run("MATCH (n: road {name:$x}) return n", parameters("x", roadName2));

            // Road(s) do not exist
            if (!matchRoad1.hasNext() || !matchRoad2.hasNext()) {
                session.close();
                return 2;
            }
            Result routeExists = tx.run(
                "MATCH (a: road {name: $x})-[r:ROUTE_TO]-(b: road {name: $y}) RETURN r",
                parameters("x", roadName1, "y", roadName2)
            );

            // route already exists
            if (routeExists.hasNext()) {
                session.close();
                return 1;
            }

            Result result = tx.run(
                "MERGE (a: road {name: $x}) WITH a MATCH (b: road {name: $y}) MERGE (a)-[:ROUTE_TO {travel_time: $z, is_traffic: $w}]->(b)",
            parameters("x", roadName1, "y", roadName2, "z", travelTime, "w", hasTraffic));

            SummaryCounters stats = result.consume().counters();
            if (stats.relationshipsCreated() != 0) {
                tx.commit();
                session.close();
                return 0;
            }
            session.close();
            return 1;
        } catch (Exception e) {
            return -1;
        }
    }

    /*
     * This method expects request body parameters:
     *
     * @parameters roadName1 - name of the road
     * roadName2 - name of the other road reachable from roadName1
     *
     * @returns -1 : 500 internal server error 0 : 200 ok 1 : 404 route doesn't exist 2 : 404 roads do not exist
     */
    public int deleteRoute(String roadName1, String roadName2) {
        try (Session session = driver.session()) {
            Transaction tx = session.beginTransaction();
            Result matchRoad1 = tx.run("MATCH (n: road {name:$x}) return n", parameters("x", roadName1));
            Result matchRoad2 = tx.run("MATCH (n: road {name:$x}) return n", parameters("x", roadName2));

            // Road(s) do not exist
            if (!matchRoad1.hasNext() || !matchRoad2.hasNext()) {
                session.close();
                return 2;
            }

            Result result = tx.run("MATCH (a: road {name: $x})-[r:ROUTE_TO]-(b: road {name: $y}) DELETE r",
            parameters("x", roadName1, "y", roadName2));

            SummaryCounters stats = result.consume().counters();
            if (stats.relationshipsDeleted() != 0) {
                tx.commit();
                session.close();
                return 0;
            }
            session.close();
            return 1;
        } catch (Exception e) {
            return -1;
        }
    }

    /*
     * This method expects request body parameters:
     *
     * @parameters roadName1 - name of the road
     * roadName2 - name of the other road reachable from roadName1
     *
     * @returns -1 : 500 internal server error 0 : 200 ok 1 : 404 driver/passenger doesn't exist 2 : 404 route(s) do not exist
     */
    public int postNavigation(String driverUid, String passengerUid) {
        try (Session session = driver.session()) {
            Transaction tx = session.beginTransaction();

            Result matchDriver = tx.run(
                "MATCH (n: user {uid:$x, is_driver: true}) return n",
                parameters("x", driverUid));

            Result matchPassenger = tx.run(
                "MATCH (n: user {uid:$x, is_driver: false}) return n",
                parameters("x", passengerUid));

            if (!matchDriver.hasNext() || !matchPassenger.hasNext()) {
                session.close();
                return 1;
            }

            Node driverNode = matchDriver.next().get("n").asNode();
            Node passengerNode = matchPassenger.next().get("n").asNode();

            String driverRoad = driverNode.get("street_at").asString();
            String passengerRoad = passengerNode.get("street_at").asString();

            // shortestpath() calculates the shortest path between two nodes
            // relationships(p) returns all the relationships in the path p
            Result result = tx.run(
                "MATCH (a:road {name: $x}), (b:road{name: $y})"
                + " MATCH p=(a)-[:ROUTE_TO*]->(b)"
                + " WITH p,reduce(s = 0, r IN relationships(p) | s + r.travel_time) AS total_time"
                + " RETURN p, total_time ORDER BY total_time ASC LIMIT 1",
                parameters("x", driverRoad, "y", passengerRoad));

            if (result.hasNext()) {
                Record routeRecord = result.next();
                ArrayList<Map<String, Object>> pathNodes = new ArrayList<Map<String, Object>>();
                int totalTime = routeRecord.get("total_time").asInt();

                Path rPath = routeRecord.get("p").asPath();
                Iterable<Node> nodes = rPath.nodes();
                Iterable<Relationship> rels = rPath.relationships();

                ArrayList<Integer> times = new ArrayList<Integer>();
                times.add(0);
                for (Relationship rel : rels) {
                    times.add(rel.get("travel_time").asInt());
                }

                int i = 0;
                for (Node node: nodes) {
                    String roadName = node.get("name").asString();
                    Boolean hasTraffic = new Boolean(node.get("is_traffic").asBoolean());
                    HashMap<String, Object> routeMap = new HashMap<String, Object>();
                    if (i == 0) {
                        routeMap.put("time", 0);
                    } else {
                        routeMap.put("time", times.get(i));
                    }
                    routeMap.put("road_name", roadName);
                    routeMap.put("is_traffic", hasTraffic);

                    pathNodes.add(routeMap);
                    i++;
                }

                memory.setTotalNavigationTime(totalTime);
                memory.setNavigation(pathNodes);

                session.close();
                return 0;
            }
            session.close();
            return 2;
        } catch (Exception e) {
            return -1;
        }
    }

    public void close() {
        driver.close();
    }
}
