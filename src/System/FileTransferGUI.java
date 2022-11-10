package System;

import RMI.Peer;
import RMI.PeerImpl;
import RMI.PeerConnections;
import RMI.PeerConnectionsImpl;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * GUI & main class for the File Transfer peer to peer system
 * Also contains methods to handle creation, joining and leaving of a system
 * @author Tamati Rudd 18045626
 */
public class FileTransferGUI extends JPanel implements ActionListener {
    public static final String REGISTRY_URL = "localhost";
    private static JFrame frame;
    private Timer listUpdateTimer;
    private Timer timestampUpdateTimer;
    private JPanel joinLeavePanel; //Join, Create & Leave panel components
    private JLabel joinTitle;
    private JTextField nameInput;
    private JButton joinButton;
    private JButton leaveButton;
    private JLabel errorLabel;
    private JPanel sendPanel; //Send Files panel components
    private JLabel sendTitle;
    private DefaultListModel peersListModel;
    private JList peersList;
    private JScrollPane peersListDisplay;
    private JButton chooseFileButton;
    private JFileChooser selectFile;
    private File selectedFile;
    private JTextField chosenFile;
    private JButton sendButton;
    private JPanel receivedPanel; //Received Files panel components
    private JLabel receivedTitle;
    private DefaultListModel receivedFilesModel;
    private JList receivedFiles;
    private JScrollPane receivedFilesDisplay;
    private JButton openButton;
    private JPanel algorithmsPanel; //Distributed Algorithms panel components
    private JLabel systemInfoLabel;
    private JLabel leaderLabel;
    private JTextField timestampDisplay;
    private JLabel snapshotFileCount;
    private DefaultListModel globalSnapshotModel;
    private JList globalSnapshot;
    private JScrollPane globalSnapshotDisplay;

    //Construct the GUI panels
    public FileTransferGUI() {
        listUpdateTimer = new Timer(100, this);
        timestampUpdateTimer = new Timer(5100, this);
        GridBagConstraints joinLeaveConstraints = new GridBagConstraints();
        joinLeaveConstraints.gridx = 0;
        joinLeaveConstraints.gridy = 0;
        joinLeaveConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        joinLeaveConstraints.weightx = 0.5;
        joinLeaveConstraints.weighty = 0.1;
        joinLeaveConstraints.fill = GridBagConstraints.HORIZONTAL;
        joinLeaveConstraints.fill = GridBagConstraints.VERTICAL;
        joinLeavePanel = new JPanel(); 
        joinLeavePanel.setPreferredSize(new Dimension(300,300));
        joinLeavePanel.setBackground(Color.white);
        GridBagConstraints sendConstraints = new GridBagConstraints();
        sendConstraints.gridx = 0;
        sendConstraints.gridy = 1;
        sendConstraints.anchor = GridBagConstraints.LAST_LINE_START;
        sendConstraints.fill = GridBagConstraints.VERTICAL;
        sendPanel = new JPanel();
        sendPanel.setPreferredSize(new Dimension(300,300));
        sendPanel.setBackground(Color.white);
        sendPanel.setVisible(false);
        GridBagConstraints receivedConstraints = new GridBagConstraints();
        receivedConstraints.gridx = 1;
        receivedConstraints.gridy = 1;
        receivedConstraints.anchor = GridBagConstraints.LAST_LINE_END;
        receivedConstraints.fill = GridBagConstraints.VERTICAL;
        receivedPanel = new JPanel();
        receivedPanel.setPreferredSize(new Dimension(300,300));
        receivedPanel.setBackground(Color.white);
        receivedPanel.setVisible(false);
        GridBagConstraints algorithmsConstraints = new GridBagConstraints();
        algorithmsConstraints.gridx = 1;
        algorithmsConstraints.gridy = 0;
        algorithmsConstraints.anchor = GridBagConstraints.FIRST_LINE_END;
        algorithmsConstraints.fill = GridBagConstraints.VERTICAL;
        algorithmsPanel = new JPanel();
        algorithmsPanel.setPreferredSize(new Dimension(300,300));
        algorithmsPanel.setBackground(Color.white);
        algorithmsPanel.setVisible(false);  
        //Join, Create & Leave panel components
        joinTitle = new JLabel("Create, Join or Leave"); 
        joinTitle.setFont(new Font("Arial", Font.BOLD, 20));
        joinLeavePanel.add(joinTitle, joinLeaveConstraints); 
        nameInput = new JTextField();
        nameInput.setColumns(20);
        nameInput.setBorder(BorderFactory.createTitledBorder("Name"));
        joinLeavePanel.add(nameInput, joinLeaveConstraints);
        joinButton = new JButton("Create/Join System");
        joinButton.addActionListener(this);
        joinLeavePanel.add(joinButton, joinLeaveConstraints);
        leaveButton = new JButton("Leave System");
        leaveButton.addActionListener(this);
        leaveButton.setEnabled(false);
        joinLeavePanel.add(leaveButton, joinLeaveConstraints);
        errorLabel = new JLabel("Error: None");
        joinLeavePanel.add(errorLabel, joinLeaveConstraints);
        //Send a File panel components
        sendTitle = new JLabel("Send a File"); 
        sendTitle.setFont(new Font("Arial", Font.BOLD, 20));
        sendPanel.add(sendTitle, sendConstraints);
        peersListModel = new DefaultListModel();
        peersList = new JList(peersListModel); //Uses multiple selection by default
        peersListDisplay = new JScrollPane(peersList);
        peersListDisplay.setBorder(BorderFactory.createTitledBorder("Select Recipients"));
        peersListDisplay.setPreferredSize(new Dimension(280,150));
        sendPanel.add(peersListDisplay, sendConstraints);
        chooseFileButton = new JButton("Choose File");
        chooseFileButton.addActionListener(this);
        sendPanel.add(chooseFileButton, sendConstraints);
        selectFile = new JFileChooser(new File("root"));
        selectFile.setMultiSelectionEnabled(false);
        chosenFile = new JTextField();
        chosenFile.setColumns(20);
        chosenFile.setEditable(false);
        chosenFile.setText("None");
        chosenFile.setBorder(BorderFactory.createTitledBorder("Chosen File"));
        sendPanel.add(chosenFile, sendConstraints);
        sendButton = new JButton("Send File");
        sendButton.setEnabled(false);
        sendButton.addActionListener(this);
        sendPanel.add(sendButton, sendConstraints);
        //Received Files panel components
        receivedTitle = new JLabel("Files Received"); 
        receivedTitle.setFont(new Font("Arial", Font.BOLD, 20));
        receivedPanel.add(receivedTitle, receivedConstraints);
        receivedFilesModel = new DefaultListModel();
        receivedFiles = new JList(receivedFilesModel); 
        receivedFiles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); //Allow only one selection at a time
        receivedFilesDisplay = new JScrollPane(receivedFiles);
        receivedFilesDisplay.setBorder(BorderFactory.createTitledBorder("Your Peer Directory"));
        receivedFilesDisplay.setPreferredSize(new Dimension(280,150));
        receivedPanel.add(receivedFilesDisplay, receivedConstraints);
        openButton = new JButton("Open File");
        openButton.addActionListener(this);
        receivedPanel.add(openButton, receivedConstraints);
        //Distributed Algorithm panel components
        systemInfoLabel = new JLabel("System Information");
        systemInfoLabel.setFont(new Font("Arial", Font.BOLD, 20));
        algorithmsPanel.add(systemInfoLabel, algorithmsConstraints);
        timestampDisplay = new JTextField();
        timestampDisplay.setColumns(20);
        timestampDisplay.setBorder(BorderFactory.createTitledBorder("Your Vector Timestamp"));
        timestampDisplay.setEditable(false);
        algorithmsPanel.add(timestampDisplay, algorithmsConstraints);
        leaderLabel = new JLabel("Leader: Not Elected");
        leaderLabel.setFont(new Font("Arial", Font.PLAIN, 17));
        algorithmsPanel.add(leaderLabel, algorithmsConstraints);
        snapshotFileCount = new JLabel("Unique Files in Peer System: 0");
        snapshotFileCount.setFont(new Font("Arial", Font.PLAIN, 17));
        algorithmsPanel.add(snapshotFileCount, algorithmsConstraints);
        globalSnapshotModel = new DefaultListModel();
        globalSnapshot = new JList(globalSnapshotModel); 
        globalSnapshotDisplay = new JScrollPane(globalSnapshot);
        globalSnapshotDisplay.setBorder(BorderFactory.createTitledBorder("Global Snapshot of System Files"));
        globalSnapshotDisplay.setPreferredSize(new Dimension(280,150));
        algorithmsPanel.add(globalSnapshotDisplay, algorithmsConstraints);
        //Add panels to frame
        frame.add(joinLeavePanel, joinLeaveConstraints);
        frame.add(sendPanel, sendConstraints);
        frame.add(receivedPanel, receivedConstraints);
        frame.add(algorithmsPanel, algorithmsConstraints);
    }
    
    //Main class: setup & display the GUI
    public static void main(String[] args) {
        frame = new JFrame("File Transfer P2P System"); 
        frame.getContentPane().setBackground(Color.white);
        frame.setLayout(new GridBagLayout());
        frame.setPreferredSize(new Dimension(630, 640));
        frame.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); 
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.getContentPane().add(new FileTransferGUI());
        frame.setResizable(false);
        frame.pack(); 
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenDimension = tk.getScreenSize();
        Dimension frameDimension = frame.getSize();
        frame.setLocation((screenDimension.width-frameDimension.width)/2, (screenDimension.height-frameDimension.height)/2);
        frame.setVisible(true);
    }
    
    //Handle button presses and other user input
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == joinButton) { //Create or Join a P2P System
            if (nameInput.getText().isBlank()) { //Verify a name was input
                errorLabel.setText("Error: name input is empty");
                nameInput.setText("");
                return;
            }
            //Attempt to create, and get, the registry
            createRegistry();
            Registry registry = getRegistry();
            //Get the peer connections object (the method creates it if it doesn't exist)
            PeerConnections connectionsProxy = getPeerConnections();
            
            //Validate unique name in system (by checking against existing peersList)
            ArrayList<Peer> existingPeers = getPeersList();
            if (existingPeers != null) { 
                boolean nameUnique = checkNameUnique(existingPeers);
                if (nameUnique) { //Get local IP address
                    //Create peer object
                    try {
                        //Create a new peer remote object. Note: size+1 used as peerCount parameter as the new peer isn't in the list yet
                        String ip = getLocalIp();
                        PeerImpl myPeerObject = new PeerImpl(nameInput.getText(), ip, existingPeers.size()+1);
                        //Bind the new peer object to the registry
                        Peer stub = (Peer) UnicastRemoteObject.exportObject(myPeerObject, 0);
                        registry.rebind(myPeerObject.getName(), stub);
                        //Add the new peer object to the peer connections object
                        connectionsProxy.addPeer(myPeerObject);
                        //Adjust GUI components, start peer list update timer
                        joinButton.setEnabled(false);
                        leaveButton.setEnabled(true);
                        nameInput.setEditable(false);
                        sendPanel.setVisible(true);
                        receivedPanel.setVisible(true);
                        algorithmsPanel.setVisible(true);
                        listUpdateTimer.start();
                        timestampUpdateTimer.start();
                    } catch (RemoteException ex) {
                        errorLabel.setText("Error: Could not add your peer object");
                        Logger.getLogger(FileTransferGUI.class.getName()).log(Level.SEVERE, null, ex);
                    }      
                } else {
                    errorLabel.setText("Error: your desired name is taken");
                }   
            }  
        }
        if (source == leaveButton) { //Leave a P2P System 
            try { 
                listUpdateTimer.stop();
                timestampUpdateTimer.stop();
                //Get own remote Peer object
                Registry registry = getRegistry();
                PeerConnections connectionsProxy = getPeerConnections();
                Peer myPeerObject = connectionsProxy.getPeerByName(nameInput.getText());
                //Remove own remote Peer object
                connectionsProxy.removePeer(myPeerObject);
                registry.unbind(myPeerObject.getName());
                //Adjust available GUI options accordingly
                joinButton.setEnabled(true);
                leaveButton.setEnabled(false);
                nameInput.setEditable(true);
                sendPanel.setVisible(false);
                receivedPanel.setVisible(false);
                algorithmsPanel.setVisible(false);
            } catch (RemoteException | NotBoundException ex) {
                errorLabel.setText("Error: error leaving peer system");
                Logger.getLogger(FileTransferGUI.class.getName()).log(Level.SEVERE, null, ex);
            }  
        } 
        if (source == chooseFileButton) { //Choose the file to send
            int option = selectFile.showOpenDialog(FileTransferGUI.this);
            if (option == JFileChooser.APPROVE_OPTION) {
                selectedFile = selectFile.getSelectedFile();
                String filename = selectedFile.getName();
                chosenFile.setText(filename);
                sendButton.setEnabled(true);
            }
        }
        if (source == sendButton) { //
            List selectedValues = peersList.getSelectedValuesList();
            if (selectedValues.isEmpty()) { //Disallow sending as no recipients selected
                JOptionPane.showMessageDialog(frame, "You must pick at least one file recipient!", "No Recipients Chosen", JOptionPane.ERROR_MESSAGE);
            }
            else { //Call the user's Peer object to send the file
                List<String> recipientNames = peersList.getSelectedValuesList();
                try {
                    PeerConnections connectionsProxy = getPeerConnections();
                    Peer myPeerObject = connectionsProxy.getPeerByName(nameInput.getText());
                    myPeerObject.sendFile(selectedFile, recipientNames);
                    JOptionPane.showMessageDialog(frame, "The File has been sent!", "File Sent", JOptionPane.INFORMATION_MESSAGE);
                } catch (RemoteException ex) {
                    JOptionPane.showMessageDialog(frame, "There was a problem sending the File!", "Error Sending File", JOptionPane.ERROR_MESSAGE);
                    errorLabel.setText("Error: Your file could not be sent");
                    Logger.getLogger(FileTransferGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
                //Clear GUI Fields as the File has been sent (or failed to send)
                sendButton.setEnabled(false);
                selectedFile = null;
                chosenFile.setText("None");
                peersList.clearSelection();
            } 
        }
        if (source == openButton) { //Open a selected File
            if (receivedFiles.isSelectionEmpty())
                JOptionPane.showMessageDialog(frame, "Please select a File from the list to open it!", "No File Selected", JOptionPane.ERROR_MESSAGE);
            try {
                //Get File object to open
                PeerConnections connectionsProxy = getPeerConnections();
                Peer myPeerObject = connectionsProxy.getPeerByName(nameInput.getText());
                ArrayList<File> peerFiles = myPeerObject.getMyPeerFiles();
                File fileToOpen = null;
                for (File f : peerFiles) {
                    if (f.getName().equals((String) receivedFiles.getSelectedValue()))
                        fileToOpen = f;
                }
                //Attempt to open the File using a Desktop application (if a File was selected & found)
                    if (Desktop.isDesktopSupported() && fileToOpen != null) {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(fileToOpen);
                    }
            } catch (RemoteException ex) {
                errorLabel.setText("Error: Could not open File");
                Logger.getLogger(FileTransferGUI.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                errorLabel.setText("Error: Desktop not supported, cannot open files");
            }
        }
        if (source == timestampUpdateTimer) { //Update the timestamp & leader displays
            try {
                PeerConnections connectionsProxy = getPeerConnections();
                Peer myPeerObject = connectionsProxy.getPeerByName(nameInput.getText());
                timestampDisplay.setText(myPeerObject.getTimestamp().toString());
                Peer leader = connectionsProxy.getLeader();
                if (leader != null) { //Update Leader, Unique System Files & Global File List
                    leaderLabel.setText("Leader: "+leader.getName());
                    snapshotFileCount.setText("Unique Files in Peer System: "+leader.getGlobalFileList().size()); 
                    ArrayList<File> globalFileList = leader.getGlobalFileList();
                    globalSnapshotModel.clear();
                    for (File f : globalFileList) {
                        globalSnapshotModel.addElement(f.getName());
                    }
                }
                else
                    leaderLabel.setText("Leader: Not Elected");
            }
            catch (RemoteException ex) {
                Logger.getLogger(FileTransferGUI.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        if (source == listUpdateTimer) { //Periodically update the Peers list and Received Files list
            if (listUpdateTimer.getDelay() == 100) //Slow timer updates after the initial list generation
                listUpdateTimer.setDelay(5000); 
            try { 
                //Get the Peers list from the Connections object, then populate an array of the names
                ArrayList<Peer> peers = getPeersList();
                String[] peerNames = new String[peers.size()-1];
                int index = 0;
                for (Peer p : peers) {
                    if (!p.getName().equals(nameInput.getText())) { //Ignore own name
                        peerNames[index] = p.getName();
                        index++;
                    } 
                }
                //Repopulate the JList model and reselecte previously selected elements
                List<String> selected = peersList.getSelectedValuesList();
                peersListModel.removeAllElements();
                int[] toSelect = new int[peerNames.length]; //Array of indicies to re-select
                index = 0;
                for (int i = 0; i < peerNames.length; i++) {
                    if (!peersListModel.contains(peerNames[i])) {
                        peersListModel.addElement(peerNames[i]); //Add name to the list model
                        if (selected.contains(peerNames[i])) { //If the name was previously selected, mark it for re-selection
                            toSelect[index] = i;
                            index++;
                        }
                    }
                }
                peersList.setSelectedIndices(toSelect); //Re-select indicies
                
                //Update list of Received Files
                PeerConnections connectionsProxy = getPeerConnections();
                Peer myPeerObject = connectionsProxy.getPeerByName(nameInput.getText());
                ArrayList<File> peerFiles = myPeerObject.getMyPeerFiles();
                String currentFile = (String) receivedFiles.getSelectedValue();
                receivedFilesModel.removeAllElements();
                for (File f : peerFiles) { //Add all Peer Files to the list
                    receivedFilesModel.addElement(f.getName());
                    if (f.getName().equals(currentFile)) //If the File was selected, re-select it
                        receivedFiles.setSelectedValue(f.getName(), true);
                }
            } 
            catch (RemoteException ex) {
                Logger.getLogger(FileTransferGUI.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }
    
    //Get reference to the RMI registry
    public Registry getRegistry() {
        try { 
           return LocateRegistry.getRegistry(REGISTRY_URL);
        } catch (RemoteException ex) { //Registry doesn't exist, co create it
            errorLabel.setText("Couldn't get registry");
            return null;
        }
    }
    
    //Create and get reference to a new RMI registry
    public void createRegistry() {
        try {
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException ex) {} //Do nothing - the registry already exists
    }
    
    //Check for and get reference to the Peer Connections remote object
    public PeerConnections getPeerConnections() {
        try {
            Registry registry = getRegistry();
            PeerConnections connectionsProxy = (PeerConnections) registry.lookup("connections");
            return connectionsProxy;
        } catch (RemoteException ex) { //Lookup failed, report & log error
            errorLabel.setText("Error: Could not get peer connections remote reference");
            Logger.getLogger(FileTransferGUI.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (NotBoundException ex) { //Remote object doesn't exist - create it
            createPeerConnections();
            return getPeerConnections(); //Run getPeerConnections() again to now get the created object
        }
    }
    
    //Create & bind a new Peer Connections remote object
    public void createPeerConnections() {
        try {
            PeerConnectionsImpl remoteObject = new PeerConnectionsImpl();
            PeerConnections stub = (PeerConnections) UnicastRemoteObject.exportObject(remoteObject, 0);
            Registry registry = getRegistry();
            registry.rebind("connections", stub);
        } catch (RemoteException ex) { //Remote object creation failed - report and log error
            errorLabel.setText("Error: Could not create peer connections remote reference");
            Logger.getLogger(FileTransferGUI.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    //Get the list of peersList from the Peer Connections remote object
    public ArrayList<Peer> getPeersList() {
        try { //Get list of Peers from the Peer Connections remote object
            Registry registry = getRegistry();
            PeerConnections connectionsProxy = (PeerConnections) registry.lookup("connections");
            ArrayList<Peer> peers = connectionsProxy.getPeers();
            
            //Increment & print own timestamp (if the peer has been created)
            Peer myPeerObject = connectionsProxy.getPeerByName(nameInput.getText());
            if (myPeerObject != null) 
                myPeerObject.updateTimestamp(null);
            return peers;
        } catch (RemoteException | NotBoundException ex) {
            errorLabel.setText("Error: could not get peers list");
            Logger.getLogger(FileTransferGUI.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    //Check the client's name input is unique in the system it wants to join
    public boolean checkNameUnique(ArrayList<Peer> existingPeers) {
        boolean nameUnique = true;
        for (Peer p : existingPeers) { //Check each peer's name against input
            try {
                if (nameInput.getText().equals(p.getName()))
                    nameUnique = false;
            } catch (RemoteException ex) {
                errorLabel.setText("Error: failed to check against a peer name");
                Logger.getLogger(FileTransferGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return nameUnique;
    }
    
    //Get the local user's IP address
    public String getLocalIp() {
        String ip = "";
        try {
            InetAddress address = InetAddress.getLocalHost();
            ip = address.getHostAddress();
        } catch (UnknownHostException ex) { //Report and log IP address get error
            errorLabel.setText("Error: could not get the local IP address");
            Logger.getLogger(FileTransferGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ip;
    }
}
