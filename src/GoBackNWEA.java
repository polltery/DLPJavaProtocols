import java.util.Queue;

import datalink.Packet;
import datalink.Protocol;

/*
  A go-back n type sliding window protocol
  */

public class GoBackNWEA extends Protocol
{
    int nextBufferToSend;        // buffer to be sent when channel is idle
    int firstFreeBufferIndex;    // buffer to getin which to store next packet
    int nextSequenceNumberExpected;  // sequence number expected
    int firstUnAcknowledged;     // last unacknowledged frame
    final int maximumSequenceNumber;
    int numberOfPacketsStored;
    final int windowSize;

    Packet[] buffer;
    double timer;

    boolean ackWaiting;
    Queue ackFramesQueue;
    
    public GoBackNWEA(int windowSize, double timer)
    {
	super( windowSize, timer);
	numberOfPacketsStored = 0;
	nextBufferToSend = 0;
	firstFreeBufferIndex= 0;
	nextSequenceNumberExpected = 0;
	firstUnAcknowledged = 0;
	maximumSequenceNumber = windowSize;
	this.windowSize = windowSize;
	this.timer = timer;
	buffer = new Packet[windowSize+1];
    }

    public void FrameArrival( Object frame)
    {
	DLLFrame f = (DLLFrame) frame;
	/* a frame has arrived from the physical layer */
	
	// If frame has packet info
	if(f.type == 1 || f.type == 2){
		/* check that it is the one that is expected */
		if (f.sequence == nextSequenceNumberExpected)
		    {
			sendPacket(f.info); /* valid frame, so send it */
		 	                    /* to the network layer */
			nextSequenceNumberExpected = inc( nextSequenceNumberExpected);
		    }
			if(isChannelIdle()){
				transmit_ackFrame();
			}else{
				ackWaiting = true;
			}
	}else{
		// If frame has acks
		/* if frame n is ACKed then that implies n-1,n-2 etc have also been */
		/* ACKed, so stop associated timers.                                 */
		while ( between( firstUnAcknowledged,
				 f.acknowledgment,
				 nextBufferToSend) )
		    {
			numberOfPacketsStored--;
			stopTimer(firstUnAcknowledged);
			firstUnAcknowledged = inc( firstUnAcknowledged);
		    }
	 	if ( numberOfPacketsStored < windowSize )
		    enableNetworkLayer();
	}
    }

    public void PacketArrival( Packet p)
    {
	DLL_Frame f;
	buffer[firstFreeBufferIndex] = p;
	numberOfPacketsStored++;		/* buffer packet */
	if ( numberOfPacketsStored >= windowSize )
	    disableNetworkLayer();

	if ( isChannelIdle() )
	    {
		transmit_frame( nextBufferToSend);
		nextBufferToSend = inc( nextBufferToSend);
	    }
	firstFreeBufferIndex = inc( firstFreeBufferIndex);
    }

    public void TimeOut( int code)
    {
	nextBufferToSend = firstUnAcknowledged;
	if ( isChannelIdle() )
	    {
		transmit_frame( nextBufferToSend);
		nextBufferToSend = inc( nextBufferToSend);
	    }
    }

    public void CheckSumError()
    {
    }

    public void ChannelIdle()
    {
	if(ackWaiting == true)
		{
			//transmit_ackFrame();
			ackWaiting = false;
		}
	if ( nextBufferToSend != firstFreeBufferIndex )
	    {
		transmit_frame( nextBufferToSend);
		nextBufferToSend = inc( nextBufferToSend);
	    }
    }

    private boolean between( int a, int b, int c)
    {  /* calculate if a<=b<c circularly */
	if(((a<=b) && (b<c))
	   || ((c<a) && (a<=b))
	   || ((b<c) && (c<a)))
	    return true;
	else
	    return false;
    }

    private int inc ( int a)
    {  /* increment modulo maximum_sequence_number + 1 */
	a++;
	a %= maximumSequenceNumber+1;
	return a;
    }

    // Transmit data
    private void transmit_frame( int sequenceNumber)
    {
	/* send it to physical layer */
	sendFrame( new DLLFrame(1,sequenceNumber, 0, buffer[sequenceNumber]));
	startTimer( sequenceNumber, timer);
    }
    
    // Transmit Acks
    private void transmit_ackFrame(){
    	int acknowledgement;
    	acknowledgement = (nextSequenceNumberExpected+maximumSequenceNumber)
    	    % (maximumSequenceNumber+1);
    	sendAckFrame(new DLLFrame(0,0,acknowledgement,null));
    }
}

// Our DLLFrame for this class
class DLLFrame {
	int type;							// 0 for acks, 1 for packet info
    int sequence;
    int acknowledgment;
    public datalink.Packet info;
    
    DLLFrame( int t, int s, int a, datalink.Packet p){
    	type = t;
    	sequence = s;
    	acknowledgment = a;
    	info = p;
    }
}
