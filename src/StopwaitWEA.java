public class StopwaitWEA
    extends datalink.Protocol
{
    datalink.Packet buffer;      // copy of last packet sent
    int frameToSend;    // sequence number to use
                        // for next frame transmitted
    int frameExpected;  // sequence number expected
    double timer;       // time out value

    boolean waiting;    // a packet is waiting to be sent
    boolean ackWaiting;	// An ack frame is waiting to be sent

    public StopwaitWEA( int windowsize, double timer)
    {
	super( windowsize, timer);
	frameToSend = 0;  // initialise sequence numbers
	frameExpected = 0;
	waiting = false;
	this.timer = timer;
    }

    public void FrameArrival( Object frame)
    {   // Object frame must be cast to DLL_Frame
	DLL_Frame f = (DLL_Frame) frame;

	// check that it is the one that is expected
	if (f.sequence == frameExpected)
	    {
		sendPacket(f.info); // valid frame is passed
		                    // to network layer
		frameExpected = 1 - frameExpected; // flip bit
		
		// Send explicit acknowledgement
		if(isChannelIdle()){
			DLL_Frame ackF = new DLL_Frame(frameToSend, 1-frameExpected);
			sendAckFrame(ackF);
			ackWaiting = false;
		}else
			ackWaiting = true;
	    }

	if( f.acknowledgment == frameToSend)
	    {   // acknowledgment has arrived
		stopTimer(0);           // cancel timer
		enableNetworkLayer();   // allow new packets
		frameToSend = 1-frameToSend;
	    }
    }

    public void PacketArrival( datalink.Packet p)
    {
	DLL_Frame f;
	buffer = p;
	if ( isChannelIdle() )
	    {
		f = new DLL_Frame(frameToSend, 1-frameExpected, buffer);
		sendFrame(f);        // transmit it
		startTimer(0, timer); // start timer
		waiting = false;
	    }
	else
		waiting = true;
	disableNetworkLayer();
    }

    public void TimeOut( int code)
    {
	DLL_Frame f;
	if ( isChannelIdle())
	    {
		// a frame has not been ACKed in time,
		// so re-send the outstanding frame
		f = new DLL_Frame( frameToSend,
				   1-frameExpected,
				   buffer);
		sendFrame( f);
		startTimer(0, timer);
	    }
	else
	    waiting = true;
    }

    public void CheckSumError()
    {  // ignore check sum errors and allow frames to time out
    }

    public void ChannelIdle()
    {
	DLL_Frame f;
	if ( waiting )
	    {
		f = new DLL_Frame( frameToSend,
				   1-frameExpected,
				   buffer);
		sendFrame( f);
		startTimer(0, timer);
		waiting = false;
	    }
    if(ackWaiting)
    	{
	    	DLL_Frame ackF = new DLL_Frame(frameToSend, 1-frameExpected);
			sendAckFrame(ackF);
			ackWaiting = false;
    	}
    }
}


