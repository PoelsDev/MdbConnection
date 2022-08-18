package be.alberts;

import com.fazecast.jSerialComm.SerialPort;

public class MdbConnection {

    private SerialPort serialPort;

    public MdbConnection(int comPort, int baudRate) {
        int maxComPort = Math.max(comPort, 1);
        int maxBaudRate = Math.max(baudRate, 115200);

        // Initialize serial connection
        SerialPort[] comPorts = SerialPort.getCommPorts();
        serialPort = comPorts[maxComPort];
        serialPort.setBaudRate(maxBaudRate);
    }

    public boolean handleVend(float amount, int id){
        try{
            // Open port
            serialPort.openPort();

            // Configure interface mode, start polling and request a vend
            String alwaysIdle = "D,2";
            String pollReader = "D,READER,1";
            String requestVend = "D,REQ," + amount + "," + id;

            byte[] writeBuffer1= alwaysIdle.getBytes();
            byte[] writeBuffer2= pollReader.getBytes();
            byte[] writeBuffer3= requestVend.getBytes();

            serialPort.writeBytes(writeBuffer1, writeBuffer1.length);
            String currentStatus = this.getTerminalStatus();
            if (currentStatus.equals("D,ERR,\"cashless master is on\"")) {
                // Restart Master Device
            }

            serialPort.writeBytes(writeBuffer2, writeBuffer1.length);
            serialPort.writeBytes(writeBuffer3, writeBuffer1.length);

            return true;

        } catch (Exception e){
            e.printStackTrace();
            serialPort.closePort();

            return false;
        }
    }

    private String getTerminalStatus(){
        StringBuilder status = new StringBuilder();
        while (serialPort.bytesAvailable() > 0) {
            byte[] readBuffer = new byte[serialPort.bytesAvailable()];
            for (byte b : readBuffer) {
                status.append((char) b);
            }
        }

        return status.toString();
    }
}
