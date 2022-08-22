/**
 * The MdbConnection class handles serial communication with a Qibixx MDB Interface in Cashless Master mode with one reader (slave device).
 * For every vend, a new instance of this class should be made, having two instances at the same time risks calling handleVend() at the same time.
 * This can cause problems because you cannot open the same serial port twice.
 */

package be.alberts;

import com.fazecast.jSerialComm.SerialPort;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class MdbConnection {

    private SerialPort serialPort;

    public MdbConnection(int comPort, int baudRate) {
        int maxBaudRate = Math.max(baudRate, 115200);

        // Initialize serial connection
        SerialPort[] comPorts = SerialPort.getCommPorts();
        serialPort = comPorts[comPort];
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
            String disableCashlessMaster = "D,0\n";
            String alwaysIdle = "D,2\n";
            String pollReader = "D,READER,1\n";
            String requestVend = "D,REQ," + amount + "," + id + "\n";
            String endVend = "D,END," + id + "\n";

            byte[] disableCashlessMasterBuffer = disableCashlessMaster.getBytes();
            byte[] alwaysIdleBuffer = alwaysIdle.getBytes();
            byte[] pollReaderBuffer = pollReader.getBytes();
            byte[] requestVendBuffer = requestVend.getBytes();
            byte[] endVendBuffer = endVend.getBytes();

            // Enable cashless master mode on interface
            serialPort.writeBytes(disableCashlessMasterBuffer, disableCashlessMasterBuffer.length);
            serialPort.writeBytes(alwaysIdleBuffer, alwaysIdleBuffer.length);

            // Enable polling to reader (slave device)
            String currentStatus = this.getTerminalStatus();
            while(true){
                if (currentStatus.contains("d,STATUS,INIT,0")) {
                    serialPort.writeBytes(pollReaderBuffer, pollReaderBuffer.length);
                    break;
                } else {
                    TimeUnit.MILLISECONDS.sleep(100);
                    currentStatus = this.getTerminalStatus();
                }
            }

            // Check if reader (slave device) is ready
            while(true){
                if(currentStatus.contains("d,STATUS,CREDIT")){
                    // Request vend
                    serialPort.writeBytes(requestVendBuffer, requestVendBuffer.length);
                    break;
                } else {
                    TimeUnit.MILLISECONDS.sleep(100);
                    currentStatus = this.getTerminalStatus();
                }
            }

            // Check vend result
            while(true){
                if(currentStatus.contains("d,STATUS,RESULT,1")){
                    // End vend and close serial port
                    serialPort.writeBytes(endVendBuffer, endVendBuffer.length);
                    serialPort.closePort();
                    return true;

                } else if (currentStatus.contains("d,STATUS,RESULT,-1")){
                    return false;

                } else {
                    TimeUnit.MILLISECONDS.sleep(100);
                    currentStatus = this.getTerminalStatus();
                }
            }

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
            serialPort.readBytes(readBuffer, readBuffer.length);
            for (byte b : readBuffer) {
                status.append((char) b);
            }
        }

        if(!status.toString().isEmpty()) System.out.println(status.toString());
        return status.toString();
    }
}