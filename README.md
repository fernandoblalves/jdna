#JDNA usage

java <jvm params> -jar JDNA.jar TASK REFERENCE INPUT OUTPUT

Where

    TASK is COMPRESS or DECOMPRESS; 

    REFERENCE is the path to the reference file; 

    INPUT is the path to the file to be compressed or decompressed; 

    OUTPUT is the path to the output file. 

Also, a config.ini is expected to be present, or these are the default values:

    kmer_size=20 

    search_window=120 

    index_window=200 

    dcmp_mem=1 - this value is in MB 

    block_size=250 - this value is in MB 

#Test case

From the folder JDNA download the JDNA.jar, config,ini, ref_ex.raw and inp_ex.raw.

To test compression run:

`java -jar JDNA.jar COMPRESS ref_ex.raw inp_ex.raw cmp.raw`

Then you can test decompression runnin:

`java -jar JDNA.jar DECOMPRESS ref_ex.raw cmp.raw dcmp.raw`.

Once both tasks are complete, you can use md5sum to verify that the decompresses file matches the input file:

`md5sum inp_ex.raw dcmp.raw`