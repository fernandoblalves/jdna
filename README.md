#JDNA usage

java <jvm params> -jar JDNA.jar TASK REFERENCE INPUT OUTPUT

Where

> TASK is COMPRESS or DECOMPRESS;

> REFERENCE is the path to the reference file;

> INPUT is the path to the file to be compressed or decompressed;

> OUTPUT is the path to the output file.

Also, a config.ini is expected to be present, or these are the default values:

> kmer\_size=20

> search\_window=120

> index\_window=200

> dcmp\_mem=1 - this value is in MB

> block\_size=250 - this value is in MB

#Test case

From the folder JDNA download the JDNA.jar, config,ini, ref\_ex.raw and inp\_ex.raw.

To test compression run:

`java -jar JDNA.jar COMPRESS ref_ex.raw inp_ex.raw cmp.raw`

Then you can test decompression runnin:

`java -jar JDNA.jar DECOMPRESS ref_ex.raw cmp.raw dcmp.raw`.

Once both tasks are complete, you can use md5sum to verify that the decompresses file matches the input file:

`md5sum inp_ex.raw dcmp.raw`