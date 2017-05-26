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
            handleInput(in.next());
        }
    }

    private static void handleInput(String Input) {
        switch (Input.toLowerCase()) {
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

            case "exit":
                server.ShutDown();
                break;

            default:
                System.out.println(Input + " is not recognized as a command. "
                        + "Please type 'help' for a list of usable commands.");
        }
    }
}
