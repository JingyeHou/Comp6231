package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import model.MyLogFormatter;
import model.Record;
import recordManager.RecordManager;
import recordManager.RecordManagerHelper;
import recordManager.RecordManagerImpl;

import java.util.logging.FileHandler;
import java.util.logging.Level;

public class Server {
	static HashMap<Integer, String> ManagerServerMap;
	HashMap<Character, List<Record>> RecordMap;
	/**
	 * The TeacherRecord_ID starts with TR10001 
	 * And the StuentRecord_ID starts with SR10001
	 */
	String TeacherRecord_ID = "TR10000";
	String StudentRecord_ID = "SR10000";
	
	public Server(){
		RecordMap = new HashMap<>();

	}
	
	public static void main(String[] args) throws IOException {

		
		ManagerServerMap = new HashMap<>();
		ManagerServerMap.put(1050,"MTL");
		ManagerServerMap.put(1040,"LVL");
		ManagerServerMap.put(1070,"DDO");
		
		if(args[1].isEmpty()){
			System.out.println("Please input a port number in the configeration window in Eclipse.");
			System.out.println("Run -> Run Configurations -> Arguments... ");
			System.out.println("1050 for MTL, 1040 for LVL, 1070 for DDO. ");
			return;
		}
		int port;
		try{
			port = Integer.parseInt(args[1]);
		}catch (Exception e) {
			System.out.println("Please input a valid port number.");

			return;
		}
		String serverName = ManagerServerMap.get(port);
		Logger logger = Logger.getLogger("ServerLogger");
		FileHandler fileHandler = new FileHandler(serverName + ".log");

		fileHandler.setFormatter(new MyLogFormatter());
		logger.addHandler(fileHandler);
		RecordManagerImpl manager = new RecordManagerImpl(port);
//-----------------------------------------CORBA
		 try {
		 ORB orb = ORB.init(args, null);
		 POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
		 rootpoa.the_POAManager().activate();
		 org.omg.CORBA.Object ref = rootpoa.servant_to_reference(manager);
		 RecordManager ss = RecordManagerHelper.narrow(ref);
		 org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
		 NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
		 NameComponent path[] = ncRef.to_name("SUM-SERVER");
		 ncRef.rebind(path, ss);
		 System.out.println ("Sum Server is running . . . ");
		 logger.log(Level.INFO, "The CORBA server has already started, port number:" + port);
		 manager.InitHashMap();
		 
			new Thread( () -> {

				DatagramSocket socket = null;
				try {
					socket = new DatagramSocket(port);
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				byte data[] = new byte[1024];//create packet data byte[]
		        DatagramPacket packet = new DatagramPacket(data, data.length);
				logger.log(Level.INFO, "UDP Server has been started, port number: " + port);

		        while(true){
		            try {
						socket.receive(packet);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}//listening 
		            InetAddress address = packet.getAddress(); 
		            int source_port = packet.getPort();
		            byte res[] = String.valueOf(manager.serverGetCount()).getBytes();
		            DatagramPacket packet2 = new DatagramPacket(res, res.length, address, source_port);
		            logger.log(Level.INFO, "Receive a reuest for GetCount.");
		            //
		            try {
						socket.send(packet2);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    		logger.log(Level.INFO, "The count has already sent.");
		        }

			}).start();
		 
		 orb.run();
		 } catch (Exception e) {
		 System.out.println ("Exception: " + e.getMessage());
		 }
//--------------------------------------------end of CORBA
//-----------------------------------------RMI
//		RecordManagerImpl manager = new RecordManagerImpl(port);
//		Registry registry = null;
//		try{
//			registry = LocateRegistry.getRegistry(port);
//			registry.list();
//		}catch (Exception e){
//			registry = LocateRegistry.createRegistry(port);
//
//		}	
//		logger.log(Level.INFO, "The RMI server has already started, port number:" + port);
//		registry.rebind("manager", manager);
//		manager.InitHashMap();
//-----------------------------------------end of RMI
		//UDP part
		DatagramSocket socket = new DatagramSocket(port);
		byte data[] = new byte[1024];//create packet data byte[]
        DatagramPacket packet = new DatagramPacket(data, data.length);
		logger.log(Level.INFO, "UDP Server has been started, port number: " + port);

        while(true){
            socket.receive(packet);//listening 
            InetAddress address = packet.getAddress(); 
            int source_port = packet.getPort();
            byte res[] = String.valueOf(manager.serverGetCount()).getBytes();
            DatagramPacket packet2 = new DatagramPacket(res, res.length, address, source_port);
            logger.log(Level.INFO, "Receive a reuest for GetCount.");
            //
            socket.send(packet2);
    		logger.log(Level.INFO, "The count has already sent.");
        }

        
        


	}
	

}
