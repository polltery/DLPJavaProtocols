import datalink.*;

public class CP_GoBackN extends Protocol {

	// Departure
	Packet[] buffer;
	int bufferIndexOpenForPacketStorage; 
	int bufferIndexToSend; 	// same as frameToSend
	int acknowledgment;
	
	// protocol info
	int numberOfPacketsStored;
	int numberOfAcksQueued;
	
	// Traffic control
	boolean channelInUse;
	
	// Arrivals
	int priorityBufferWaitingForAck;
	int expectedFrameSequence;
	
	// Configs
	double timer;
	int windowSize;
	boolean viewDebugMessages = true;
	
	// Default Constructor for the protocol
	public CP_GoBackN(int windowSize, double timerValue) {
		super(windowSize, timerValue);
		// TODO Auto-generated constructor stub
		// Departures
		bufferIndexOpenForPacketStorage = 0;
		bufferIndexToSend = 0;
		buffer = new Packet[windowSize+1];
		priorityBufferWaitingForAck = 0;
		// Arrivals
		expectedFrameSequence = 0;
		
		// configs
		this.timer = timerValue;
		this.windowSize = windowSize;
	}

	@Override
	public void ChannelIdle() {
		// TODO Auto-generated method stub
		channelInUse = false;
		// Send any packets that are queued
		if(bufferIndexToSend != bufferIndexOpenForPacketStorage){
			// Since we are sending the ack with this, lets reduce the number of acks that are queued
			if(numberOfAcksQueued > 0)
				numberOfAcksQueued--;
			acknowledgment = (expectedFrameSequence+windowSize)%(windowSize+1);
			sendFrame(new DataLinkFrame(1,bufferIndexToSend,acknowledgment,buffer[bufferIndexToSend]));
			startTimer(bufferIndexToSend,timer);
			bufferIndexToSend = inc(bufferIndexToSend);
			channelInUse = true;
		}
		// Send any acks that are queued when free
		if(numberOfAcksQueued > 0 && channelInUse == false){
			acknowledgment = (expectedFrameSequence+windowSize)%(windowSize+1);
			sendAckFrame(new DataLinkFrame(0,acknowledgment));
		}
	}

	@Override
	public void CheckSumError() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void FrameArrival(Object frame) {
		// TODO Auto-generated method stub
		// TODO Check frame contents
		
		DataLinkFrame f = (DataLinkFrame) frame;
		
		switch(f.type){
		case -1:
			// TODO negative ack
			break;
		case 0:
			// Seems like we have got acknowledgement.. lets check.
			if(viewDebugMessages == true)
				System.out.println(f.acknowledgment + " " +priorityBufferWaitingForAck);
			// Checking acks
			if(f.acknowledgment == priorityBufferWaitingForAck){
				stopTimer(priorityBufferWaitingForAck);
				priorityBufferWaitingForAck = inc(priorityBufferWaitingForAck);
				numberOfPacketsStored--;
				if ( numberOfPacketsStored < windowSize )
				    enableNetworkLayer();
			}
			break;
		case 1:
			// We have recieved the right packet
			if(f.sequenceNumber == expectedFrameSequence){
				// send packet to network layer
				sendPacket(f.info);
				// Let's change our expectations, and wait for the next sequence
				if(viewDebugMessages == true)
					System.out.println("Expected Sequence: "+expectedFrameSequence+"->"+inc(expectedFrameSequence));
				expectedFrameSequence = inc(expectedFrameSequence);
				// let's see if the other protocol has recieved our packets or not
				if(f.acknowledgment == priorityBufferWaitingForAck){
					stopTimer(priorityBufferWaitingForAck);
					priorityBufferWaitingForAck = inc(priorityBufferWaitingForAck);
					numberOfPacketsStored--;
					if ( numberOfPacketsStored < windowSize )
					    enableNetworkLayer();
				}
				// Since we have recieved a correct packet, lets send an ack back
				
				if(isChannelIdle()){
					if(viewDebugMessages = true){
						System.out.println("ack sent upon packet arrival of"+f.sequenceNumber);
						System.out.print(acknowledgment);
						System.out.print("->"+(expectedFrameSequence+windowSize)%(windowSize+1));
						System.out.println(" = ("+expectedFrameSequence+"+"+windowSize+")%("+windowSize+"+1)");
					}
					acknowledgment = (expectedFrameSequence+windowSize)%(windowSize+1);
					sendAckFrame(new DataLinkFrame(0,acknowledgment));
				}else{
					numberOfAcksQueued++;
				}
			}
			break;
		}
	}

	@Override
	public void PacketArrival(Packet newPacket) {
		// TODO Auto-generated method stub
		
		// Add the packets to the buffer
		buffer[bufferIndexOpenForPacketStorage] = newPacket;
		numberOfPacketsStored++;
		if(numberOfPacketsStored >= windowSize)
			disableNetworkLayer();
		
		// Send the buffer index that is to be sent, if the channel is idle
		if(isChannelIdle()){
			// Since we are sending the ack with this, lets reduce the number of acks that are queued
			if(numberOfAcksQueued > 0)
				numberOfAcksQueued--;
			acknowledgment = (expectedFrameSequence+windowSize)%(windowSize+1);
			sendFrame(new DataLinkFrame(1,bufferIndexToSend,acknowledgment,buffer[bufferIndexToSend]));
			startTimer(bufferIndexToSend,timer);
			bufferIndexToSend = inc(bufferIndexToSend);
		}
		
		// This allows the bufferIndex to go from 0 -> 1 -> 2 -> .. -> windowSize -> 0
		bufferIndexOpenForPacketStorage = inc(bufferIndexOpenForPacketStorage);
	}

	@Override
	public void TimeOut(int codeNumber) {
		// TODO Auto-generated method stub
		bufferIndexToSend = priorityBufferWaitingForAck;
		if(isChannelIdle()){
			// Since we are sending the ack with this, lets reduce the number of acks that are queued
			if(numberOfAcksQueued > 0)
				numberOfAcksQueued--;
			acknowledgment = (expectedFrameSequence+windowSize)%(windowSize+1);
			sendFrame(new DataLinkFrame(1,bufferIndexToSend,acknowledgment,buffer[bufferIndexToSend]));
			startTimer(bufferIndexToSend,timer);
			bufferIndexToSend = inc(bufferIndexToSend);
		}
	}
	
	private int inc(int x){
		x = (x+1)%(windowSize+1);
		return x;
	}
	
	class DataLinkFrame{
		int type;
		int sequenceNumber;
		int acknowledgment;
		Packet info;
		
		DataLinkFrame(int t, int s, int a, datalink.Packet p) {
			// TODO Auto-generated constructor stub
			type = t;
			// 0 ack frame, -1 nack frame, 1 ack+dataframe
			sequenceNumber = s;
			acknowledgment = a;
			info = p;
		}
		
		// Ack Frames
		DataLinkFrame(int t, int a) {
			// TODO Auto-generated constructor stub
			type = t;
			// 0 ack frame, -1 nack frame, 1 ack+dataframe
			acknowledgment = a;
		}
	}

}
