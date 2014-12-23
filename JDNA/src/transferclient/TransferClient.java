package transferclient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * Copyright (c) 2014, Fernando Alves <falves@lasige.di.fc.ul.pt>
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
public class TransferClient {

    /**
     * Simple transfer class for the passage of a file
     * 
     * @param args [0]: the host where to transfer in ipv4 format
     *              [1]: the port of the host where to connect
     *              [2]: the name of the file to transfer
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        Socket socket;
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String fileName = args[2];

        socket = new Socket(host, port);

        File file = new File(fileName);
        // Get the size of the file
        long length = file.length();
        byte[] bytes;
                
        if(length < Integer.MAX_VALUE)
            bytes = new byte[(int) length];
        else
            bytes = new byte[Integer.MAX_VALUE];
         
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

        int count;

        while ((count = bis.read(bytes)) > 0) {
            out.write(bytes, 0, count);
        }

        out.flush();
        out.close();
        bis.close();
        socket.close();
    }
}
