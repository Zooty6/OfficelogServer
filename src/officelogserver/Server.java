package officelogserver;

import Messages.*;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    private void AddListeners(SocketIOServer server) {
        server.addConnectListener((SocketIOClient client) -> {            
            for (ClientData client1 : clients) {
                maxCID = (maxCID<client1.getID())?client1.getID():maxCID;
            }
            ClientData storedclient = new ClientData(++maxCID, client);
            clients.add(storedclient);            
            System.out.println("A client conected. CID: " + storedclient.getID());                        
        });

        server.addDisconnectListener((SocketIOClient client) -> {
            ClientData storedclient = null;
            for (ClientData client1 : clients) {
                if (client1.getClientSocket() == client)
                    storedclient = client1;                  
            }
            if(storedclient != null){
                System.out.println("Client disconnected! CID: " + storedclient.getID());
                clients.remove(storedclient);
            }
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
                if(client.getClientSocket()==sender)
                    System.out.println("Sending people to CID: " + client.getID());
            }
            
            ArrayList<PersonTemplate> people = new ArrayList<>();
            ResultSet APerson = DatabaseConnection.createStatement().executeQuery(SQLSELECTPEOPLE1); 
                                                                                //"SELECT ID, Name, Loc, Pic, Job FROM People WHERE IsDeleted = 0";
            
            while (APerson.next()) {
                ArrayList<String> per = new ArrayList<>();
                int id = APerson.getInt(1);
                
                ResultSet rsper = DatabaseConnection.createStatement().executeQuery(SQLSELECTPERMISSIONS);
                while (rsper.next()){
                    if(rsper.getInt(1) == id)
                        per.add(rsper.getString(2));
                }
                
                String job = APerson.getString(5);
                if (APerson.wasNull()) {
                    people.add(new PersonTemplate(APerson.getString(2), APerson.getString(3), APerson.getBytes(4), id, null, per.toArray(new String[per.size()])));
                }else{
                    people.add(new PersonTemplate(APerson.getString(2), APerson.getString(3), APerson.getBytes(4), id, job, per.toArray(new String[per.size()])));
                }
                //System.out.println(APerson.getString(1)+"\t"+APerson.getString(2)+"\t"+APerson.getString(3)+"\t"+APerson.getString(5));
           
            }
//            System.out.println("-----------------------------");
//            System.out.println(people);
            sender.sendEvent("people", people);
            try{
                ackSender.sendAckData(people);
            }catch(Exception e){
                System.out.println("CAN'T SEND");
            }
            //System.out.println("sent");
        });

        server.addEventListener("fetchrooms", null, (sender, data, ackSender) -> {
            for (ClientData client : clients) {
                if(client.getClientSocket()==sender)
                    System.out.println("Sending rooms to CID:" + client.getID());
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
                    if(room.getName().equals(conn.rA))
                        room.getNeighbors().add(conn.rB);
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
            ResultSet rsNeighbor = DatabaseConnection.createStatement().executeQuery("SELECT RNameB FROM RoomConnections WHERE RNameA = '"+room +"'");
            ArrayList<String> neighbors = new ArrayList<>();            
            while (rsNeighbor.next()){
                neighbors.add(rsNeighbor.getString(1));
            }
            //System.out.println(neighbors);
            ResultSet rsRoom = DatabaseConnection.createStatement().executeQuery("SELECT * FROM Rooms WHERE Name = '"+room + "'");
            rsRoom.next();
//            if(rsRoom.getBoolean("isOpen")){
//                System.out.println(rsRoom.getString(1));
//            }
            ArrayList<String> permissions = new ArrayList<>();
            ResultSet rsPer = DatabaseConnection.createStatement().executeQuery("SELECT RoomName FROM Permissions WHERE PersonID = " + personID);
            while(rsPer.next()){
                permissions.add(rsPer.getString(1));
            }
            
            if(neighbors.contains(loc)){
                //System.out.println("it's a neighbor");
                if(permissions.contains(room) || rsRoom.getBoolean("isOpen")){
                    //event = "Entered";
                    DatabaseConnection.createStatement().executeUpdate(SQLUPDATEPEOPLE3FIRST + room +"'" + SQLUPDATEPEOPLE3SECOND + personID); //"UPDATE People SET Loc = '"  " WHERE ID = "
                    server.getBroadcastOperations().sendEvent("entered", data);
                }else{
                    event = "Acces Denied";
                    DatabaseConnection.createStatement().executeUpdate(SQLINSERTLOGS + event +"', CURRENT_TIMESTAMP, "+ personID+ ", '" + room + "')");
                }
                //System.out.println(SQLINSERTLOGS + event +"', CURRENT_TIMESTAMP, "+ personID+ ", '" + room + "')");
                
            }else{
                System.out.println("Impossible step, possibly client has an outdated dataset.");
            }
            
//            if (((ButtonRoom) (event.getSource())).getRoom().isOpen()
//                    || selectedPerson.isAllowed(((ButtonRoom) (event.getSource())).getRoom())) {
//                if (((ButtonRoom) (event.getSource())).getRoom().getMaxPeople() == 0
//                        || (((ButtonRoom) (event.getSource())).getRoom().getMaxPeople()
//                        > ((ButtonRoom) (event.getSource())).getRoom().getBtnRoom().getPplHere())) {
//                    ((ButtonRoom) (event.getSource())).Enter(selectedPerson);
//                    model.getEventList().addEvent(
//                            new Event("Entered", selectedPerson, ((ButtonRoom) (event.getSource())).getRoom()));
//                    EnableNeighburs();
//                } else {
//                    model.getEventList().addEvent(
//                            new Event("No more place", selectedPerson, ((ButtonRoom) (event.getSource())).getRoom()));
//                    System.out.println("Sorry, we are full :(");
//                }
//            } else {
//                model.getEventList().addEvent(
//                        new Event("Acces denied", selectedPerson, ((ButtonRoom) (event.getSource())).getRoom()));
//                try (Connection conn = DriverManager.getConnection(URL, USER, PASSW)) {
//                    conn.createStatement().executeUpdate(
//                                    SQLINSERTLOGS + 
//                                    selectedPerson.getID() + ", '" + 
//                                    ((ButtonRoom) (event.getSource())).getRoom().getName() + "')");
//                } catch (SQLException ex) {
//                    Alert alert = new Alert(Alert.AlertType.ERROR);
//                    alert.setTitle("Officelog");
//                    alert.setHeaderText("SQL Error");
//                    alert.setContentText("There was an error connecting to the database");
//                    alert.showAndWait();                    
//                }
//                //System.out.println("GTFO");
//            }
//            //lbSelected.setText("TODO: " + selectedPerson.getName());
//           
            
        });

        //TODO
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
