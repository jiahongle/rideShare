package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class App {
    static int PORT = 8000;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        Memory mem = new Memory();
        Database db = new Database(mem);

        User user = new User(mem, db);
        Location location = new Location(mem, db);
        NearbyDriver nearbyDriver = new NearbyDriver(mem, db);
        Road road = new Road(mem, db);
        Route route = new Route(mem, db);
        Navigation navigation = new Navigation(mem, db);

        server.createContext("/user", user);
        server.createContext("/location", location);
        server.createContext("/NearbyDriver", nearbyDriver);
        server.createContext("/Road", road);
        server.createContext("/has_route", route);
        server.createContext("/route", route);
        server.createContext("/Navigation", navigation);

        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}
