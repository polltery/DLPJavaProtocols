import datalink.Packet;
import datalink.Protocol;

/*
  A go-back n type sliding window protocol
  */

public class GoBackNWEA3 extends Protocol
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
    
    boolean ackInQueue;			// is there any Acknowledgement waiting to be sent?
    boolean channelBusy;		// Is the channel busy? true when channel gets busy by calling channel idle method
    
    public GoBackNWEA3(int windowSize, double timer)
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
    ackInQueue = false;
    channelBusy = false;
    }

    public void FrameArrival( Object frame)
    {
	DLL_Frame f = (DLL_Frame) frame;
	/* a frame has arrived from the physical layer */

	/* check that it is the one that is expected */
	if (f.sequence == nextSequenceNumberExpected)
	    {
		sendPacket(f.info); /* valid frame, so send it */
	 	                    /* to the network layer */
		nextSequenceNumberExpected = inc( nextSequenceNumberExpected);
		
		// Send an ack when we recieve correct packet
		if(isChannelIdle()){
			int a = (nextSequenceNumberExpected+maximumSequenceNumber)
				    % (maximumSequenceNumber+1);
			sendAckFrame(new DLL_Frame(-1,a));
		}else{
			// put the ack in a queue (Check channelIdle() method)
			ackInQueue = true;
		}
	    
	    }
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
    	// TODO add negative acknowledgement
    }

    public void ChannelIdle()
    {
    channelBusy = false;
	if ( nextBufferToSend != firstFreeBufferIndex )
	    {
		transmit_frame( nextBufferToSend);
		nextBufferToSend = inc( nextBufferToSend);
	    channelBusy = true;
	    }
	// send any acks that are in queue while the channel is not busy
	if (ackInQueue == true && channelBusy == false){
		int a = (nextSequenceNumberExpected+maximumSequenceNumber)
			    % (maximumSequenceNumber+1);
		sendAckFrame(new DLL_Frame(-1,a));
		ackInQueue = false;
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

    private void transmit_frame( int sequenceNumber)
    {
    
    // we won't need the ack in queue since it will be piggybacked
    if(ackInQueue)
    	ackInQueue = false;
    	
	int acknowledgement;
	/* piggyback acknowledge of last frame receieved */
	acknowledgement = (nextSequenceNumberExpected+maximumSequenceNumber)
	    % (maximumSequenceNumber+1);
	/* send it to physical layer */
	sendFrame( new DLL_Frame( sequenceNumber,
				 acknowledgement,
				 buffer[sequenceNumber]));
	startTimer( sequenceNumber, timer);
    }
}
