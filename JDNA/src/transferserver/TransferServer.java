package transferserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
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
public class TransferServer {

    /**
     * @param args [0]: The port where to setup the server
     *              [1]: The name of the file to output
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = null;
        int port = Integer.parseInt(args[0]);
        String outFile = args[1];

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            System.out.println("Can't setup server on this port number. ");
            System.exit(-1);
        }

        Socket socket = null;
        BufferedInputStream is = null;
        BufferedOutputStream bos = null;
        int bufferSize = 0;

        try {
            socket = serverSocket.accept();
        } catch (IOException ex) {
            System.out.println("Can't accept client connection. ");
            System.exit(-1);
        }

        try {
            is = new BufferedInputStream(socket.getInputStream());

            bufferSize = socket.getReceiveBufferSize();
            System.out.println("Buffer size: " + bufferSize);
        } catch (IOException ex) {
            System.out.println("Can't get socket input stream. ");
            System.exit(-1);
        }

        bos = new BufferedOutputStream(new FileOutputStream(outFile));

        byte[] bytes = new byte[bufferSize];

        int count;

        while ((count = is.read(bytes)) > 0) {
            bos.write(bytes, 0, count);
        }

        bos.flush();
        bos.close();
        is.close();
        socket.close();
        serverSocket.close();
    }
}
