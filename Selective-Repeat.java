import java.util.*;
import java.io.*;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          create a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;

    //Holder/iteration variables
    private Packet[] sendBuffer;
    private Packet[] recieveBuffer;
    private int currentSeqNumber;
    private int currentAckNumber;
    private int nRet;

    //Final statistics variables
    private int transmittedByA;
    private int retransmissionsByA;
    private int deliveredToLayer5;
    private int acksSentByB;
    private int corruptedPackets;
    private double RTT;
    private double communicationTime;
    private int lostPackets;
    private double[][] timers;
    
    
    
    

    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
	WindowSize = winsize;
	LimitSeqNo = WindowSize; //SR
	RxmtInterval = delay;
    }

    
    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
        Packet packet = new Packet(currentSeqNumber, currentAckNumber, 0, message.getData());
        int cSum = calculateChecksum(packet);
        packet.setChecksum(cSum);

        //System.out.println("packet with seqNum=" + packet.getSeqnum()+ " has been added to the buffer");

        if (sendBuffer[packet.getSeqnum()] == null) {
            sendBuffer[packet.getSeqnum()] = packet;
            double startTime = System.currentTimeMillis();
            timers[currentSeqNumber][0] = startTime;
            timers[currentSeqNumber][2] = 0.0;
        }
        
        startTimer(A, RxmtInterval*(((double)WindowSize+1)*.5));
        
        currentSeqNumber +=1;
        if (currentSeqNumber >= LimitSeqNo) {
            currentSeqNumber = currentSeqNumber % LimitSeqNo;
        }

        //System.out.println("A Sends " + packet);
        toLayer3(A, sendBuffer[packet.getSeqnum()]);
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        int cSum = calculateChecksum(packet);
        if (cSum == packet.getChecksum()) {
            System.out.println("Final Recieve " + packet);
            
            if (recieveBuffer[packet.getSeqnum()] != null && recieveBuffer[packet.getSeqnum()].getChecksum() == packet.getChecksum()) {
                acksSentByB += 1;

                double finishTime = System.currentTimeMillis();
                timers[currentSeqNumber][1] = finishTime;
                //System.out.println(timers[packet.getSeqnum()][0] + " " +timers[packet.getSeqnum()][1] + " " + timers[packet.getSeqnum()][2]);
                if (timers[packet.getSeqnum()][2] == 0.0) {
                    RTT += -1*(timers[packet.getSeqnum()][1] - timers[packet.getSeqnum()][0]);
                }
                communicationTime += -1*(timers[packet.getSeqnum()][1] - timers[packet.getSeqnum()][0]);

                toLayer5(packet.getPayload());
                deliveredToLayer5 += 1;

                sendBuffer[packet.getSeqnum()] = null;
                recieveBuffer[packet.getSeqnum()] = null;
                transmittedByA +=1;
            }
        }
        else {
            System.out.println("AIn corrupted packet detected " + packet);
            corruptedPackets += 1;
            retransmissionsByA +=1;

            if (packet.getSeqnum() > LimitSeqNo) {
                if (packet.getAcknum()-1 != -1 && recieveBuffer[packet.getAcknum()-1] != null) {
                    toLayer3(A, recieveBuffer[packet.getAcknum()-1]);
                    timers[packet.getAcknum()-1][2] = 1.0;
                }
            }
            else {
                if (recieveBuffer[packet.getSeqnum()] != null) {
                    toLayer3(A, recieveBuffer[packet.getSeqnum()]);
                    timers[packet.getSeqnum()][2] = 1.0;
                }
            }
        }   
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
        retransmissionsByA += 1;
        lostPackets += 1;
        System.out.println("ATO");
        
        nRet = -1;
        for (int x=0; x < WindowSize; x++) {
            if(sendBuffer[x] != null) {
                nRet = x;
                System.out.println(nRet);
                //break;
            }
        }
        if(nRet != -1) {
            System.out.println("Rtransmitting packet " + sendBuffer[nRet]);
            timers[nRet][2] = 1.0;
            toLayer3(A, sendBuffer[nRet]);
        }
    } 
    
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {   
        sendBuffer = new Packet[WindowSize];
        timers = new double[WindowSize][3];
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
        int cSum = calculateChecksum(packet);
        if (cSum == packet.getChecksum()) {
            currentAckNumber = packet.getSeqnum()+1;
            if (currentAckNumber >= LimitSeqNo) {
                currentAckNumber = currentAckNumber % LimitSeqNo;
            }

            Packet ackPacket = new Packet(packet.getSeqnum(), currentAckNumber, 0, packet.getPayload());
            ackPacket.setChecksum(calculateChecksum(ackPacket));

            System.out.println("B Receives " + ackPacket);

            recieveBuffer[packet.getSeqnum()] = ackPacket;

            toLayer3(B, recieveBuffer[packet.getSeqnum()]);
        }
        else {
            System.out.println("BIn corrupted packet detected " + packet);
            corruptedPackets +=1;
            retransmissionsByA +=1;
            
            if (packet.getSeqnum() > LimitSeqNo) {
                if (packet.getAcknum()-1 != -1 && sendBuffer[packet.getAcknum()] != null) {
                    toLayer3(A, sendBuffer[packet.getAcknum()]);
                    timers[packet.getAcknum()][2] = 1.0;
                }
            }
            else {
                if (sendBuffer[packet.getSeqnum()] != null) {
                    toLayer3(A, sendBuffer[packet.getSeqnum()]);
                    timers[packet.getSeqnum()][2] = 1.0;
                }
            }
        }
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        recieveBuffer = new Packet[WindowSize];
    }

    // checksum routine
    public static int calculateChecksum(Packet packet) {
        //generate checksum from payload, ackNum, and seqNum
        int checksum = (packet.getPayload().charAt(0)*20) + packet.getAcknum() + packet.getSeqnum();
        return checksum;
    }

    // Use to print final statistics
    protected void Simulation_done()
    {
    	// TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO NOT CHANGE THE FORMAT OF PRINTED OUTPUT
    	System.out.println("\n\n===============STATISTICS=======================");
    	System.out.println("Number of original packets transmitted by A:" + transmittedByA);
    	System.out.println("Number of retransmissions by A:" + retransmissionsByA);
    	System.out.println("Number of data packets delivered to layer 5 at B:" + deliveredToLayer5);
    	System.out.println("Number of ACK packets sent by B:" + acksSentByB);
    	System.out.println("Number of corrupted packets:" + corruptedPackets);
    	System.out.println("Ratio of lost packets:" + (double)(lostPackets) / (double)((transmittedByA + retransmissionsByA)));
    	System.out.println("Ratio of corrupted packets:" + (double)(corruptedPackets) / (double)((transmittedByA + retransmissionsByA)));
    	System.out.println("Average RTT:" + (double)RTT/(double)transmittedByA);
    	System.out.println("Average communication time:" + (double)communicationTime/(double)(transmittedByA + retransmissionsByA));
    	System.out.println("==================================================");

    	// PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
    	System.out.println("\nEXTRA:");
    	// EXAMPLE GIVEN BELOW
    	//System.out.println("Example statistic you want to check e.g. number of ACK packets received by A :" + "<YourVariableHere>"); 
    }	

}

