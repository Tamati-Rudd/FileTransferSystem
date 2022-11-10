package RMI;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server implementation of the PeerConnections remote interface
 * This remote object maintains a list of all Peers in the peer to peer system
 * The list also facilitates a logical "Token Ring" structure for Leader Election
 * There should only be one of these remote objects in the system
 * @author Tamati Rudd 18045626
 */
public class PeerConnectionsImpl implements PeerConnections {
    private ArrayList<Peer> peers; //Facilitates a "Token Ring" structure
    private Peer leader; //The leader of system
    private Timer timer;
    
    //Construct an empty peer connection object
    public PeerConnectionsImpl() {
        peers = new ArrayList<>();
        timer = new Timer();
        TimerTask snapshot = new TakeSnapshot();
        timer.schedule(snapshot, 1000, 10000); //Schedule the snapshot task to run every 10 seconds
    }
    
    //Inner class that periodically runs a Leader Election and takes a Global Snapshot 
    class TakeSnapshot extends TimerTask {
        @Override
        public void run() { //Scheduled Task
            try { 
                //Run an Election to determine who will take the next Snapshot
                peers.get(0).ChangRobertsStartElection();
                //Have the leader take a Global Snapshot
                leader.ChandyLamportInitiateSnapshot();
            } catch (RemoteException ex) {
                Logger.getLogger(PeerConnectionsImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    //Lookup a Peer in the list based off its name. Return null if not found
    @Override
    public Peer getPeerByName(String peerName) throws RemoteException {
        Peer match = null;
        for (Peer p : peers) {
            if (p.getName().equals(peerName))
                match = p;
        }
        return match;
    }
    
    //Get the next Peer (neighbour) in the "Token Ring of Peers" 
    //The next Peer is the Peer at the next index in the list, or loop back to the start
    @Override
    public Peer getNextPeer(Peer p) throws RemoteException {
        int index = peers.indexOf(p);
        if (index < peers.size()-1) { //Get next Peer
            index++;
            return peers.get(index); 
        }
        else //Get first Peer in the list (returns self if only one Peer)
            return peers.get(0); 
    }
    
    //Get the previous Peer (neighbour) in the "Token Ring of Peers"
    //The previous Peer is the Peer at the previous index in the list, or loop forward to the end
    @Override
    public Peer getPrevPeer(Peer p) throws RemoteException {
        int index = peers.indexOf(p);
        if (index > 0) { //Get previous Peer
            index--;
            return peers.get(index);
        }
        else //Get last Peer in the list (returns self if only one Peer)
            return peers.get(peers.size()-1);
    }


    //Add a peer to the list
    //Expand the Vector Timestamp of all existing Peers
    @Override
    public void addPeer(Peer peer) throws RemoteException {
        for (Peer p : peers) { 
            p.expandTimestamp();
        }
        peers.add(peer);
    }
    
    //Remove a peer from the list
    //Shrink the Vector Timestamp of all remaining Peers
    @Override
    public void removePeer(Peer peer) throws RemoteException {
        int indexRemoved = peer.getTimestamp().getOwnIndex();
        peers.remove(peer);
        for (Peer p : peers) { 
            p.shrinkTimestamp(indexRemoved);
        }
    }
    
    //Getter & Setter Methods
    @Override
    public ArrayList<Peer> getPeers() throws RemoteException {
        return peers;
    }
    
    @Override
    public Peer getLeader() throws RemoteException {
        return leader;
    }

    @Override
    public void setLeader(Peer leader) throws RemoteException {
        this.leader = leader;
    }
}
