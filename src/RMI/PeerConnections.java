package RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
/**
 * Remote interface representing a Peer Connections object
 * @author Tamati Rudd 18045626
 */
public interface PeerConnections extends Remote {
    public Peer getPeerByName(String peerName) throws RemoteException;
    public Peer getNextPeer(Peer p) throws RemoteException;
    public Peer getPrevPeer(Peer p) throws RemoteException;
    public void addPeer(Peer peer) throws RemoteException;
    public void removePeer(Peer peer) throws RemoteException;
    public ArrayList<Peer> getPeers() throws RemoteException;
    public Peer getLeader() throws RemoteException;
    public void setLeader(Peer leader) throws RemoteException;
}
