#README

Makefile:
make : compile the progame
make ckean : remove all executables

Run:
To run the program, you need three different terminal windows with different host.
1. nEmulator: ./nEmulator-linux386 <emulator's receiving UDP port number in the forward (sender) direction>
				   <receiver’s network address>
				   <receiver’s receiving UDP port number>
				   <emulator's receiving UDP port number in the backward (receiver) direction>
				   <sender’s network address>
			 	   <sender’s receiving UDP port number> number>
				   <maximum delay of the link in units of millisecond>
				   <packet discard probability>
				   <verbose mode>
2. receiver: java receiver <hostname for the network emulator>
			   <UDP port number used by the link emulator to receive ACKs from the receiver>
			   <UDP port number used by the receiver to receive data from the emulator>
			   <name of the file into which the received data is written>
3. sender: java sender <host address of the network emu lator>lator>
		       <UDP port number used by the emulator to receive data from the sender> sender>
		       <UDP port number used by the sender to receive ACKs from the emulator> emulator>
		       <name of the file to be transferred>

Example Execution:
On the host ubuntu1604-002: ./nEmulator-linux386 13923 ubuntu1804-002 27261 6076 ubuntu1804-004 38287 1 0.2 0
On the host ubuntu1804-002: java receiver ubuntu1604-002 6076 27261 "out.txt"
On the host ubuntu1804-004: java sender ubuntu1604-002 13923 38287 "in.txt" 

undergrad mechines used for testing:
ubuntu1604-002, ubuntu1804-002, ubuntu1804-004

compiler: javac

output file: 
seqnum.log, ack.log, arrival.log, <outpit file>
