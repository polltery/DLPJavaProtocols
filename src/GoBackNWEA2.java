import datalink.*;

/*
  A go-back n type sliding window protocol
  */

public class GoBackNWEA2 extends Protocol
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
    
    int acksToSend;
    int lastFrameAck;
    boolean sentLastAck = false;

    public GoBackNWEA2(int windowSize, double timer)
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
	DLL_Frame f = (DLL_Frame) frame;
	/* a frame has arrived from the physical layer */

	/* check that it is the one that is expected */
	if (f.sequence == nextSequenceNumberExpected)
	    {
		sendPacket(f.info); /* valid frame, so send it */
	 	                    /* to the network layer */
		nextSequenceNumberExpected = inc( nextSequenceNumberExpected);
		acksToSend++;
	    }
	/* if frame n is ACKed then that implies n-1,n-2 etc have also been */
	/* ACKed, so stop associated timers. (reading of acks)                                */
	lastFrameAck = f.acknowledgment;
	while ( between( firstUnAcknowledged,
			 f.acknowledgment,
			 nextBufferToSend) )
	    {
		numberOfPacketsStored--;
		stopTimer(firstUnAcknowledged);
		firstUnAcknowledged = inc( firstUnAcknowledged);
		acksToSend = 0;
	    }
 	if ( numberOfPacketsStored < windowSize )
	    enableNetworkLayer();
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

    public void PacketArrival( Packet p)
    {
	DLL_Frame f;
	buffer[firstFreeBufferIndex] = p;
	numberOfPacketsStored++;		/* buffer packet */
	if ( numberOfPacketsStored >= windowSize )
	    disableNetworkLayer();

	if ( isChannelIdle() )
	    {
		acksToSend--;
		transmit_frame( nextBufferToSend);
		nextBufferToSend = inc( nextBufferToSend);
	    }
		if(acksToSend > 0){
			transmit_ackFrame();
			acksToSend--;
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
    	if(isChannelIdle())
    	transmit_nackFrame(nextSequenceNumberExpected);
    }

    public void ChannelIdle()
    {
	if ( nextBufferToSend != firstFreeBufferIndex )
	    {
		transmit_frame( nextBufferToSend);
		nextBufferToSend = inc( nextBufferToSend);
	    }
    }
    
    private int dec ( int a)
    {  /* increment modulo maximum_sequence_number + 1 */
	a--;
	a %= maximumSequenceNumber+1;
	return a;
    }

    private void transmit_frame( int sequenceNumber)
    {
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
    
    private void transmit_ackFrame(){
    	int acknowledgement;
    	acknowledgement = (nextSequenceNumberExpected+maximumSequenceNumber)
    		    % (maximumSequenceNumber+1);
    	sendAckFrame( new DLL_Frame(acknowledgement));
    }
    
    private void transmit_nackFrame( int sequenceNumber){
    	sendNackFrame(new DLL_Frame(sequenceNumber, firstUnAcknowledged));
    }
}
