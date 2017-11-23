import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPservice {

	DatagramSocket server = null;
	DatagramPacket sendPacket = null;
	
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
		
		
		try {
			sendPacket = new DatagramPacket(new byte[10], 0,InetAddress.getByName("192.168.2.116"), 8765);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		
		new Thread(new Runnable() {
			public void run() {
				byte[] recvbuf = new byte[1024];
				DatagramPacket recvPacket = new DatagramPacket(recvbuf,recvbuf.length);
				byte[] b;
				//计算丢包率
				int vdnum = 0;
				int vcnum = 0;
				while (true) {
					try {
						server.receive(recvPacket);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					b=copybyte(recvPacket.getData());
					
					if (b[0] == 1) {
						 vdnum++;
				            if ((vdnum % 500) == 0) {//每500个包输出一次
				                System.out.println("视频丢包率：" + 
				            ((float)byte_to_int(b[1], b[2],b[3], b[4]) - (float) vdnum) * (float) 100 / (float)byte_to_int(b[1], b[2],b[3], b[4]) + "%");
				            }
					}else if (b[0] == 0) {
						vcnum++;
						if ((vcnum % 50) == 0) {//每50个包输出一次
			                System.out.println("音频丢包率：" + 
						((float)byte_to_int(b[1], b[2],b[3], b[4]) - (float) vcnum) * (float) 100 / (float)byte_to_int(b[1], b[2],b[3], b[4]) + "%");
			            }
					}

					try {
						sendPacket.setData(b);
						server.send(sendPacket);
						// System.out.println(bytes.length);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();

	}

	/*
	 * 裁剪数组
	 */
	protected byte[] copybyte(byte[] data) {
		byte [] bytes; 
		int lengthnum;
		
		if(data[0] == 1){
			lengthnum = byte_to_short(data[10], data[11]) + 12;
			bytes = new byte[lengthnum];
            System.arraycopy(data, 0, bytes, 0, lengthnum);
            
		}else{
			lengthnum = byte_to_short(data[9], data[10]) + 11;
			for (int i = 0; i < 4; i++) {
				 lengthnum = lengthnum + byte_to_short(data[lengthnum + 4], data[lengthnum + 5]) + 6;//记录偏移量
			}
			bytes = new byte[lengthnum];
            System.arraycopy(data, 0, bytes, 0, lengthnum);
            
		}
		
		return bytes;
	}

	// -----------------------

    public  short byte_to_short(byte b1, byte b2) {
        return (short) ((b1 & 0xff) << 8 | (b2 & 0xff));
    }
    public  int byte_to_int(byte b1, byte b2, byte b3, byte b4) {
        return (((b1 & 0xFF) << 24) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 8) | (b4 & 0xFF));
    }
}
