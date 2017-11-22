import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

public class UDPservice {

	DatagramSocket server = null;
	ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<byte[]>(100);

	public static void main(String[] args) {
		UDPservice udPservice = new UDPservice();
		udPservice.star();
	}

	private void star() {
		try {
			server = new DatagramSocket(8765);
			server.setSendBufferSize(1024 * 1024 * 5);
			server.setReceiveBufferSize(1024 * 1024 * 5);
			System.out.println("打开");
		} catch (SocketException e) {
			e.printStackTrace();
		}

		//接受包
		new Thread(new Runnable() {
			public void run() {
				byte[] recvbuf = new byte[548];
				DatagramPacket recvPacket = new DatagramPacket(recvbuf,recvbuf.length);
				byte[] bytes;
				while (true) {
					try {
						server.receive(recvPacket);
					} catch (IOException e) {
						e.printStackTrace();
					}
					bytes = new byte[548];
					System.arraycopy(recvPacket.getData(), 0, bytes, 0, 548);
					queue.add(bytes);
				}
			}
		}).start();

		//发送包
		new Thread(new Runnable() {
			public void run() {
				DatagramPacket sendPacket = null;
				try {
					sendPacket = new DatagramPacket(new byte[10], 0,InetAddress.getByName("192.168.2.116"), 8765);
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}
				byte[] bytes;
				byte[] b;
				
				//计算丢包率
				int vdnum = 0;
				int vcnum = 0;
				while (true) {
					if (queue.size() > 0) {
						b = queue.poll();
						
						//计算丢包率
						if (b[0] == 1) {
							 vdnum++;
					            if ((vdnum % 500) == 0) {//每500个包输出一次
					                System.out.println("视频丢包率：" + 
					            ((float)byte_to_int(new byte[] { b[4], b[5],b[6], b[7] }) - (float) vdnum) * (float) 100 / (float)byte_to_int(new byte[] { b[4], b[5],b[6], b[7] }) + "%");
					            }
						}else if (b[0] == 0) {
							vcnum++;
							if ((vcnum % 100) == 0) {//每100个包输出一次
				                System.out.println("音频丢包率：" + 
							((float)byte_to_int(new byte[] { b[4], b[5],b[6], b[7] }) - (float) vcnum) * (float) 100 / (float)byte_to_int(new byte[] { b[4], b[5],b[6], b[7] }) + "%");
				            }
						}
						
						bytes = new byte[byte_to_short(b[2], b[3]) + 12];//12是数据前面协议字节长度，如果修改协议记得修改头长度
						System.arraycopy(b, 0, bytes, 0, bytes.length);
						try {
							sendPacket.setData(bytes);
							server.send(sendPacket);
							// System.out.println(bytes.length);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
	}

	// -----------------------
	// -----------------------
	public short byte_to_short(byte b1, byte b2) {
		return (short) ((b1 & 0xff) << 8 | (b2 & 0xff));
	}

	public int byte_to_int(byte[] bytes) {
		return Integer.parseInt(new BigInteger(bytes).toString(10));// 这里的1代表正数,10表示10进制
	}
}














