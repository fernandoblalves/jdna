#JDNA
JDNA is an implementation of the On-Demand Indexing algorithm for referential compression of aligned DNA sequences proposed in [this paper](http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0132460 "ODI Article"). Note that the reference must always be in raw format (no endlines or comments, only base-pairs). I recommend using references consisting only of A,C,T,G or N, since compression is made to deal only with these bps.

#Using JDNA
This tutorial explains how to use JDNA to compress DNA sequences based on a representative reference.


## Basic usage ##

    $ java -jar JDNA.jar <TASK> <REFERENCE> <INPUT> <OUTPUT>
 
arguments:

  * TASK:	The task to be executed (COMPRESS, or DECOMPRESS)
  * REFERENCE:	The path to the file containing the reference sequence
  * INPUT:	The path to the to-be-compressed or decompressed file 
  * OUTPUT:	The path to the resulting (de)compressed file 

examples:

    $ java -jar JDNA.jar COMPRESS human_g1k_v37.raw HG01390.fasta HG01390.cmp
    $ java -jar JDNA.jar DECOMPRESS human_g1k_v37.raw HG01390.cmp HG01390.fasta

## A straightforward example ##

Download the JAR file:

    $ wget  https://github.com/Camandros/jdna/raw/master/JDNA/JDNA.jar

Download two example files containing DNA sequences (1kB each):

    $ wget https://raw.githubusercontent.com/Camandros/jdna/master/JDNA/ref_ex.raw
    $ wget https://raw.githubusercontent.com/Camandros/jdna/master/JDNA/inp_ex.raw

Execute the JDNA to compress the input sequence:

    $ java -jar JDNA.jar COMPRESS ref_ex.raw inp_ex.raw out_ex.cmp

Run the following _du_ command to verify the size of the input and output files (in bytes using the _-b_ option):

    $ du -b inp_ex.raw out_ex.cmp
    1032	inp_ex.raw
    44  	out_ex.cmp

Execute the JDNA to decompress the output file:

    $ java -jar JDNA.jar DECOMPRESS ref_ex.raw out_ex.cmp inp_ex_dcmp.raw

Run a checksum command (e.g., _md5sum_) to verify that the decompressed output is exactly the same as the original to-be-compressed input (JDNA is lossless):

    $ md5sum inp_ex.raw inp_ex_dcmp.raw 
    9217d97a92cf8d76a92ef60a9101f468  inp_ex.raw
    9217d97a92cf8d76a92ef60a9101f468  inp_ex_dcmp.raw

## Customizing JDNA ##
JDNA allows users to provide a configuration file (_config.ini_) in the same folder as the JAR file with customized values for the following parameters:

| Parameter     | Default Value | Description                                                          |
|---------------|---------------|----------------------------------------------------------------------|
| kmer_size     | 20            | Size of k-mer when indexing the reference (in bp)                    |
| search_window | 120           | Window size for brute-force searches (in bp)                         |
| index_window  | 200           |               Window size for indexed searches (in bp)               |
| dcmp_mem      | 1             | Maximum amount of memory to be used during decompression (in MB)     |
| block_size    | 250           | Size of each reference block to be loaded to the main memory (in MB) |

example:

    kmer_size=20
    search_window=120 
    index_window=200 
    dcmp_mem=1 
    block_size=250


## Notes and throubleshooting ##
  * JDNA accepts input files with two extensions: _.raw_ and _.fasta_. The former is a raw DNA sequence that contains only A, C, G, T, and N nucleotides, while the latter accepts files with comment lines (i.e., lines started by the ">" character).
  * If the file is a _.fasta_ then a _.ccom_ file is generated. The _.ccom_ file has the comments present in the FASTA file and the line numbers of those comments. For decompression, the compressed _cmp_ file is assigned as input. JDNA will then search for a _.ccom_ file with the same name as the _cmp_ file. If the _.ccom_ file is found then a _.fasta_ file is generated.

Example (note that the file name is always **HG01390**):

    $ java -jar JDNA.jar COMPRESS human_g1k_v37.raw HG01390.fasta HG01390.cmp
    $ java -jar JDNA.jar DECOMPRESS human_g1k_v37.raw HG01390.cmp HG01390.fasta
