import java.io.*;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.io.FileWriter;

public class receiver {
	private static int MAX_PACKET_SIZE = 1024;
	private static InetAddress emulator_address;
	private static int recevice_port;
	private static int sender_port;
	private static String file_name;

	private static final int WINDOW_SIZE = 10;
	private static final int MAX_PACKET_LENGTH = 500;
	private static final int SEQ_NUM_MODULE = 32;
	private static int curr_seq_num = 0; // next seq num
	private static int returned_seq_num;
	private static DatagramSocket UDPSendSocket;
	private static DatagramSocket UDPReceiveSocket;

	public static void sendACK() throws Exception {
		packet p = packet.createACK(returned_seq_num);
        byte[] send = p.getUDPdata();
        DatagramPacket dp = new DatagramPacket(send, send.length, emulator_address, recevice_port);      
		UDPSendSocket.send(dp);
	}

	public static void main(String args[]) throws Exception {
		// read input from command line
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
			recevice_port = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			System.err.println("Wrong format for port number used to recevice data from the sender.");
			System.exit(1);
		}
		try {
			sender_port = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			System.err.println("Wrong format for port number used to receive ACKs.");
			System.exit(1);
		}
		file_name = args[3];

		UDPSendSocket = new DatagramSocket(recevice_port);
		UDPReceiveSocket = new DatagramSocket(sender_port);

		curr_seq_num = 0;
		returned_seq_num = -1;
		FileWriter fw = new FileWriter("arrival.log");
		FileWriter out = new FileWriter(file_name);
		fw.close();
		out.close();

		// receive  packet
		while (true) {
			byte[] recive = new byte[MAX_PACKET_SIZE];
			DatagramPacket dp = new DatagramPacket(recive, recive.length);
			UDPReceiveSocket.receive(dp);
			byte[] data = dp.getData();
			packet result = packet.parseUDPdata(data);

			// handle received packet
			if (result.getType() == 1) {
				if (result.getSeqnum() == curr_seq_num) {
					returned_seq_num = curr_seq_num;
					sendACK(); // this can add an argument
					// write to out file
					byte[] read_in = result.getData();
					out = new FileWriter(file_name, true);
					out.write(new String(read_in));
           			out.close(); 
           			curr_seq_num = (curr_seq_num + 1) % SEQ_NUM_MODULE;
				} else {
					if (curr_seq_num == 0) {
						return;
					}
					sendACK();
				}
				fw = new FileWriter("arrival.log", true);
				fw.write(String.valueOf(result.getSeqnum()));
				fw.write(System.getProperty( "line.separator" ));
				fw.close();
			} else if (result.getType() == 2) {
				packet packetToSend = packet.createEOT(0);
				byte[] eot = packetToSend.getUDPdata();
				DatagramPacket eotdatagram = new DatagramPacket(eot, eot.length, emulator_address, recevice_port);
				UDPSendSocket.send(eotdatagram);
				UDPSendSocket.close();
				UDPReceiveSocket.close();
				break;
			}
		}

	}
}
