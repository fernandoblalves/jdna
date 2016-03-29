package kmercompression;

import java.util.ArrayList;

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
class CompressionWriter {

    private final BitOutputStream bos;
    private final int maxBits;
    //
    private final ArrayList<Character> characters;
    private final ArrayList<Integer> nPositions;
    //
    private static final byte AA = 0x0;
    private static final byte CC = 0x1;
    private static final byte TT = 0x2;
    private static final byte GG = 0x3;
    private static final byte NN = 0x4;
    private static final byte charseq = 0x5;
    private static final byte change = 0x6;
    private static final byte CARDINAL = 0x7;
    //
    private static final byte ZERO = 0x0;
    private static final byte ONE = 0x1;
    private static final byte _16 = 0x10;
    private static final byte _48 = 48;
    private static final byte _112 = 112;
    private static final int _240 = 240;
    private static final int _496 = 496;
    private static final int _1008 = 1008;
    private static final int _2032 = 2032;

    CompressionWriter(BitOutputStream b, int m) {
        bos = b;
        maxBits = m;
        characters = new ArrayList<>();
        nPositions = new ArrayList<>();
    }

    void writeChar(char c){
        characters.add(c);
        if(c == 'N' || c == 'n'){
            characters.add('-');
        }
    }
    
    void writeChar(char c, int pos) {
        characters.add(c);
        characters.add('+');
        nPositions.add(pos);
    }

    void writeAll(int difference, int matchSize) {
        writeCharSequence();
        writeIndexAdjustment(difference);
        writeMatch(matchSize);
    }

    private void writeMatch(int value) {
        if (value >= JDNA.KMER_SIZE && value < JDNA.KMER_SIZE + _16) {
            bos.write(3, 0);
            bos.write(4, value - JDNA.KMER_SIZE);
        } else if (value >= JDNA.KMER_SIZE + _16 && value < JDNA.KMER_SIZE + _48) {
            bos.write(3, 1);
            bos.write(5, value - (JDNA.KMER_SIZE + _16));
        } else if (value >= JDNA.KMER_SIZE + _48 && value < JDNA.KMER_SIZE + _112) {
            bos.write(3, 2);
            bos.write(6, value - (JDNA.KMER_SIZE + _48));
        } else if (value >= JDNA.KMER_SIZE + _112 && value < JDNA.KMER_SIZE + _240) {
            bos.write(3, 3);
            bos.write(7, value - (JDNA.KMER_SIZE + _112));
        } else if (value >= JDNA.KMER_SIZE + _240 && value < JDNA.KMER_SIZE + _496) {
            bos.write(3, 4);
            bos.write(8, value - (JDNA.KMER_SIZE + _240));
        } else if (value >= JDNA.KMER_SIZE + _496 && value < JDNA.KMER_SIZE + _1008) {
            bos.write(3, 5);
            bos.write(9, value - (JDNA.KMER_SIZE + _496));
        } else if (value >= JDNA.KMER_SIZE + _1008 && value < JDNA.KMER_SIZE + _2032) {
            bos.write(3, 6);
            bos.write(10, value - (JDNA.KMER_SIZE + _1008));
        } else if (value >= JDNA.KMER_SIZE + _2032) {
            bos.write(3, 7);
            bos.write(maxBits, value);
        }
    }

    private void writeIndexAdjustment(int value) {
        if (value == 1) {
            bos.write(1, ZERO);
        } else {
            bos.write(1, ONE);
            if (value < (-127)) {
                bos.write(3, 0);
                bos.write(maxBits, (-value));
            } else if (value >= (-127) && value <= 0) {
                bos.write(3, 1);
                bos.write(7, (-value));
            } else if (value >= 2 && value <= 9) {
                bos.write(3, 2);
                bos.write(3, value - 2);
            } else if (value >= 10 && value <= 17) {
                bos.write(3, 3);
                bos.write(3, value - 10);
            } else if (value >= 18 && value <= 33) {
                bos.write(3, 4);
                bos.write(4, value - 18);
            } else if (value >= 34 && value <= 289) {
                bos.write(3, 5);
                bos.write(8, value - 34);
            } else if (value >= 290 && value <= 801) {
                bos.write(3, 6);
                bos.write(9, value - 290);
            } else {
                bos.write(3, 7);
                bos.write(maxBits, value);
            }
        }
    }

    private void writeCharSequence() {
        if (!characters.isEmpty()) {
            if (characters.size() > 1) {
                bos.write(Compressor.CHAR_SIZE, charseq);
            }
            for (int i = 0; i < characters.size(); i++) {
                char c = characters.get(i);

                switch (c) {
                    case 'A': //0
                        bos.write(Compressor.CHAR_SIZE, AA);
                        break;
                    case 'C': //1
                        bos.write(Compressor.CHAR_SIZE, CC);
                        break;
                    case 'T': //2
                        bos.write(Compressor.CHAR_SIZE, TT);
                        break;
                    case 'G': //3
                        bos.write(Compressor.CHAR_SIZE, GG);
                        break;
                    case '+': //5
                        // empty
                        break;
                    case '&': //6
                        bos.write(Compressor.CHAR_SIZE, change);
                        break;
                    case '#': //7 -> termination
                        bos.write(Compressor.CHAR_SIZE, CARDINAL);
                        break;
                    default: //4  -> N and default
                        if (c == 'N' || c == 'n') {
                            if(characters.get(i+1) == '-'){
                                bos.write(3, NN);
                                bos.write(1, 0);
                            }else {
                                bos.write(3, NN);
                                bos.write(1, 1);

                               bos.write(maxBits, nPositions.remove(0));
                                
                            }
                            
                            i++;
                        } else {
                            if (c == '\n' || c == '\r') {
                                System.err.println("endline found");
                            } else {
                                System.err.println("Character: \"" + c + "\"; replaced with 'N'.");
                                bos.write(3, NN);
                                bos.write(1, 0);
                            }
                        }
                        break;
                }
            }
            if (characters.size() > 1) {
                bos.write(Compressor.CHAR_SIZE, charseq);
            }
            characters.clear();
        }else{
            bos.write(Compressor.CHAR_SIZE, charseq);
            bos.write(Compressor.CHAR_SIZE, charseq);
        }
    }

    void writeChange() {
        writeChar('&');
        writeCharSequence();
    }

    void writeTermination() {
        writeChar('#');
        writeCharSequence();
    }

    void flush() {
        bos.flush();
    }

    void close() {
        bos.close();
    }
}
