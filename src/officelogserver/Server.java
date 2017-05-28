package officelogserver;

import Messages.*;
import com.corundumstudio.socketio.AckMode;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/**
 * @author Zooty
 */
public class Server implements DBConnection {

    private final SocketIOServer server;
    private Connection DatabaseConnection;
    private final int PORT = 8012;
    private final ArrayList<ClientData> clients = new ArrayList<>();
    private static final ObjectMapper objMapper = new ObjectMapper();
    private int maxCID = 0;

    /**
     * Initialize the server
     */
    public Server() {
        objMapper.registerModule(new JsonOrgModule());
        Configuration serverConfiguration = new Configuration();
        serverConfiguration.setHostname("localhost");
        serverConfiguration.setPort(PORT);
//        serverConfiguration.setPingTimeout(10);
        server = new SocketIOServer(serverConfiguration);
        System.out.println("Connecting to database...");
        try {
            DatabaseConnection = DriverManager.getConnection(URL, USER, PASSW);
        } catch (SQLException ex) {
            System.out.println("Could not connect to database! Error: " + ex.getMessage());
            System.exit(1);
        }
        System.out.println("Connected to database!");
        AddListeners(server);
        server.start();
        System.out.println("Server has started, listening at port: " + PORT);
    }

    public ArrayList<ClientData> getClients() {
        return clients;
    }

    public void broadcastMessage(String message){
        server.getBroadcastOperations().sendEvent("message", message);
    }
    
    private void AddListeners(SocketIOServer server) {
        server.addConnectListener((SocketIOClient client) -> {
            for (ClientData client1 : clients) {
                maxCID = (maxCID < client1.getID()) ? client1.getID() : maxCID;
            }
            ClientData storedclient = new ClientData(++maxCID, client);
            clients.add(storedclient);
            System.out.println("A client connected. CID: " + storedclient.getID());
        });
        
        server.addDisconnectListener((SocketIOClient client) -> {
            ClientData storedclient = null;
            for (ClientData client1 : clients) {
                if (client1.getClientSocket() == client) {
                    storedclient = client1;
                }
            }
            if (storedclient != null) {
                System.out.println("Client disconnected! CID: " + storedclient.getID());
                clients.remove(storedclient);
            }
        });
        
        server.addEventListener("delrequest", PersonTemplate.class, (SocketIOClient sender, PersonTemplate ta, AckRequest ackRequest) -> {
            Statement stm = DatabaseConnection.createStatement();
            stm.executeUpdate(SQLUPDATEPEOPLE2 + ta.getID());
            stm.close();
            System.out.println(ta.getID());
            for (ClientData client : clients) {
                client.getClientSocket().sendEvent("delperson", ta);
            }
    });

        server.addEventListener("addperson", PersonTemplate.class,
                (SocketIOClient sender, PersonTemplate ta, AckRequest ackRequest) -> {
                    DatabaseConnection.setAutoCommit(false);
//                    System.out.println(ta.getName());
                    PreparedStatement pstmPpl = DatabaseConnection.prepareStatement(SQLINSERTPEOPLE1);
                    pstmPpl.setInt(1, ta.getID());
                    pstmPpl.setString(2, ta.getName());
                    pstmPpl.setString(3, ta.getLocationName());
                    pstmPpl.setBytes(4, ta.getPic());
                    pstmPpl.setString(5, ta.getJob() != null ? ta.getJob() : null);
                    pstmPpl.setBoolean(6, false);
                    pstmPpl.executeUpdate();
                    if (ta.getJob() != null) {
                        PreparedStatement pstmPerm = null;
                        for (String room : ta.getPer()) {
                            pstmPerm = DatabaseConnection.prepareStatement(SQLINSERTPERMISSIONS2);
                            pstmPerm.setInt(1, ta.getID());
                            pstmPerm.setString(2, room);
                            pstmPerm.executeUpdate();
                        }
                        if (pstmPerm != null) {
                            pstmPerm.close();
                        }
                    }
                    DatabaseConnection.commit();
                    DatabaseConnection.setAutoCommit(true);
                    pstmPpl.close();
//                    System.out.println("new prson: " +ta.getName());
                    
                    for (ClientData client : clients) {
                        client.getClientSocket().sendEvent("newperson", 
                                (Object) new PersonTemplate(ta.getName(), ta.getLocationName(), ta.getPic(), ta.getID(), ta.getJob(), ta.getPer()));
                    }
                    //server.getBroadcastOperations().sendEvent("newperson", (Object) new PersonTemplate(ta.getName(), ta.getLocationName(), ta.getPic(), ta.getID(), ta.getJob(), ta.getPer()));                    


                });

        server.addEventListener("SimpleMessage", SimpleMessage.class,
                (SocketIOClient sender, SimpleMessage data, AckRequest ackRequest) -> {
                    for (ClientData client : clients) {
                        if (client.getClientSocket() == sender) {
                            System.out.println("CID: " + client.getID()
                                    + " Sent a message: " + data.getMessage());
                        }
                    }

                });

        server.addEventListener("fetchpeople", null, (sender, data, ackSender) -> {
            for (ClientData client : clients) {
                if (client.getClientSocket() == sender) {
                    System.out.println("Sending people to CID: " + client.getID());
                }
            }

            ArrayList<PersonTemplate> people = new ArrayList<>();
            ResultSet APerson = DatabaseConnection.createStatement().executeQuery(SQLSELECTPEOPLE1);
            //"SELECT ID, Name, Loc, Pic, Job FROM People WHERE IsDeleted = 0";

            while (APerson.next()) {
                ArrayList<String> per = new ArrayList<>();
                int id = APerson.getInt(1);

                ResultSet rsper = DatabaseConnection.createStatement().executeQuery(SQLSELECTPERMISSIONS);
                while (rsper.next()) {
                    if (rsper.getInt(1) == id) {
                        per.add(rsper.getString(2));
                    }
                }

                String job = APerson.getString(5);
                if (APerson.wasNull()) {
                    people.add(new PersonTemplate(APerson.getString(2), APerson.getString(3), APerson.getBytes(4), id, null, per.toArray(new String[per.size()])));
                } else {
                    people.add(new PersonTemplate(APerson.getString(2), APerson.getString(3), APerson.getBytes(4), id, job, per.toArray(new String[per.size()])));
                }
                //System.out.println(APerson.getString(1)+"\t"+APerson.getString(2)+"\t"+APerson.getString(3)+"\t"+APerson.getString(5));

            }
//            System.out.println("-----------------------------");
//            System.out.println(people);
            sender.sendEvent("people", people);
            try {
                ackSender.sendAckData(people);
            } catch (Exception e) {
                System.out.println("CAN'T SEND");
            }
            //System.out.println("sent");
            APerson.close();
        });

        server.addEventListener("fetchrooms", null, (sender, data, ackSender) -> {
            for (ClientData client : clients) {
                if (client.getClientSocket() == sender) {
                    System.out.println("Sending rooms to CID:" + client.getID());
                }
            }
            ArrayList<RoomTemplate> rooms = new ArrayList<>();
            ResultSet cat = DatabaseConnection.createStatement().executeQuery(SQLSELECTROOMS1);
            while (cat.next()) {
                //System.out.println(cat.getString(1) + "\t" + cat.getString(2) + "\t" + cat.getString(3));  
                RoomTemplate rt = new RoomTemplate(cat.getString(1),
                        cat.getString(1),
                        Integer.parseInt(cat.getString(2)),
                        (Integer.parseInt(cat.getString(3)) == 1));
                rooms.add(rt);
            }
            ResultSet fox
                    = // Zoli likes foxes
                    DatabaseConnection.createStatement().executeQuery(SQLSELECTROOMCONNECTIONS);
            class connStruct {

                String rA;
                String rB;

                public connStruct(String rA, String rB) {
                    this.rA = rA;
                    this.rB = rB;
                }
            }
            List<connStruct> conns = new ArrayList<>();
            while (fox.next()) {
                conns.add(new connStruct(fox.getString(1), fox.getString(2)));
            }

            for (RoomTemplate room : rooms) {
                for (connStruct conn : conns) {
                    if (room.getName().equals(conn.rA)) {
                        room.getNeighbors().add(conn.rB);
                    }
                }
                //System.out.println(room.getName() + " n: " + room.getNeighbors());
            }

//            for (RoomTemplate roomTemplate : rooms) {
//                System.out.println(roomTemplate.getName());
//            }
            ackSender.sendAckData(rooms);
            fox.close();
            cat.close();
        });

        server.addEventListener("enterrequest", Moving.class, (sender, data, ackSender) -> {
            String event = "";
            String room = data.getRoom();
            int personID = data.getPersonID();
            ResultSet rsPersonLoc = DatabaseConnection.createStatement().executeQuery("SELECT Loc FROM People WHERE ID = " + personID);
            rsPersonLoc.next();
            String loc = rsPersonLoc.getString(1);
            //System.out.println(loc);
            ResultSet rsNeighbor = DatabaseConnection.createStatement().executeQuery("SELECT RNameB FROM RoomConnections WHERE RNameA = '" + room + "'");
            ArrayList<String> neighbors = new ArrayList<>();
            while (rsNeighbor.next()) {
                neighbors.add(rsNeighbor.getString(1));
            }
            //System.out.println(neighbors);
            ResultSet rsRoom = DatabaseConnection.createStatement().executeQuery("SELECT * FROM Rooms WHERE Name = '" + room + "'");
            rsRoom.next();
//            if(rsRoom.getBoolean("isOpen")){
//                System.out.println(rsRoom.getString(1));
//            }
            ArrayList<String> permissions = new ArrayList<>();
            ResultSet rsPer = DatabaseConnection.createStatement().executeQuery("SELECT RoomName FROM Permissions WHERE PersonID = " + personID);
            while (rsPer.next()) {
                permissions.add(rsPer.getString(1));
            }

            if (neighbors.contains(loc)) {
                //System.out.println("it's a neighbor");
                if (permissions.contains(room) || rsRoom.getBoolean("isOpen")) {
                    //event = "Entered";
                    DatabaseConnection.createStatement().executeUpdate(SQLUPDATEPEOPLE3FIRST + room + "'" + SQLUPDATEPEOPLE3SECOND + personID); //"UPDATE People SET Loc = '"  " WHERE ID = "
                    server.getBroadcastOperations().sendEvent("entered", data);
                } else {
                    event = "Acces Denied";
                    DatabaseConnection.createStatement().executeUpdate(SQLINSERTLOGS + event + "', CURRENT_TIMESTAMP, " + personID + ", '" + room + "')");
                }
                //System.out.println(SQLINSERTLOGS + event +"', CURRENT_TIMESTAMP, "+ personID+ ", '" + room + "')");

            } else {
                System.out.println("Impossible step, possibly client has an outdated dataset.");
            }
            rsPer.close();
            rsPersonLoc.close();
            rsNeighbor.close();
            rsRoom.close();
        });

    }

    /**
     * Exit point, closes all connections.
     */
    public void ShutDown() {
//        I have thought
//        That this will never end
//        And things go on
//        But nothing will last
        try {
            for (ClientData client : clients) {
                client.getClientSocket().disconnect();
            }
            DatabaseConnection.close();
        } catch (SQLException ex) {
            System.out.println("Closing the connection to the database FAILED!");
            System.exit(1);
        }
        System.exit(0);
    }

}
