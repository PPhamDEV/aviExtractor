package com.example.myapplication;

import androidx.annotation.IntDef;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.IndexSeekMap;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.ParsableByteArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public class AviExtractor implements Extractor, SeekMap {

    private static final int MAX_AUDIO_STREAMS = 1;
    private boolean isEndOfInput = false;

    @Override
    public boolean isSeekable() {
        return false;
    }

    @Override
    public long getDurationUs() {
        return 100000000;
    }

    @Override
    public SeekPoints getSeekPoints(long timeUs) {
        return null;
    }


    @IntDef({
            STATE_READING_AVI_HEADER,
            STATE_READING_TAG_DATA
    })
    private @interface States {}

    private static final int STATE_READING_AVI_HEADER = 1;
    private static final int STATE_READING_TAG_DATA = 2;

    private static final int AVI_HEADER_SIZE = 4;
    private static final int AVI_TAG = 0x41564920;

    //private int chunkSize;

    private AVIHeader aviHeader;

    private int currentChunkSize = 0;
    private @States int state;
    private ExtractorOutput extractorOutput;
    private TrackOutput audioTrack;
    private TrackOutput videoTrack;

    private byte[] idx = null;
    private int numberOfAudioChannels = 0;
    private long durationUs = C.TIME_UNSET;

    private int index = 0;
    private String streamVideoTag;



    public AviExtractor() {
        state = STATE_READING_AVI_HEADER;
    }


    @Override
    public boolean sniff(ExtractorInput input) throws IOException {
        return true;
    }

    @Override
    public void init(ExtractorOutput output) {
        this.extractorOutput = output;


        System.out.println("Extractor INIT");
    }

    private final int readByte(ExtractorInput input) throws IOException {
        byte[] data = new byte[ 1 ];
        input.read( data, 0, 1 );
        return data[0];
    }

    /**
     * Read up to 4 bytes
     */
    private final int readBytes(ExtractorInput input, int number ) throws IOException {
        byte[] buffer = new byte[ number ];
        int read = input.read( buffer, 0, number );

        if ( read != buffer.length ) {
            if ( read < 0 ) throw new IOException( "End of Stream" );
            for ( int i = read; i < buffer.length; i++ ) buffer[ i ] = (byte)readByte(input);
        }

        /**
         * Create integer
         */
        switch ( number ) {
            case 1: return (buffer[ 0 ] & 0xff);
            case 2: return (buffer[ 0 ] & 0xff) | ((buffer[ 1 ] & 0xff) << 8);
            case 3: return (buffer[ 0 ] & 0xff) | ((buffer[ 1 ] & 0xff) << 8) | ((buffer[ 2 ] & 0xff) << 16);
            case 4: return (buffer[ 0 ] & 0xff) | ((buffer[ 1 ] & 0xff) << 8) | ((buffer[ 2 ] & 0xff) << 16) | ((buffer[ 3 ] & 0xff) << 24);
            default: throw new IOException( "Illegal Read quantity" );
        }
    }

    private byte[] readBuffer(ExtractorInput input, int size) throws IOException {
        byte[] buffer = new byte[ size ];

        int read = 0;
        while ( read < size ) {
            int next = input.read( buffer, read, size - read );
            if ( next < 0 ) throw new IOException( "End of Stream" );
            read += next;
        }
        return buffer;
    }

    @Override
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        Assertions.checkStateNotNull(extractorOutput);
        while (true) {
            switch (state) {
                case STATE_READING_AVI_HEADER:
                    if (!readAviHeader(input)) {
                        return RESULT_END_OF_INPUT;
                    }
                    break;
                case STATE_READING_TAG_DATA:
                    //System.out.println(input.getPosition());
                    long a = input.getPosition();
                    long b = input.getLength();
                    boolean o = readFrame(input, true);
                    if (!o) {
                        if(isEndOfInput == true){
                            return RESULT_END_OF_INPUT;
                        }
                        return RESULT_CONTINUE;
                    }

                    break;
                default:
                    // Never happens.
                    throw new IllegalStateException();
            }
        }
    }
    private boolean getChunk(ExtractorInput input) throws IOException {
        String command = new String( readBuffer(input, 4 ), "ASCII" ).toUpperCase();
        currentChunkSize = readBytes(input,4);
        if(currentChunkSize == C.RESULT_END_OF_INPUT)
        {
            isEndOfInput = true;
            return true;
        }
        /**
         * Skip LIST and RIFF
         */
        while (   "LIST".equals( command )
                || "RIFF".equals( command ) ) {
            readBuffer(input, 4 );
            command = new String( readBuffer(input, 4 ), "ASCII" ).toUpperCase();
            currentChunkSize = readBytes(input,4);
        }

        /**
         * Look for ##db ##dc ##wb [video]
         */
        String videoTag = streamVideoTag.substring(0, 3);
        if ( command.substring(0, 3).equalsIgnoreCase( videoTag ) &&
                (command.charAt(3) == 'B' || command.charAt(3) == 'C') ) {
            /**
             * Video
             */

            ParsableByteArray chunk = new ParsableByteArray(readBuffer(input, currentChunkSize));

            byte[] chunkHeader = new byte[4];
            byte[] payload = new byte[currentChunkSize - 4];


            chunk.readBytes(chunkHeader,0, 4);
            chunk.readBytes(payload, 0, currentChunkSize - 4);

            TrackOutput to = videoTrack;

            to.sampleData(new ParsableByteArray(new byte[]{0,0,0,1}), 4);
            to.sampleData(new ParsableByteArray(payload), currentChunkSize-4);

            int flag = payload[0] % 10 == 5 || index == 0 ? 1 : 0;



            //positions = addX(positions, currentChunkSize +3 + positions[index]);
            //timesUs = addX(timesUs, (long) index * rateVideo *1000L/scaleVideo);
            to.sampleMetadata((long) index * aviHeader.getDwMicroSecPerFrame(), flag, currentChunkSize, 0, null);

            index++;

            return true;
        }
        /**
         * Match Audio strings
         */
        for ( int i = 0; i < numberOfAudioChannels; i++ ) {
//            if ( command.equalsIgnoreCase( audio[i].getAudioTag() ) ) {
            /**
             * Audio
             */
            readBuffer(input, currentChunkSize);
            //audioTrack.sampleData(new ParsableByteArray(readBuffer(input, currentChunkSize)), currentChunkSize);
            return false;
//          }
        }
        throw new IOException( "Not header " + command );
    }
    public static long[] addX( long arr[], long x)
    {
        int i;

        // create a new array of size n+1
        long newarr[] = new long[arr.length + 1];

        // insert the elements from
        // the old array into the new array
        // insert all elements till n
        // then insert x at n+1
        for (i = 0; i < arr.length; i++)
            newarr[i] = arr[i];

        newarr[arr.length] = x;

        return newarr;
    }

    public boolean readFrame(ExtractorInput input, boolean video) throws IOException {
        boolean isVideo;


            isVideo = getChunk(input);
            if(isEndOfInput) return false;
            if ( (currentChunkSize & 1) == 1 ) readBuffer(input, 1 );

        return false;
    }
    private boolean readAviHeader(ExtractorInput input) throws IOException {
        /**
         * Check this is an AVI file
         */

        String id = new String( readBuffer( input, 4 ), "ASCII" );
        readBuffer( input, 4 );
        String type = new String( readBuffer( input, 4 ), "ASCII" );

        boolean a = !"RIFF".equalsIgnoreCase( id )
                || !"AVI ".equalsIgnoreCase( type );

        System.out.println("RIFF == id" + "RIFF".equalsIgnoreCase( id ));
        System.out.println("AVI  == type" + "AVI ".equalsIgnoreCase( type ));

        byte[] hdrl = null;

        while ( true ) {
            String command = new String( readBuffer(input, 4 ), "ASCII" );
            int length = (readBytes(input, 4) + 1) &~1;

            if ( "LIST".equals( command ) ) {
                command = new String( readBuffer(input, 4 ), "ASCII" ); length -= 4;
                if ( "movi".equals( command ) ) {
                    break;
                }
                if ( "hdrl".equals( command ) ) {
                    hdrl = readBuffer(input, length );
                }
                if ( "idx1".equals( command ) ) {
                    idx = readBuffer(input, length );
                }
                if ( "iddx".equals( command ) ) {
                    idx = readBuffer(input, length );
                }

            } else {
                readBuffer(input, length );
            }
        }

        int streamNumber = 0;
        int lastTagID = 0;
        for ( int i = 0; i < hdrl.length; ) {
            String command = new String(hdrl, i, 4);
            int size = str2ulong(hdrl, i + 4);

            if ("LIST".equals(command)) {
                i += 12;
                continue;
            }


            String command2 = new String(hdrl, i + 8, 4);
            if ("avih".equals(command)){
                aviHeader = new AVIHeader();
                aviHeader.setDwMicroSecPerFrame(str2ulong(hdrl, i + 8));
                aviHeader.setDwMaxBytesPerSec(str2ulong(hdrl, i + 12));
                aviHeader.setDwPaddingGranularity(str2ulong(hdrl, i + 16));
                aviHeader.setDwFlags(str2ulong(hdrl, i + 20));
                aviHeader.setDwTotalFrames(str2ulong(hdrl, i + 24));
                aviHeader.setDwInitialFrames(str2ulong(hdrl, i + 28));
                aviHeader.setDwStreams(str2ulong(hdrl, i + 32));
                aviHeader.setDwSuggestedBufferSize(str2ulong(hdrl, i + 36));
                aviHeader.setDwWidth(str2ulong(hdrl, i + 40));
                aviHeader.setDwHeight(str2ulong(hdrl, i + 44));
                this.durationUs = (long) aviHeader.getDwTotalFrames() * aviHeader.getDwMicroSecPerFrame();
            }
            if ("strh".equals(command)) {
                lastTagID = 0;
                if ("vids".equals(command2)) {
                    StreamHeader strhVideo = new StreamHeader();
                    strhVideo.setFccHandler(new String(hdrl, i + 12, 4));
                    strhVideo.setDwFlags(str2ulong(hdrl, i + 16));
                    strhVideo.setwPriority(str2ulong(hdrl, i + 20));
                    strhVideo.setwLanguage(str2ulong(hdrl, i + 24));
                    strhVideo.setDwScale(str2ulong(hdrl, i + 28));
                    strhVideo.setDwRate(str2ulong(hdrl, i + 32));
                    strhVideo.setDwStart(str2ulong(hdrl, i + 36));
                    strhVideo.setDwLength(str2ulong(hdrl, i + 40));
                    strhVideo.setDwSuggestedBufferSize(str2ulong(hdrl, i + 44));
                    strhVideo.setDwQuality(str2ulong(hdrl, i + 48));
                    strhVideo.setDwSampleSize(str2ulong(hdrl, i + 52));
                    strhVideo.setRcFrameLeft(str2ulong(hdrl, i + 56));
                    strhVideo.setRcFrameTop(str2ulong(hdrl, i + 60));
                    strhVideo.setRcFrameRight(str2ulong(hdrl, i + 64));
                    strhVideo.setRcFrameBottom(str2ulong(hdrl, i + 68));
                    streamVideoTag = getVideoTag(0);

                    //TrackOutput trackOutput = extractorOutput.track(/* id= */ 0, C.TRACK_TYPE_IMAGE);


                    //videoTrack = this.extractorOutput.track(0, C.TRACK_TYPE_VIDEO);
                    byte[][] arr = new byte[][]{new byte[]{0, 0, 0, 1, 103, 66, -64, 30, -39, 1, -32, -113, -21, 1, 16, 0, 0, 3, 0, 16, 0, 0, 3, 3, -64, -15, 98, -28, -128},new byte[]{0, 0, 0, 1, 104, -53, -116, -78}};
                    lastTagID = 1;
                    Format format = new Format.Builder()
                                    .setId(1)
                                    .setFrameRate(901 / (durationUs / 1000000f))
                                    .setCodecs(null)
                                    .setSampleMimeType("video/avc")
                                    .setWidth(480)
                                    .setHeight(270)
                                    .setMaxInputSize(51650)
                                    .setInitializationData(new ArrayList<>(Arrays.asList(arr)))
                                    .build();
                    videoTrack = this.extractorOutput.track(0, C.TRACK_TYPE_VIDEO);
                    videoTrack.format(format);

                }
                if ("auds".equalsIgnoreCase(command2)) {


                    Format format =
                            new Format.Builder()
                                    .setId(2)
                                    .build();
                    audioTrack = this.extractorOutput.track(1 + numberOfAudioChannels, C.TRACK_TYPE_AUDIO);
                    audioTrack.format(format);
                    numberOfAudioChannels++;
                    lastTagID = 2;

                }
            }
            if ( "strf".equalsIgnoreCase( command ) ) {
                if ( lastTagID == 1 ) {
                    /**
                     * Video information
                     */
                    byte[] information = new byte[ size - 4 ];
                    System.arraycopy( hdrl, i + 4,
                            information, 0, information.length );
                }
                if ( lastTagID == 2) {
                    /**
                     * Audio information
                     */
                    byte[] information = new byte[ size - 4 ];
                    System.arraycopy( hdrl, i + 4,
                            information, 0, information.length );

                }
            }
            i += size + 8;
        }

        this.extractorOutput.endTracks();
        this.extractorOutput.seekMap(this);

        state = STATE_READING_TAG_DATA;
        return true;
    }

    public String getVideoTag(int streamNumber) {
        return new String( new char[] { (char)((streamNumber / 10) + '0'),
                (char)((streamNumber % 10) + '0'),
                'd',
                'b' } );
    }
    /**
     * str2ulong
     */
    public static final int str2ulong( byte[] data, int i ) {
        return    (data[ i ] & 0xff)
                | ((data[ i + 1 ] & 0xff) << 8 )
                | ((data[ i + 2 ] & 0xff) << 16 )
                | ((data[ i + 3 ] & 0xff) << 24 );
    }
    //@RequiresNonNull("extractorOutput")
    /*private boolean readAviHeader(ExtractorInput input) throws IOException {
        if (!input.readFully(headerBuffer.getData(), 0, AVI_HEADER_SIZE, true)) {
            // We've reached the end of the stream.
            return false;
        }

        int flags = headerBuffer.readUnsignedInt24();
        System.out.println("Exctractor readAviHeader READ : " + flags);

        //headerBuffer.skipBytes(4);
        return true;
    }*/

    @Override
    public void seek(long position, long timeUs) {
        System.out.println("Exctractor SEEK");
        System.out.println("Exctractor SEEK: position=" + position);
        if (position == 0) {
            state = STATE_READING_AVI_HEADER;
            //outputFirstSample = false;
        }
    }

    @Override
    public void release() {
        System.out.println("Exctractor RELEASE");
    }


}



