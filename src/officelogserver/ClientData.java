package officelogserver;

import com.corundumstudio.socketio.SocketIOClient;
import java.util.Objects;

/**
 *
 * @author Zooty
 */
public class ClientData {
    private final int ID;
    private final SocketIOClient clientSocket;

    public ClientData(int ID, SocketIOClient clientSocket) {
        this.ID = ID;
        this.clientSocket = clientSocket;
    }

    public int getID() {
        return ID;
    }

    public SocketIOClient getClientSocket() {
        return clientSocket;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.clientSocket);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ClientData other = (ClientData) obj;
        if (!Objects.equals(this.clientSocket, other.clientSocket)) {
            return false;
        }
        return true;
    }
    
    
    
    

}
