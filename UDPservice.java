import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPservice {

	private DatagramSocket server = null;
	private ArrayBlockingQueue<byte[]> udpQueue = new ArrayBlockingQueue<byte[]>(100);
	private ExecutorService executorService;
	
	public static void main(String[] args) {
		UDPservice udPservice = new UDPservice();
		udPservice.star();
	}

	private void star() {
		try {
			server = new DatagramSocket(8765);
			server.setSendBufferSize(1024 * 1024);
			server.setReceiveBufferSize(1024 * 1024);
			System.out.println("打开");
		} catch (SocketException e) {
			e.printStackTrace();
		}
		executorService = Executors.newFixedThreadPool(2);
		
		executorService.execute(new Runnable() {
			public void run() {
				byte[] recvbuf = new byte[1024];
				DatagramPacket recvPacket = new DatagramPacket(recvbuf,recvbuf.length);
				//计算丢包率
				int vdnum = 0;
				int vcnum = 0;
				while (true) {
					try {
						server.receive(recvPacket);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					if (recvbuf[0] == 1) {
				            if ((vdnum % 500) == 0) {//每500个包输出一次
				                System.out.println("视频丢包率：" + 
				            ((float)byte_to_int(recvbuf[1], recvbuf[2],recvbuf[3], recvbuf[4]) 
				            		- (float) vdnum) * (float) 100 / (float)byte_to_int(recvbuf[1], recvbuf[2],recvbuf[3], recvbuf[4]) + "%");
				            }
				            vdnum++;
					}else if (recvbuf[0] == 0) {
						if ((vcnum % 50) == 0) {//每50个包输出一次
			                System.out.println("音频丢包率：" + 
						((float)byte_to_int(recvbuf[1], recvbuf[2],recvbuf[3], recvbuf[4]) 
								- (float) vcnum) * (float) 100 / (float)byte_to_int(recvbuf[1], recvbuf[2],recvbuf[3], recvbuf[4]) + "%");
			            }
						vcnum++;
					}

					if(udpQueue.size()>98){
						udpQueue.poll();
					}
					udpQueue.offer(Arrays.copyOfRange(recvbuf, 0, recvPacket.getLength()));
				}
			}
		});
		
		executorService.execute(new Runnable() {
			
			public void run() {
				// TODO Auto-generated method stub
				DatagramPacket sendPacket = null;
				try {
					 sendPacket = new DatagramPacket(new byte[10], 0,InetAddress.getByName("192.168.2.116"), 8765);
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}
				byte[] data;
				while(true){
					try {
						data = udpQueue.take();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						break;
					}
					sendPacket.setData(data);
					try {
						server.send(sendPacket);
						Thread.sleep(1);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});

	}

	// -----------------------

    public  int byte_to_int(byte b1, byte b2, byte b3, byte b4) {
        return (((b1 & 0xFF) << 24) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 8) | (b4 & 0xFF));
    }
}
