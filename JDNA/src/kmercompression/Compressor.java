package kmercompression;

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
class Compressor {

    private static final int INPUT_MATCH_POSITION = 0;
    private static final int REFERNECE_MATCH_POSITION = 1;
    //
    private final KmerTable structure;
    //
    static final int CHAR_SIZE = 3;
    private final int blockSize;
    private int refLength;
    private int inpLength;
    //
    final char[] reference;
    char[] input;
    //
    private final int[] getResult;
    //
    long ts, te;

    Compressor(int bs) {
        blockSize = bs;
        structure = new KmerTable();
        getResult = new int[2];
        reference = new char[blockSize];
        input = new char[blockSize];
    }

    void init(int readSize) {
        ts = System.currentTimeMillis();
        refLength = readSize;
        structure.init();
    }

    void compress(CompressionWriter writer, int readSize) {

        int index;
        int matchIndex;
        int referenceIndex = 0;
        int matchSize;
        int difference;
        int inpDiff;
        int i;

        this.inpLength = readSize;

        //for every char in input
        for (index = 0; index < readSize - JDNA.KMER_SIZE; index++) {

            if (input[index] == 'N') {
                int numN = treatNSeq(index, readSize);
                if (numN == -1) {
                    writer.writeChar(input[index]);
                } else {
                    writer.writeChar(input[index], numN);
                    index += numN - 1;
                    referenceIndex += numN;
                }
            } else {

                matchSize = 0;

                //search for a match
                structure.get(index, referenceIndex);
                matchIndex = getResult[REFERNECE_MATCH_POSITION];

                //no match
                if (matchIndex < 0) {
                    writer.writeChar(input[index]);

                } else {
                    //adjust index position
                    if (index < getResult[INPUT_MATCH_POSITION]) {
                        inpDiff = getResult[INPUT_MATCH_POSITION] - index;
                        for (i = 0; i < inpDiff; i++) {
                            writer.writeChar(input[index + i]);
                        }
                        index += inpDiff;
                    }
                    //stretch match
                    matchSize += JDNA.KMER_SIZE;
                    while ((index + matchSize < readSize) && //do not pass input length
                            (matchIndex + matchSize < refLength) && //do not pass reference length
                            ((input[index + matchSize]) == (reference[matchIndex + matchSize])) //chars are still matching
                            ) {
                        matchSize++;
                    }

                    if (matchIndex != referenceIndex) {
                        difference = matchIndex - referenceIndex;
                        referenceIndex = matchIndex;
                    } else {
                        difference = 0;
                    }

                    writer.writeAll(difference, matchSize);

                    if (matchSize > 0) {
                        index += matchSize - 1;
                        referenceIndex += matchSize;
                    }
                }
            }
        }
        //if input is bigger than ref, then write the remainder
        for (int j = index; j < readSize; j++) {
            writer.writeChar(input[j]);
        }

    }

    private int treatNSeq(int index, int readSize) {
        int i;
        //the N sequence must be greater than KMER_SIZE for efficiency
        for (i = index; i < JDNA.KMER_SIZE - 1; i++) {
            if (input[i] != 'N') {
                return -1;
            }
        }

        //get the N sequence
        while (i < readSize && input[i] == 'N') {
            i++;
        }

        return i - index;
    }

    private class KmerTable {

        private static final int INITIAL_SIZE = 5;
        private static final int INCREASE_FACTOR = 10;
        private static final int blockSizeRatio = 100;
        private final int MAX_WINDOW;
        //
        private int numKeys;
        private final int tableSize;
        private int lastIndexEnd;
        //
        private static final boolean REF = true;
        private static final boolean INP = false;
        //
        private final int[][] table;
        private final int[] counter;
        private int[] multiplier;
        private final static int numChars = 5;
        private boolean indexing;

        //create table
        KmerTable() {
            tableSize = blockSize / 5;
            table = new int[tableSize][];
            counter = new int[tableSize];
            setMultiplier(JDNA.KMER_SIZE);
            numKeys = 0;
            MAX_WINDOW = blockSize / blockSizeRatio;
            indexing = false;
        }

        //reset table
        void init() {
            lastIndexEnd = 0;
            if (numKeys != 0) {
                Arrays.fill(counter, 0);
            }
            numKeys = 0;
        }

        void put(int refPos) {
            int hash = localHashCode(REF, refPos) % tableSize;
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

        /**
         * get[0]: input position get[1]: ref position
         */
        void get(int inpPos, int refPos) {
            if (!indexing) {

                //if there is a direct match
                if (refPos + JDNA.KMER_SIZE < refLength
                        && inpPos + JDNA.KMER_SIZE < inpLength
                        && equals(refPos, inpPos)) {
                    getResult[INPUT_MATCH_POSITION] = inpPos;
                    getResult[REFERNECE_MATCH_POSITION] = refPos;
                    return;
                }

                //Test for SNP
                if (refPos + JDNA.KMER_SIZE < refLength
                        && inpPos + JDNA.KMER_SIZE < inpLength
                        && equals(refPos + 1, inpPos + 1)) {
                    getResult[INPUT_MATCH_POSITION] = inpPos + 1;
                    getResult[REFERNECE_MATCH_POSITION] = refPos + 1;
                    indexing = false;
                    return;
                }

                //Test for SNP
                if (refPos + JDNA.KMER_SIZE < refLength
                        && inpPos + JDNA.KMER_SIZE < inpLength
                        && equals(refPos + 1, inpPos + 1)) {
                    getResult[INPUT_MATCH_POSITION] = inpPos + 1;
                    getResult[REFERNECE_MATCH_POSITION] = refPos + 1;
                    indexing = false;
                    return;
                }

                int maxRef = (refPos + JDNA.SEARCH_WINDOW) > refLength
                        ? refLength - refPos - JDNA.KMER_SIZE
                        : JDNA.SEARCH_WINDOW - JDNA.KMER_SIZE;
                int maxInp = (inpPos + JDNA.SEARCH_WINDOW) > inpLength
                        ? inpLength - inpPos - JDNA.KMER_SIZE
                        : JDNA.SEARCH_WINDOW - JDNA.KMER_SIZE;

                //search forward
                for (int i = 0; i < maxRef - 1; i++) {
                    for (int j = 0; j < maxInp - 1; j++) {
                        if (equals(refPos + i, inpPos + j)) {
                            getResult[INPUT_MATCH_POSITION] = inpPos + j;
                            getResult[REFERNECE_MATCH_POSITION] = refPos + i;
                            indexing = false;
                            return;
                        }
                    }
                }

                int min = refPos - JDNA.SEARCH_WINDOW > 0 ? -JDNA.SEARCH_WINDOW : 0;
                maxRef = refPos + JDNA.KMER_SIZE > refLength ? -JDNA.KMER_SIZE : 0;

                //search backwards
                //no sense in searching the input backwards, would ony repeat already processed positions
                for (int i = min; i < maxRef; i++) {
                    if (equals(refPos + i, inpPos)) {
                        getResult[INPUT_MATCH_POSITION] = inpPos;
                        getResult[REFERNECE_MATCH_POSITION] = refPos + i;
                        indexing = false;
                        return;
                    }
                }
            }

            int hash = localHashCode(INP, inpPos) % tableSize;
            if (hash < 0) {
                hash += tableSize;
            }

            if (counter[hash] == 0) {
                index(refPos);
            }

            if (counter[hash] == 0) {
                getResult[INPUT_MATCH_POSITION] = -1;
                getResult[REFERNECE_MATCH_POSITION] = -1;
            } else {
                obtainClosestMatch(inpPos, table[hash], counter[hash]);
            }
        }

        private void index(int refPos) {
            if (lastIndexEnd != refLength) {
                indexing = true;

                int start, end;
                //don't index already indexed ref positions
                if (refPos < lastIndexEnd) {
                    start = lastIndexEnd;
                } else {
                    start = refPos;
                }
                end = start + JDNA.INDEX_WINDOW;
                end = end > refLength ? refLength - JDNA.KMER_SIZE : end - JDNA.KMER_SIZE;

                //actual indexing
                for (int i = start; i < end; i++) {
                    put(i);
                }
                lastIndexEnd = end;
            }
        }

        private int localHashCode(boolean ref, int pos) {
            int res = 0;
            if (ref) {
                for (int i = 0; i < JDNA.KMER_SIZE; i++) {
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
            } else {
                for (int i = 0; i < JDNA.KMER_SIZE; i++) {
                    switch (input[i + pos]) {
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
            }

            return res;
        }

        private void obtainClosestMatch(int inpPos, int[] values, int numValues) {
            if (numValues == 1) {
                if (equals(values[0], inpPos)) {
                    if ((values[0] > inpPos - MAX_WINDOW && values[0] < inpPos + MAX_WINDOW)) {
                        getResult[INPUT_MATCH_POSITION] = inpPos;
                        getResult[REFERNECE_MATCH_POSITION] = values[0];
                        indexing = false;
                        return;
                    } else {
                        getResult[INPUT_MATCH_POSITION] = -1;
                        getResult[REFERNECE_MATCH_POSITION] = -1;
                        return;
                    }
                }
            }

            int dif1 = -1, dif2;
            int count = 0;
            int lastRes = -1;

            boolean dif1Done = false, dif2Done = false;
            //first, a match with one of the ref values must be found
            while (!dif1Done && count < numValues) {
                if (equals(values[count], inpPos)) {
                    dif1 = Math.abs(values[count] - inpPos);
                    lastRes = values[count];
                    dif1Done = true;
                }
                count++;
            }
            if (!dif1Done) {
                getResult[INPUT_MATCH_POSITION] = -1;
                getResult[REFERNECE_MATCH_POSITION] = -1;
                return;
            }

            //search the remaining values to find the closest one to the input
            while (!dif2Done && count < numValues) {
                if (equals(values[count], inpPos)) {
                    dif2 = Math.abs(values[count] - inpPos);
                    if (dif2 > dif1) {
                        dif2Done = true;
                    } else {
                        dif1 = dif2;
                        lastRes = values[count];
                    }
                }
                count++;
            }

            if ((lastRes > inpPos - MAX_WINDOW && lastRes < inpPos + MAX_WINDOW)) {
                getResult[INPUT_MATCH_POSITION] = inpPos;
                getResult[REFERNECE_MATCH_POSITION] = lastRes;
                indexing = false;
            } else {
                getResult[INPUT_MATCH_POSITION] = -1;
                getResult[REFERNECE_MATCH_POSITION] = -1;
            }
        }

        private boolean equals(int posRef, int posInp) {
            for (int i = 0; i < JDNA.KMER_SIZE; i++) {
                if (reference[posRef + i] != input[posInp + i]) {
                    return false;
                }
            }
            return true;
        }

        private void setMultiplier(int kSize) {
            multiplier = new int[kSize];
            for (int i = 0; i < kSize; i++) {
                multiplier[i] = (int) Math.pow(numChars, i);
            }
        }
    }
}//Kmer
