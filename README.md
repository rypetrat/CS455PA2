# CS455PA2
Stop and wait 
The following code for stop and wait works by sending a single packet from A Output and then waiting until an ack for that packet has been recieved by A Input. Then handling loss and corruption of packets by using A Timer Interupt and checksums for sent/recieved packets. Follow the prompts given by the initialization phase of the program for proper usage. 

Selective Repeat
The following code for selective repeat works by sending packets in the sending window as soon as they are recieved by A Output If the sending window is not already full, It maintains a timer to insure that packets lost due to loss are quickly detected and retransmitted as well as implementing checksums such that corrupted packets are also quickly detected and then retransmitted. Upon reaching a timeout for the A side the first remaining packet in the sending window. Follow the prompts given by the initialization phase of the program for proper usage. 

Go Back N 
The following code for Go Back N works by sending packets in the sending window as soon as they are recieved by A Output If the sending window is not already full, It maintains a timer to insure that packets lost due to loss are quickly detected and retransmitted as well as implementing checksums such that corrupted packets are also quickly detected and then retransmitted. Upon reaching a timeout for the A side the entire sending window of unAcked packets will be retransmitted.
