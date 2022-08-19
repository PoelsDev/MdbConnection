/**
 * The MdbConnection class handles serial communication with a Qibixx MDB Interface in Cashless Master mode with one reader (slave device).
 * For every vend, a new instance of this class should be made, having two instances at the same time risks calling handleVend() at the same time.
 * This can cause problems because you cannot open the same serial port twice.
 */

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

    /**
     * Handles a vend
     * @param amount the price of the item
     * @param id the identifier of the item
     * @return did the vend succeed
     */

    public boolean handleVend(float amount, int id){
        try{
            // Open port
            serialPort.openPort();

            // Configure interface mode, start polling and request a vend
            String disableCashlessMaster = "D,0";
            String alwaysIdle = "D,2";
            String pollReader = "D,READER,1";
            String requestVend = "D,REQ," + amount + "," + id;

            byte[] writeBuffer0= disableCashlessMaster.getBytes();
            byte[] writeBuffer1= alwaysIdle.getBytes();
            byte[] writeBuffer2= pollReader.getBytes();
            byte[] writeBuffer3= requestVend.getBytes();

            // Enable cashless master mode on interface
            serialPort.writeBytes(writeBuffer1, writeBuffer1.length);
            String currentStatus = this.getTerminalStatus();
            if (currentStatus.contains("D,ERR,\"cashless master is on\"")) {
                serialPort.writeBytes(writeBuffer0, writeBuffer0.length);
                serialPort.writeBytes(writeBuffer1, writeBuffer1.length);
            }

            // Enable polling to reader (slave device)
            currentStatus = this.getTerminalStatus();
            if (currentStatus.contains("d,STATUS,INIT,1")) {
                serialPort.writeBytes(writeBuffer2, writeBuffer1.length);
            } else {
                return false;
            }

            // Check if reader (slave device) is ready
            currentStatus = this.getTerminalStatus();
            if(currentStatus.contains("d,STATUS,IDLE")){
                // Request vend
                serialPort.writeBytes(writeBuffer3, writeBuffer1.length);
            } else {
                return false;
            }

            // Check vend result
            currentStatus = this.getTerminalStatus();
            if (!currentStatus.contains(String.format("d,STATUS,RESULT,1,%.2f",amount))){
                return false;
            }

            // Check product dispense result
            if (currentStatus.contains("d,END,-1")){
                return false;
            }

            serialPort.closePort();

            // Check if reader (slave device) is ready again
            return currentStatus.contains("d,STATUS,IDLE");

        } catch (Exception e){
            e.printStackTrace();
            serialPort.closePort();

            return false;
        }
    }

    /**
     * Reads the serial responses of the MDB Interface
     * @return a string with a response
     */

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