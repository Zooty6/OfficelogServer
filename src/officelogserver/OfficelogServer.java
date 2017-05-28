package officelogserver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

/**
 *
 * @author Zooty
 */
public class OfficelogServer {

    private static Server server;

    /**
     *
     * Entry point
     */
    public static void main(String[] args) throws InterruptedException {
        server = new Server();
        System.out.println("Waiting for commands..");
        while (true) {
//            Once upon a time
//            No more words to say 
//            Find me in the circle
//            Find me in the end 
            Scanner in = new Scanner(System.in);
            handleInput(in.nextLine());
        }
    }

    private static void handleInput(String Input) {
        //System.out.println(Input);
        String[] command = Input.split("\\s+");
        switch (command[0].toLowerCase()) {
            case "hi":
            case "hello":
                System.out.println("Hi!");
                break;

            case "help":
                try {
                    try (BufferedReader in = new BufferedReader(new FileReader("files\\Commands.txt"))) {
                        String line;
                        while ((line = in.readLine()) != null) {
                            System.out.println(line);
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("Can't load 'Commands.txt'");
                }
                break;

            case "list":
                if (server.getClients().size() > 0) {
                    for (ClientData client : server.getClients()) {
                        System.out.print("CID: " + client.getID() + " ");
                    }
                } else {
                    System.out.print("No client is connected.");
                }
                System.out.println();
                break;

            case "disconnect":
                if (command.length == 2) {
                    try {
                        int id = Integer.parseInt(command[1]);
                        for (ClientData client : server.getClients()) {
                            if (client.getID() == id) {
                                client.getClientSocket().disconnect();
                                server.getClients().remove(client);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Wrong parameter: " + command[1]);
                    }
                } else if (command.length == 1) {
                    for (ClientData client : server.getClients()) {
                        client.getClientSocket().disconnect();                        
                    }
                    server.getClients().clear();
                }

                break;
                
            case "message":
                if(command.length>1){
                    String message = "";
                    try{
                        int id = Integer.parseInt(command[1]);
                        for (int i = 2; i < command.length; i++) {
                            message+=command[i] + " ";
                        }
                        for (ClientData client : server.getClients()) {
                            if(client.getID() == id)
                                client.getClientSocket().sendEvent("message", message);
                        }
                    }catch(NumberFormatException e){                        
                        for (int i = 1; i < command.length; i++) {
                            message+=command[i] + " ";
                        }
                        server.broadcastMessage(message);
                    }
                }
                break;

            case "exit":
                server.ShutDown();
                break;

            default:
                System.out.println(Input + " is not recognized as a command. "
                        + "Please type 'help' for a list of usable commands.");
        }
    }
}
