package serializationtest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

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
public class SerializationTest {

    private int blockSize;
    private int tableSize;
    private char[] reference;
    private final int KMERSIZE = 20;
    private KmerTable table;
    private int[][] newTable;
    private int[] newCounter;

    /**
     * Main class to start serialization tests
     * Results are written to time_results.txt, and appended per tested chromosome
     * @param args the chromosome number of the reference to create the table, which will
     * be serialized and then de-serialized 10 times
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {

        SerializationTest serTest = new SerializationTest();
        serTest.start(Integer.parseInt(args[0]));
    }

    private void start(int pos) throws Exception {
        File ref = new File("RefChr/refChr" + pos + ".dna");
        File outputTable = new File("KmerTable_table" + pos + ".ser");
        File timeResults = new File("time_results.txt");

        BufferedWriter resultWriter = new BufferedWriter(new FileWriter(timeResults, true));
        DataInputStream tableReader;

        blockSize = (int) ref.length();
        tableSize = blockSize / 5;
        BufferedReader refReader = new BufferedReader(new FileReader(ref));
        reference = new char[(int) ref.length()];
        refReader.read(reference);
        refReader.close();
        
        newTable = new int[tableSize][];
        newCounter = new int[tableSize];

        table = new KmerTable();
        table.init();
        

        System.out.println("create table");

        for (int i = 0; i < blockSize - KMERSIZE; i++) {
            table.put(i);
        }

        DataOutputStream tableWriter = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputTable)));
        serialize(tableWriter);
        tableWriter.close();

        table = null;
        System.gc();

        long st, et;
        
        System.out.println("Starting tests "+pos);
        
        resultWriter.write("deserialize " + pos + "\n");
        for (int i = 0; i < 10; i++) {
            System.out.println("run "+(i+1));
            tableReader = new DataInputStream(new BufferedInputStream(new FileInputStream((outputTable))));

            st = System.currentTimeMillis();

            deserialize(tableReader);

            et = System.currentTimeMillis();
            resultWriter.write((int) (et - st) + "\n");
            
            cleanNewTable();
            
            tableReader.close();

            System.gc();
        }
        
        System.out.println("Tests finished");

        resultWriter.close();
    }

    private void serialize(DataOutputStream outputTable) throws Exception {
        for (int i = 0; i < table.counter.length; i++) {
            outputTable.writeInt(table.counter[i]);
            if (table.counter[i] > 0) {
                for (int j = 0; j < table.counter[i]; j++) {
                    outputTable.writeInt(table.table[i][j]);
                }
            }
        }
    }

    private void deserialize(DataInputStream input) {
        int elems, position = 0, i;
        try {
            for (int j = 0; j < newCounter.length; j++) {
                elems = input.readInt();
                newCounter[position] = elems;
                newTable[position] = new int[elems];
                for (i = 0; i < elems; i++) {
                    newTable[position][i] = input.readInt();
                }
                position++;
            }
        } catch (EOFException e) {
        } catch (IOException ex) {
            System.err.println("exception while reading serialized file");
            System.exit(0);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    private void cleanNewTable(){
        Arrays.fill(newCounter, 0);
        newTable = null;
        newTable = new int[tableSize][];
    }

    /**
     * Kmer table class copied from JDNA
     * This class has been trimmed to just the creation of the table,
     * since no other functionalities are required
     * 
     * For reference of how this table works, go to JDNA.KmerCompression.JDNA.KmerTable
     */
    private class KmerTable implements Serializable {

        private static final int INITIAL_SIZE = 10;
        private static final int INCREASE_FACTOR = 5;
        //
        private int numKeys;
        private final int tableSize;
        //
        int[][] table;
        int[] counter;
        private int[] multiplier;
        private final static int numChars = 5;

        //create table
        KmerTable() {
            tableSize = blockSize / 5;
            table = new int[tableSize][];
            counter = new int[tableSize];
            setMultiplier(20);
            numKeys = 0;
        }

        //reset table
        void init() {
            if (numKeys != 0) {
                Arrays.fill(counter, 0);
            }
            numKeys = 0;
        }

        void put(int refPos) {
            int hash = localHashCode(refPos) % tableSize;
            if (hash < 0) {
                hash += tableSize;
            }

            if (table[hash] == null) {
                table[hash] = new int[INITIAL_SIZE];
                numKeys++;
            }
            if (counter[hash] == table[hash].length) {
                table[hash] = Arrays.copyOf(table[hash], table[hash].length * INCREASE_FACTOR);
            }
            table[hash][counter[hash]] = refPos;
            counter[hash]++;
        }

        private int localHashCode(int pos) {
            int res = 0;

            for (int i = 0; i < 20; i++) {
                switch (reference[i + pos]) {
                    case 'A':
                        res += 0;
                        break;
                    case 'C':
                        res += multiplier[i];
                        break;
                    case 'T':
                        res += 2 * multiplier[i];
                        break;
                    case 'G':
                        res += 3 * multiplier[i];
                        break;
                    case 'N':
                        res += 4 * multiplier[i];
                        break;
                }
            }

            return res;
        }

        private void setMultiplier(int kSize) {
            multiplier = new int[kSize];
            for (int i = 0; i < kSize; i++) {
                multiplier[i] = (int) Math.pow(numChars, i);
            }
        }
    }
}
