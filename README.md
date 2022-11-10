# FileTransferSystem
Distributed peer to peer file transfer system that runs through Java remote method invocation (RMI)

Allows many peers to connect to each other remotely and share files with each other
- Uses Java RMI to manage the connection and messaging
- Each peer has a vector timestamp, which is included with and updated by messages
- Uses leader election, global snapshot and mutual exclusion algorithms to help run the system

Created in Netbeans for a university assignment.
