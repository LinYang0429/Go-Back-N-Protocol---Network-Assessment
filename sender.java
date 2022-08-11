import java.util.List;
import java.lang.*;
import java.io.*;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.FileWriter;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class sender {
	private static int MAX_PACKET_SIZE = 1024;

	public static void main(String[] args) throws Exception {
		InetAddress emulator_address = InetAddress.getLocalHost();
		int recevice_sender_port = 0;
		int recevice_ack_port = 0;
		String file_name = "";

		final int WINDOW_SIZE = 10;
		final int MAX_PACKET_LENGTH = 500;
		final int SEQ_NUM_MODULE = 32;
		
		List packets = new ArrayList();
		int curr_window_used = 0;
		int curr_seq_num = 0;
		String curr_data = "";
		int curr_ACK = -1;
		int curr_send_seq = 0;
		int window_start = 0;

		if (args.length != 4) {
			System.err.println("Number of command line parameters should be 4.");
			System.exit(1);
		}

		try {
			emulator_address = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			System.err.println("Wrong format for host address of the network emulator.");
			System.exit(1);
		}
		try {
			recevice_sender_port = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			System.err.println("Wrong format for port number used to recevice data from the sender.");
			System.exit(1);
		}
		try {
			recevice_ack_port = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			System.err.println("Wrong format for port number used to receive ACKs.");
			System.exit(1);
		}
		file_name = args[3];

		DatagramSocket UDPSendSocket = new DatagramSocket();
		DatagramSocket UDPReceiveSocket = new DatagramSocket(recevice_ack_port);
		FileWriter fwsn = new FileWriter("seqnum.log");
		FileWriter fwack = new FileWriter("ack.log");
		fwsn.close();
		fwack.close();

		// read file
		try{
			InputStream in = new FileInputStream(file_name);
			Reader r = new InputStreamReader(in, "US-ASCII");
			int intch;
			while ((intch = r.read()) != -1) {
  				char ch = (char) intch;
  				curr_data += ch;
  				if (curr_data.length() == MAX_PACKET_LENGTH) {
  					packet nextPacket = packet.createPacket(curr_seq_num, curr_data);
  					packets.add(nextPacket);
  					curr_seq_num++;
  					curr_data = "";
  				}
			}
			if (curr_data.length() != 0) {
				packet nextPacket = packet.createPacket(curr_seq_num, curr_data);
  				packets.add(nextPacket);
  				curr_data = "";
			}
		} catch (IOException e){
			System.err.println("IOException when read the file");
			System.exit(1);
		}

		// send file and receive acks
		while(curr_ACK != packets.size()-1) {
			// send packets
			while(curr_send_seq < packets.size() && curr_window_used < WINDOW_SIZE) {
				packet p = (packet) packets.get(curr_send_seq);
				curr_send_seq++;
				// print index of packet
				try{
					// print seqnum.log
           			fwsn = new FileWriter("seqnum.log", true);
           			fwsn.write(String.valueOf(p.getSeqNum()));    
           			fwsn.write(System.getProperty( "line.separator" ));
           			fwsn.close();
          		} catch (Exception e) {
          			System.out.println(e);
          		}
          		// send packet with udp
          		byte[] send = new byte[MAX_PACKET_SIZE];
			send = p.getUDPdata();
          		DatagramPacket dp = new DatagramPacket(send, send.length, emulator_address, recevice_sender_port);      
				UDPSendSocket.send(dp);

				if (curr_window_used++ == 0) {
					UDPReceiveSocket.setSoTimeout(100);
				}
			}
			// receiveACK
			while (curr_ACK != packets.size()-1) {
				try {
					// receive packet
					byte[] recive = new byte[MAX_PACKET_SIZE];
					DatagramPacket dp = new DatagramPacket(recive, recive.length);
					UDPReceiveSocket.receive(dp);
					byte[] data = dp.getData();
					packet result = packet.parseUDPdata(data);

					// handle ACK packet
					if (result.getType() == 0) {
						// print ack.log
						fwack = new FileWriter("ack.log", true);
						fwack.write(String.valueOf(result.getSeqnum()));    
           				fwack.write(System.getProperty( "line.separator" ));
           				fwack.close(); 

           				// compute ack change
           				int change = 0;
           				if (curr_ACK == -1) {
           					change = result.getSeqnum() - curr_ACK;
           				} else {
           					change = result.getSeqnum() - curr_ACK % SEQ_NUM_MODULE;
           				}
           				// handle ack change
           				if (change == 0) {
           					UDPReceiveSocket.setSoTimeout(0);
           					break;
           				} else if (change > 0 && change <= WINDOW_SIZE) {
           					curr_ACK += change;
           					window_start = curr_ACK + 1;
           				} else if (change < 0 && WINDOW_SIZE >= change + SEQ_NUM_MODULE) {
           					curr_ACK += change + SEQ_NUM_MODULE;
           					window_start = curr_ACK + 1;
           				} else {
           					continue;
           				}

           				if (curr_ACK == curr_send_seq - 1) {
           					UDPReceiveSocket.setSoTimeout(0);
           					break;
           				} else {
           					UDPReceiveSocket.setSoTimeout(100);
           				}
           				window_start = curr_ACK + 1;
					}

				} catch (SocketTimeoutException e) {
					curr_send_seq = window_start;
					curr_window_used = 0;
					break;
				}
			}
			curr_send_seq = window_start;
			curr_window_used = 0;

		}

		// handle EOT packet
		// send EOT
		packet packetToSend = packet.createEOT(0);
		byte[] eot = packetToSend.getUDPdata();
		DatagramPacket eotdatagram = new DatagramPacket(eot, eot.length, emulator_address, recevice_sender_port);
		UDPSendSocket.send(eotdatagram);
		// receive EOT
		while(true){
			byte[] recive = new byte[MAX_PACKET_SIZE];
			DatagramPacket dp = new DatagramPacket(recive, recive.length);
			UDPReceiveSocket.receive(dp);
			byte[] data = dp.getData();
			packet result = packet.parseUDPdata(data);
			 // if it is an EOT packet, clost socket and return
		    if (result.getType() == 2){
				UDPSendSocket.close();
				UDPReceiveSocket.close();
				break;
		    }
		}


	}


}
