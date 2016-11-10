package util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

    public Log(String message) {
        if(!write(message)) {
            System.err.println("ERROR: Logfile not writeable.");
        }
    }

    private boolean write(String s) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(System.getProperty("user.dir") + File.separator + "log.txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            out.println("[" + getDateString() + "] " + s);
            out.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private String getDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        return sdf.format(new Date());
    }

}
