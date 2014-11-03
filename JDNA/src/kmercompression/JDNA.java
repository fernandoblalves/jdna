package kmercompression;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.zip.GZIPOutputStream;

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
public class JDNA {
    
    private static long blockSize = 1024 * 1024 * 250; //1,5GB -> 1024 * 512 * 3
    static final int ONEMB = 1024 * 1024;
    private static final int FASTA_LINE_SIZE_MARGIN = 100;
    static final int FASTA_LINE_SIZE = 60;
    static int KMER_SIZE = 20;
    static int DECOMPRESS_MEM = ONEMB;
    static int SEARCH_WINDOW = 120;
    static int INDEX_WINDOW = 200;
    
    static final String COMMENT_SEPARATOR = "!";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            printUsage();
            System.exit(0);
        }

        System.err.println("Start!");

        File refFile = new File(args[1]);
        File inputFile = new File(args[2]);
        File outputFile = new File(args[3]);
        
        File config = new File("config.ini");
        if(config.exists()){
            BufferedReader configReader = new BufferedReader(new FileReader(config));
            KMER_SIZE = Integer.parseInt(configReader.readLine().split("=")[1]);
            SEARCH_WINDOW = Integer.parseInt(configReader.readLine().split("=")[1]);
            INDEX_WINDOW = Integer.parseInt(configReader.readLine().split("=")[1]);
            DECOMPRESS_MEM = Integer.parseInt(configReader.readLine().split("=")[1]) * ONEMB;
            blockSize = Integer.parseInt(configReader.readLine().split("=")[1]) * ONEMB;
            configReader.close();
        }

        long bigger = refFile.length() > inputFile.length() ? refFile.length() : inputFile.length();
        int effectiveSize = (int) (bigger < blockSize ? bigger : blockSize);
        int maxDigits = numDigitsB(effectiveSize);

        if (args[0].equalsIgnoreCase("COMPRESS")) {
            compress(refFile, inputFile, outputFile, effectiveSize, maxDigits);
        } else if (args[0].equalsIgnoreCase("DECOMPRESS")) {
            decompress(refFile, inputFile, outputFile, effectiveSize, maxDigits);
        } else {
            System.out.println("Command not recognized. Program will exit.\n");
            printUsage();
        }
    }

    private static void compress(File refFile, File inputFile, File outputFile, int effectiveSize, int maxDigits)
            throws Exception {

        int readRef;
        int readInput;
        int lineCounter = 0, charCounter = 0;
        StringBuilder inputBuffer = new StringBuilder((int)blockSize + FASTA_LINE_SIZE_MARGIN);
        StringBuilder surplus = new StringBuilder();

        boolean inpHasComments = false;
        BufferedWriter commentWriter = null;
        String line = inputFile.getName();
        String[] splitted = line.split("\\.");
        
        if (splitted[1].equals("fasta")) {
            inpHasComments = true;
            commentWriter = new BufferedWriter(new FileWriter(splitted[0] + ".ccom"));
        }

        boolean done = false;
        boolean inputDone = false;

        BufferedReader refReader = new BufferedReader(new FileReader(refFile));
        BufferedReader inputReader = new BufferedReader(new FileReader(inputFile));
        CompressionWriter outputWriter = new CompressionWriter(new BitOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(outputFile)), ONEMB*10)), maxDigits);

        Compressor compressor = new Compressor(effectiveSize);
        
        char[] input = compressor.input;
        char[] ref = compressor.reference;

        long et, st;
        long bs, be;
        
        LinkedList<String> comments = new LinkedList<String>();

        st = System.currentTimeMillis();

        do {
            readInput = 0;
            readRef = refReader.read(ref);
            inputBuffer.delete(0, inputBuffer.length());
            if (readRef != effectiveSize) {
                done = true;
            }

            if (readRef != -1) {
                compressor.init(readRef);

                if (inpHasComments) {
                    if(surplus.length() > 0){
                        inputBuffer.append(surplus);
                        surplus.delete(0, surplus.length());
                    }
                    while (readInput < effectiveSize && inputReader.ready()) {
                        line = inputReader.readLine();
                        if(line.length() != 0){
                            if (line.charAt(0) == '>') {
                                comments.add(lineCounter+COMMENT_SEPARATOR+line+COMMENT_SEPARATOR+charCounter+"\n");
                                charCounter = 0;
                            } else {
                                inputBuffer.append(line);
                                readInput += line.length();
                                charCounter += line.length();
                            }
                            lineCounter++;
                        }
                    }
                    input = inputBuffer.toString().toCharArray();
                    surplus = inputBuffer.delete(0, effectiveSize);
                    readInput = input.length - surplus.length();
                    for(String s : comments){
                        commentWriter.write(s, 0, s.length());
                    }
                } else {
                    readInput = inputReader.read(input);
                }

                bs = System.currentTimeMillis();
                if (readInput != -1) {
                    compressor.compress(outputWriter, readInput);
                }
                if (readInput != effectiveSize) {
                    done = true;
                    inputDone = true;
                }
                be = System.currentTimeMillis();
                System.out.println("COMPRESS TIME: " + (be - bs) / 1000 + "s (" + (be - bs) + ")");
                System.out.println("===================================\n");
                if (!done) {
                    outputWriter.writeChange();
                }
            }
        } while (!done);

        if (!inputDone) {
            done = false;
            do {
                readInput = inputReader.read(input);
                if (readInput == -1) {
                    done = true;
                } else {
                    for (int i = 0; i < readInput; i++) {
                        outputWriter.writeChar(input[i]);
                    }
                    if (readInput != effectiveSize) {
                        done = true;
                    }
                }
            } while (!done);
        }

        outputWriter.writeTermination();
        outputWriter.flush();
        outputWriter.close();

        if(commentWriter != null){
            commentWriter.flush();
            commentWriter.close();
        }
        
        et = System.currentTimeMillis();
        
        System.out.println("INDEX & COMPRESS TIME: " + (et - st) / 1000 + "s (" + (et - st) + ")");
    }

    private static void decompress(File refFile, File inputFile, File outputFile, int effectiveSize, int maxDigits)
            throws Exception {

        long st, et;

        Decompressor k1 = new Decompressor(effectiveSize, maxDigits);

        st = System.currentTimeMillis();
        k1.decompress(refFile, inputFile, outputFile);
        et = System.currentTimeMillis();
        System.out.println("DECOMPRESS TIME: " + (et - st) / 1000 + "s (" + (et - st) + ")");
    }

    private static void printUsage() {
        System.out.println("\nKmerCompression TASK REFERENCE INPUT OUTPUT");
        System.out.println("Where\n\tTASK is COMPRESS or DECOMPRESS;");
        System.out.println("\tREFERENCE is the path to the reference file;");
        System.out.println("\tINPUT is the path to the file to be compressed or decompressed;");
        System.out.println("\tOUTPUT is the path to the output file.\n");
        System.out.println("Also, a config.ini is expected to be present, or these are the default values:");
        System.out.println("\tkmer_size=20");
        System.out.println("\tsearch_window=120");
        System.out.println("\tindex_window=200");
        System.out.println("\tdcmp_mem=1 - this value is in MB");
        System.out.println("\tblock_size=250 - this value is in MB");
    }

    private static int numDigitsB(int num) {
        int res = 1;

        while (num / 2 > 0) {
            num /= 2;
            res++;
        }

        return res;
    }
}
