package kmercompression;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;

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
public class Decompressor {

    private static final int CHAR_SIZE = 3;
    private int MAXIMUM_NUMBER_BITS;
    private long blockSize;
    private byte[] ref;
    private BufferedOutputStream dcmpWriter;
    private static final boolean debugDecompression = false;
    private int charCounter;
    private int lineCounter;
    private int totalCounter;
    private boolean hasComments;
    private LinkedList<String> comments;
    private LinkedList<Integer> positions;
    private LinkedList<Integer> sizes;
    //
    private static final byte _16 = 0x10;
    private static final byte _48 = 48;
    private static final byte _112 = 112;
    private static final int _240 = 240;
    private static final int _496 = 496;
    private static final int _1008 = 1008;

    Decompressor(int effectiveSize, int maxDigits) {
        MAXIMUM_NUMBER_BITS = maxDigits;
        blockSize = effectiveSize;
        ref = new byte[JDNA.DECOMPRESS_MEM];
        charCounter = 0;
        lineCounter = 0;
        totalCounter = 0;
    }

    void decompress(File referenceFile, File compressedFile, File outputFile) {

        String line = compressedFile.getName();
        String[] splitted = line.split("\\.");
        File cFile = new File(splitted[0] + ".ccom");
        if (cFile.exists()) {
            hasComments = true;
            comments = new LinkedList<String>();
            positions = new LinkedList<Integer>();
            sizes = new LinkedList<Integer>();
            try {
                BufferedReader commentReader = new BufferedReader(new FileReader(cFile));
                while (commentReader.ready()) {
                    line = commentReader.readLine();
                    splitted = line.split(JDNA.COMMENT_SEPARATOR);
                    positions.add(Integer.parseInt(splitted[0]));
                    comments.add(splitted[1]);
                    sizes.add(Integer.parseInt(splitted[2]));
                }
                commentReader.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            hasComments = false;
        }

        BitInputStream bis = null;
        BufferedInputStream refReader = null;
        FileInputStream fis = null;
        FileChannel fileChannel = null;

        boolean done = false;
        boolean change;
        boolean readChars = false;

        int refArraySize = ref.length;

        int b;
        int length = 0;
        int globalIndex = 0;
        int arrayIndex = 0;
        int indexAdjust;
        int prevFilledSize = 0;
        int amountToWrite;
        int amountWrote;
        long counter = 0;

        try {

            bis = new BitInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(compressedFile)), JDNA.ONEMB));
            fis = new FileInputStream(referenceFile);
            refReader = new BufferedInputStream(fis);
            fileChannel = fis.getChannel();
            dcmpWriter = new BufferedOutputStream(new FileOutputStream(outputFile), JDNA.ONEMB);

            do {
                change = false;
                arrayIndex = 0;
                globalIndex = 0;

                fileChannel.position(((long) (counter * blockSize)));
                refReader.read(ref);

                b = bis.read(CHAR_SIZE);
                switch (b) {
                    case 0:
                        write('A');
                        break;
                    case 1:
                        write('C');
                        break;
                    case 2:
                        write('T');
                        break;
                    case 3:
                        write('G');
                        break;
                    case 4:
                        b = bis.read(1);
                        if (b == 0) {
                            write('N');
                        } else {
                            b = bis.read(MAXIMUM_NUMBER_BITS);
                            for (int i = 0; i < b; i++) {
                                write('N');
                            }
                            globalIndex += b;
                        }
                        break;
                    case 5:
                        do {
                            b = bis.read(3);
                            switch (b) {
                                case 0:
                                    write('A');
                                    break;
                                case 1:
                                    write('C');
                                    break;
                                case 2:
                                    write('T');
                                    break;
                                case 3:
                                    write('G');
                                    break;
                                case 4:
                                    b = bis.read(1);
                                    if (b == 0) {
                                        write('N');
                                    } else {
                                        b = bis.read(MAXIMUM_NUMBER_BITS);
                                        for (int i = 0; i < b; i++) {
                                            write('N');
                                        }
                                        globalIndex += b;
                                    }
                                    break;
                                case 5:
                                    readChars = true;
                                    break;
                                case 6:
                                    change = true;
                                    break;
                                case 7:
                                    change = true;
                                    done = true;
                                    break;
                            }
                        } while (!readChars);
                        readChars = false;
                        break;
                    case 6:
                        change = true;
                        break;
                    case 7:
                        change = true;
                        done = true;
                        break;
                }

                do {
                    b = bis.read(1);
                    if (debugDecompression) {
                        System.out.print("index: " + globalIndex);
                    }
                    if (b == 0) { //read 0 -> SNP
                        globalIndex++;
                        indexAdjust = 1;
                    } else { // read 1 -> index adjust
                        b = bis.read(3);
                        switch (b) {
                            case 0:
                                indexAdjust = bis.read(MAXIMUM_NUMBER_BITS);
                                globalIndex -= indexAdjust;
                                break;
                            case 1:
                                indexAdjust = bis.read(7);
                                globalIndex -= indexAdjust;
                                break;
                            case 2:
                                indexAdjust = bis.read(3) + 2;
                                globalIndex += indexAdjust;
                                break;
                            case 3:
                                indexAdjust = bis.read(3) + 10;
                                globalIndex += indexAdjust;
                                break;
                            case 4:
                                indexAdjust = bis.read(4) + 18;
                                globalIndex += indexAdjust;
                                break;
                            case 5:
                                indexAdjust = bis.read(8) + 34;
                                globalIndex += indexAdjust;
                                break;
                            case 6:
                                indexAdjust = bis.read(9) + 290;
                                globalIndex += indexAdjust;
                                break;
                            case 7:
                                indexAdjust = bis.read(MAXIMUM_NUMBER_BITS);
                                globalIndex += indexAdjust;
                                break;
                        }
                    }

                    b = bis.read(3);

                    switch (b) {
                        case 0:
                            length = bis.read(4) + JDNA.KMER_SIZE;
                            break;
                        case 1:
                            length = bis.read(5) + JDNA.KMER_SIZE + _16;
                            break;
                        case 2:
                            length = bis.read(6) + JDNA.KMER_SIZE + _48;
                            break;
                        case 3:
                            length = bis.read(7) + JDNA.KMER_SIZE + _112;
                            break;
                        case 4:
                            length = bis.read(8) + JDNA.KMER_SIZE + _240;
                            break;
                        case 5:
                            length = bis.read(9) + JDNA.KMER_SIZE + _496;
                            break;
                        case 6:
                            length = bis.read(10) + JDNA.KMER_SIZE + _1008;
                            break;
                        case 7:
                            length = bis.read(MAXIMUM_NUMBER_BITS);
                            break;
                    }
                    
                    //if the writing point is behind the current point
                    if (globalIndex < arrayIndex) {
                        fileChannel.position((long) (globalIndex + (blockSize * counter)));
                        arrayIndex = globalIndex;

                        if (length > refArraySize) {
                            while (arrayIndex + refArraySize < globalIndex + length) {
                                refReader.read(ref);
                                write();
                                arrayIndex += refArraySize;
                            }
                            refReader.read(ref);
                            write(0, globalIndex + length - arrayIndex);

                        } else {
                            refReader.read(ref);
                            write(0, length);
                        }

                        //if the array must advance in the reference
                    } else if (globalIndex + length > arrayIndex + refArraySize) {
                        //if the array must fully move, then move the reader to the new place, then refill the array
                        if (arrayIndex + refArraySize < globalIndex) {
                            //refReader.skip( (long) ( globalIndex - (arrayIndex + refArraySize) ) );
                            fileChannel.position((long) (globalIndex + (blockSize * counter)));
                            arrayIndex = globalIndex;
                            if (length > refArraySize) {
                                while (arrayIndex + refArraySize < globalIndex + length) {
                                    refReader.read(ref);
                                    write();
                                    arrayIndex += refArraySize;
                                }
                                refReader.read(ref);
                                write(0, globalIndex + length - arrayIndex);

                            } else {
                                refReader.read(ref);
                                write(0, length);
                            }

                        } else { //if just a part of the array has to advance in the reference
                            prevFilledSize = globalIndex - arrayIndex;
                            if (length > refArraySize) {
                                amountToWrite = length;
                                write(prevFilledSize, refArraySize - prevFilledSize);
                                amountToWrite -= (refArraySize - prevFilledSize);
                                arrayIndex += refArraySize;
                                if (amountToWrite > refArraySize) {
                                    amountWrote = 0;
                                    while (amountToWrite - amountWrote > refArraySize) {
                                        refReader.read(ref);
                                        write();
                                        arrayIndex += refArraySize;
                                        amountWrote += refArraySize;
                                    }

                                    refReader.read(ref);
                                    write(0, amountToWrite - amountWrote);
                                } else {
                                    refReader.read(ref);
                                    write(0, amountToWrite);
                                }
                            } else {
                                amountToWrite = length;
                                write(prevFilledSize, refArraySize - prevFilledSize);
                                amountToWrite -= (refArraySize - prevFilledSize);

                                refReader.read(ref);
                                write(0, amountToWrite);
                                arrayIndex += refArraySize;
                            }
                        }
                    } else {
                        write(globalIndex - arrayIndex, length);
                    }

                    globalIndex += length;

                    b = bis.read(CHAR_SIZE);
                    switch (b) {
                        case 0:
                            write('A');
                            break;
                        case 1:
                            write('C');
                            break;
                        case 2:
                            write('T');
                            break;
                        case 3:
                            write('G');
                            break;
                        case 4:
                            b = bis.read(1);
                            if (b == 0) {
                                write('N');
                            } else {
                                b = bis.read(MAXIMUM_NUMBER_BITS);
                                for (int i = 0; i < b; i++) {
                                    write('N');
                                }
                                globalIndex += b;
                            }
                            break;
                        case 5:
                            do {
                                b = bis.read(3);
                                switch (b) {
                                    case 0:
                                        write('A');
                                        break;
                                    case 1:
                                        write('C');
                                        break;
                                    case 2:
                                        write('T');
                                        break;
                                    case 3:
                                        write('G');
                                        break;
                                    case 4:
                                        b = bis.read(1);
                                        if (b == 0) {
                                            write('N');
                                        } else {
                                            b = bis.read(MAXIMUM_NUMBER_BITS);
                                            for (int i = 0; i < b; i++) {
                                                write('N');
                                            }
                                            globalIndex += b;
                                        }
                                        break;
                                    case 5:
                                        readChars = true;
                                        break;
                                    case 6:
                                        change = true;
                                        break;
                                    case 7:
                                        change = true;
                                        done = true;
                                        break;
                                }
                            } while (!readChars);
                            readChars = false;
                            break;
                        case 6:
                            change = true;
                            break;
                        case 7:
                            change = true;
                            done = true;
                            break;
                    }

                    if (debugDecompression) {
                        System.out.println("; diff: " + indexAdjust + "; ms: " + length);
                    }
                } while (!change);
                counter++;
            } while (!done);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                bis.close();
                dcmpWriter.flush();
                dcmpWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void write(int start, int len) throws Exception {
        if (!hasComments) {
            dcmpWriter.write(ref, start, len);
        } else {
            int max = start + len;
            for (int i = start; i < max; i++) {
                dcmpWriter.write(ref[i]);
                totalCounter++;
                charCounter = (charCounter + 1) % JDNA.FASTA_LINE_SIZE;
                if (sizes.size() > 0 && totalCounter == sizes.get(0)) {
                    charCounter = 0;
                    sizes.remove(0);
                }
                if (charCounter == 0) {
                    if (positions.size() > 0 && positions.get(0) == lineCounter) {
                        positions.remove();
                        if (lineCounter != 0) {
                            dcmpWriter.write('\n');
                        }
                        dcmpWriter.write(comments.remove().getBytes());
                    }
                    dcmpWriter.write('\n');
                    lineCounter++;
                }

            }
        }
    }

    private void write(char c) throws Exception {
        if (!hasComments) {
            dcmpWriter.write(c);
        } else {
            if (sizes.size() > 0 && totalCounter == sizes.get(0)) {
                charCounter = 0;
                sizes.remove(0);
            }
            if (charCounter == 0) {
                if (positions.size() > 0 && positions.get(0) == lineCounter) {
                    positions.remove();
                    if (lineCounter != 0) {
                        dcmpWriter.write('\n');
                    }
                    dcmpWriter.write(comments.remove().getBytes());
                }
                dcmpWriter.write('\n');
                lineCounter++;
            }
            dcmpWriter.write(c);
            totalCounter++;
            charCounter = (charCounter + 1) % JDNA.FASTA_LINE_SIZE;
        }
    }

    private void write() throws Exception {
        if (!hasComments) {
            dcmpWriter.write(ref);
        } else {
            for (int i = 0; i < ref.length; i++) {
                if (sizes.size() > 0 && totalCounter == sizes.get(0)) {
                    charCounter = 0;
                    sizes.remove(0);
                }
                if (charCounter == 0) {
                    if (positions.size() > 0 && positions.get(0) == lineCounter) {
                        positions.remove();
                        if (lineCounter != 0) {
                            dcmpWriter.write('\n');
                        }
                        dcmpWriter.write(comments.remove().getBytes());
                    }
                    dcmpWriter.write('\n');
                    lineCounter++;
                }
                dcmpWriter.write(ref[i]);
                totalCounter++;
                charCounter = (charCounter + 1) % JDNA.FASTA_LINE_SIZE;
            }
        }
    }
}