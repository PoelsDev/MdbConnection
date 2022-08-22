package be.alberts;
import com.fazecast.jSerialComm.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        MdbConnection mdbConnection = new MdbConnection(0, 115200);
        boolean result = mdbConnection.handleVend(0.01f, 1);
        if (result) {
            System.out.println("Vend Success!");
        } else {
            System.out.println("Vend Failed!");
        }
    }
}
