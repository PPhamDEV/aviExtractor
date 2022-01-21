package com.example.myapplication;

import androidx.annotation.IntDef;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.ParsableByteArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class AviExtractor implements Extractor, SeekMap {

    private static final int MAX_AUDIO_STREAMS = 1;
    private boolean isEndOfInput = false;

    @Override
    public boolean isSeekable() {
        return false;
    }

    @Override
    public long getDurationUs() {
        return this.durationUs;
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
    private byte[][] initData = null;


    public AviExtractor() {
        state = STATE_READING_AVI_HEADER;
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
                boolean o = readFrame(input);
                if (!o) {
                    if(isEndOfInput){
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

    //TODO: peek() benutzen, um nach SPS und PPS zu suchen

    private boolean readAviHeader(ExtractorInput input) throws IOException {
        /**
         * Check this is an AVI file
         */

        readBuffer( input, 12 );

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

        int lastTagID = 0;
        for ( int i = 0; i < hdrl.length; ) {
            String command = new String(hdrl, i, 4);
            int size = convertByteArrayToUInt(hdrl, i + 4);

            if ("LIST".equals(command)) {
                i += 12;
                continue;
            }


            String command2 = new String(hdrl, i + 8, 4);
            if ("avih".equals(command)){
                this.aviHeader = createAviHeader(hdrl, i);
                this.durationUs =
                        (long) aviHeader.getDwTotalFrames() * aviHeader.getDwMicroSecPerFrame();
            }
            if ("strh".equals(command)) {
                lastTagID = 0;
                if ("vids".equals(command2)) {

                    StreamHeader strhVideo = createStreamHeader(hdrl, i);
                    streamVideoTag = getVideoTag(0);

                    //searching for SPS and PSP
                    initData = prepareInitData(input);
//                    byte[] SPS = new byte[]{0,0,0,1,71,77,64,30,-103,-96,40,11,-2,88,64,0,0,-6,0,0,48,-44,33};
//                    byte[] PPS = new byte[]{0,0,0,1,72,-18,60,-128};
//                    byte[][] initData = new byte[][]{SPS,PPS};
                    lastTagID = 1;
                    Format format = new Format.Builder()
                            .setId(1)
                            .setFrameRate(strhVideo.getDwLength() / (this.durationUs / 1000000f))
                            .setCodecs(null)
                            .setSampleMimeType(MimeTypes.VIDEO_H264)
                            .setWidth(aviHeader.getDwWidth())
                            .setHeight(aviHeader.getDwHeight())
                            .setMaxInputSize(aviHeader.getDwMaxBytesPerSec())
                            .setInitializationData(new ArrayList<>(Arrays.asList(initData)))
                            .build();
                    videoTrack = this.extractorOutput.track(0, C.TRACK_TYPE_VIDEO);
                    videoTrack.format(format);

                }
                if ("auds".equals(command2)) {
                    StreamHeader strhAudio = createStreamHeader(hdrl, i);
                    numberOfAudioChannels++;

                    byte[] test = new byte[hdrl.length - i - 64];
                    System.arraycopy(hdrl, i + 64, test, 0, hdrl.length - i - 64);

                    String formatTagHex = Integer.toHexString(test[17] & 0xff) + Integer.toHexString(test[16] & 0xff);
                    int formatTag = Integer.decode("0x" + formatTagHex);

                    String mimeType = MimeTypes.AUDIO_UNKNOWN;
                    if(formatTag == 255) mimeType = MimeTypes.AUDIO_AAC;

                    //byte[] initializationData = new byte[]{test[34], test[35]}; // TODO: IST <BLOB> IMMER VON GRÖßE 2?

                    Format format =
                            new Format.Builder()
                                    .setId(2)
                                    .setCodecs("mp4a.40.2")
                                    .setSampleMimeType(mimeType)
                                    .setChannelCount(2)
                                    .setSampleRate(strhAudio.getDwRate())
                                    .setInitializationData(null)
                                    .build();
                    audioTrack = this.extractorOutput.track(numberOfAudioChannels, C.TRACK_TYPE_AUDIO);
                    audioTrack.format(format);

                    lastTagID = 2;

                }
            }
            if ( "strf".equals( command ) ) {
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

    /*F7 F7 F7 F6 F7 F7 F6 F7 F7 F7 F7 F7 F7 F7 F7 F7 30 30 64 62 FC 7D 00 00 00 00 00 01 47 4D 40 1E 99 A0 28 0B FE 58 40 00 00 FA 00 00 30 D4 21 00 00 00 01 48 EE 3C 80 00 00 00 01 45 B8 00 E7 FF*/
    //[F7,F7,F7,F6,F7]
    private byte[][] prepareInitData(ExtractorInput input) throws IOException {
        long startingPeekPosition = input.getPeekPosition();
        boolean initDataFound = false;
        Byte[] last5Bytes = new Byte[5];
        byte[] currentByte = new byte[1];
        List<Byte> SPS = new ArrayList<Byte>();
        List<Byte> PPS = new ArrayList<Byte>();

        boolean SPSfound = false;
        boolean PPSfound = false;
        int counter = 0;

        while (!initDataFound){
            input.peek(currentByte, 0, 1);

            if(counter < 5){
                last5Bytes[counter] = currentByte[0];
                counter++;
                continue;
            }
            boolean startCodeFound = last5Bytes[0] == 0 && last5Bytes[1] == 0 && last5Bytes[2] == 0 && last5Bytes[3] == 1;
            if(!SPSfound) SPSfound = startCodeFound && (last5Bytes[4] & 0x0f) == 7;
            if(!PPSfound) PPSfound = startCodeFound && (last5Bytes[4] & 0x0f) == 8;

            if(SPSfound && SPS.size() == 0){
                SPS.addAll(Arrays.asList(last5Bytes));
                SPS.add(currentByte[0]);

            } else if(SPSfound && SPS.size() != 0 && PPSfound == false){
                SPS.add(currentByte[0]);
            }
            if(PPS.size() == 0 && PPSfound ){
                int SPSsize = SPS.size();
                for (int i = 1; i <= 5; i++) {
                    SPS.remove(SPSsize - i);
                }

                PPS.addAll(Arrays.asList(last5Bytes));
                PPS.add(currentByte[0]);

            } else if(PPS.size() != 0 && PPSfound && !startCodeFound){
                PPS.add(currentByte[0]);
            } else if(PPS.size() != 0 && PPSfound){
                int PPSsize = PPS.size();
                for (int i = 1; i <= 5; i++) {
                    PPS.remove(PPSsize - i);
                }
                initDataFound = true;
            }

                last5Bytes[0] = last5Bytes[1];
                last5Bytes[1] = last5Bytes[2];
                last5Bytes[2] = last5Bytes[3];
                last5Bytes[3] = last5Bytes[4];
                last5Bytes[4] = currentByte[0];
        }


        return new byte[][]{convertByteListToPrimitive(SPS), convertByteListToPrimitive(PPS)};







        /*int remainingBytes = (int) (input.getLength() - input.getPosition());
        byte[] readBytes = new byte[remainingBytes];
        long startPositionSPS = 0, startPositionPPS = 0, endPositionSPS = 0, endPositionPPS = 0;
        while (startPositionSPS == 0) {
            input.peek(readBytes, (int) (input.getPeekPosition() - startingPeekPosition), 1);
            {0,0,0,1,1234,1231,0,0,0,1, 142,412,41234, 0,0,0,1}
            boolean SPSfound = peekArray[0] == 0 && peekArray[1] == 0 && peekArray[2] == 0 && peekArray[3] == 0 && (peekArray[4] & 0x0f) == 7;

            if(SPSfound) startPositionSPS = input.getPosition();
        }
        for(int counter = 0; counter < peekArray.length; counter++){
            byte[] test = new byte[4];
            System.arraycopy(peekArray, counter, test, 0, 4);
            String startingByte  = new String( test, "ASCII" ).toUpperCase();
            pos = (int) input.getPosition() + counter;
            if(startingByte.substring(0, 3).equalsIgnoreCase( "01w" )) {
                byte[] next4bytes = new byte[4];
                System.arraycopy(peekArray, counter+4, next4bytes, 0, 4);
                int chunkSize =  getChunkSize(next4bytes);
                counter += chunkSize+7;
                continue;
            }else {
                boolean potetialSPSfound = false;
                boolean potetialPPSfound = false;
                String currentByteHex = Integer.toHexString(peekArray[counter] & 0xff);
                if(currentByteHex.length() == 1) currentByteHex = "0" + currentByteHex;

                if(!(currentByteHex.charAt(1) == '7') && !(currentByteHex.charAt(1) == '8') && startPositionSPS == 0){
                    continue;
                }else if(currentByteHex.charAt(1) == '7') {
                    potetialSPSfound = true;
                }else if(currentByteHex.charAt(1) == '8') {
                    potetialPPSfound = true;
                }
                boolean startCodeFound = peekArray[counter-4] == 0 &&
                                         peekArray[counter-3] == 0 &&
                                         peekArray[counter-2] == 0 &&
                                         peekArray[counter-1] == 1;

                boolean SPSfound = startCodeFound && potetialSPSfound;
                boolean PPSfound = startCodeFound && potetialPPSfound;
                pos = (int) input.getPosition() + counter;
                if(SPSfound){
                    startPositionSPS = counter;
                } else if(PPSfound){
                    startPositionPPS = counter;
                    endPositionSPS = counter - 5;
                } else if(endPositionSPS > 0 && startPositionPPS > 0 && startCodeFound){
                    endPositionPPS = counter - 5;
                    break;
                }
            }

        }
        pos = (int) input.getPosition() + counter;
        byte[] SPS = createParameterSet(peekArray, startPositionSPS, endPositionSPS);
        byte[] PPS = createParameterSet(peekArray, startPositionPPS, endPositionPPS);

        return new byte[][]{SPS,PPS};*/
    }
    public static byte[] convertByteListToPrimitive(List<Byte> bytes)
    {
        byte[] ret = new byte[bytes.size()];
        for (int i=0; i < ret.length; i++)
        {
            ret[i] = bytes.get(i);
        }
        return ret;
    }
    private byte[] createParameterSet(byte[] srcArray, int startPosition, int endPosition){
        int length = endPosition - startPosition + 1;

        byte[] outputArray = new byte[length + 4];

        appendHeader(outputArray);
        System.arraycopy(srcArray, startPosition, outputArray, 4, length);
        return outputArray;
    }

    private void appendHeader(byte[] array){
        array[0] = 0;
        array[1] = 0;
        array[2] = 0;
        array[3] = 1;
    }
    public static long getUnsignedInt(int x) {
        return x & (-1L >>> 32);
    }
    private AVIHeader createAviHeader(final byte[] hdrl, final int offset) {
        AVIHeader aviHeader = new AVIHeader();
        aviHeader.setDwMicroSecPerFrame(convertByteArrayToUInt(hdrl, offset + 8));
        aviHeader.setDwMaxBytesPerSec(convertByteArrayToUInt(hdrl, offset + 12));
        aviHeader.setDwPaddingGranularity(convertByteArrayToUInt(hdrl, offset + 16));
        aviHeader.setDwFlags(convertByteArrayToUInt(hdrl, offset + 20));
        aviHeader.setDwTotalFrames(convertByteArrayToUInt(hdrl, offset + 24));
        aviHeader.setDwInitialFrames(convertByteArrayToUInt(hdrl, offset + 28));
        aviHeader.setDwStreams(convertByteArrayToUInt(hdrl, offset + 32));
        aviHeader.setDwSuggestedBufferSize(convertByteArrayToUInt(hdrl, offset + 36));
        aviHeader.setDwWidth(convertByteArrayToUInt(hdrl, offset + 40));
        aviHeader.setDwHeight(convertByteArrayToUInt(hdrl, offset + 44));
        return aviHeader;
    }

    private StreamHeader createStreamHeader(final byte[] hdrl, final int offset){
        StreamHeader strh = new StreamHeader();
        strh.setFccHandler(new String(hdrl, offset + 12, 4));
        strh.setDwFlags(convertByteArrayToUInt(hdrl, offset + 16));
        strh.setwPriority(convertByteArrayToUInt(hdrl, offset + 20));
        strh.setwLanguage(convertByteArrayToUInt(hdrl, offset + 24));
        strh.setDwScale(convertByteArrayToUInt(hdrl, offset + 28));
        strh.setDwRate(convertByteArrayToUInt(hdrl, offset + 32));
        strh.setDwStart(convertByteArrayToUInt(hdrl, offset + 36));
        strh.setDwLength(convertByteArrayToUInt(hdrl, offset + 40));
        strh.setDwSuggestedBufferSize(convertByteArrayToUInt(hdrl, offset + 44));
        strh.setDwQuality(convertByteArrayToUInt(hdrl, offset + 48));
        strh.setDwSampleSize(convertByteArrayToUInt(hdrl, offset + 52));
        strh.setRcFrameLeft(convertByteArrayToUInt(hdrl, offset + 56, true));
        strh.setRcFrameTop(convertByteArrayToUInt(hdrl, offset + 58, true));
        strh.setRcFrameRight(convertByteArrayToUInt(hdrl, offset + 60, true));
        strh.setRcFrameBottom(convertByteArrayToUInt(hdrl, offset + 62, true));
        return strh;
    }

    public boolean readFrame(ExtractorInput input) throws IOException {

        getChunk(input);
        if(isEndOfInput) return false;
        if ( (currentChunkSize & 1) == 1 ) readBuffer(input, 1 );

        return false;
    }

    private boolean getChunk(ExtractorInput input) throws IOException {
        String command = "";
        try {
            command = new String( readBuffer(input, 4 ), "ASCII" ).toUpperCase();
        } catch (IOException e) {
            isEndOfInput = true;
            return true;
        }
        int position = (int) input.getPosition();
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
            if(currentChunkSize == 0) {
                return true;
            }
            byte[] chunkHeader = new byte[4];
            byte[] payload = new byte[currentChunkSize - 4];


            chunk.readBytes(chunkHeader,0, 4);
            chunk.readBytes(payload, 0, currentChunkSize - 4);

            TrackOutput to = videoTrack;

            to.sampleData(new ParsableByteArray(new byte[]{0,0,0,1}), 4);
            to.sampleData(new ParsableByteArray(payload), currentChunkSize-4);

            int flag = payload[0] % 10 == 5 || index == 0 ? 1 : 0;


            to.sampleMetadata((long) index * aviHeader.getDwMicroSecPerFrame(),
                              flag,
                              currentChunkSize,
                              0,
                              null);

            index++;

            return true;
        }
        /**
         * Match Audio strings
         */
        for ( int i = 0; i < numberOfAudioChannels; i++ ) {
            /**
             * Audio
             */
            // TODO: sampleData und sampleMetadata für audio
            readBuffer(input, currentChunkSize);
            return false;
        }
        throw new IOException( "Not header " + command );
    }

    private final int getChunkSize(byte[] src){
        return (src[ 0 ] & 0xff) | ((src[ 1 ] & 0xff) << 8) | ((src[ 2 ] & 0xff) << 16) | ((src[ 3 ] & 0xff) << 24);
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

    public String getVideoTag(int streamNumber) {
        return new String( new char[] { (char)((streamNumber / 10) + '0'),
                (char)((streamNumber % 10) + '0'),
                'd',
                'b' } );
    }
    public static final int convertByteArrayToUInt(byte[] data, int i){
        return convertByteArrayToUInt(data, i, false);
    }
    public static final int convertByteArrayToUInt(byte[] data, int i, boolean isByte) {

        if(isByte){
            return    (data[ i ] & 0xff)  | ((data[ i + 1 ] & 0xff) << 8 );
        }

        return    (data[ i ] & 0xff)
                  | ((data[ i + 1 ] & 0xff) << 8 )
                  | ((data[ i + 2 ] & 0xff) << 16 )
                  | ((data[ i + 3 ] & 0xff) << 24 );
    }

    private final int readByte(ExtractorInput input) throws IOException {
        byte[] data = new byte[ 1 ];
        input.read( data, 0, 1 );
        return data[0];
    }
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


    @Override
    public boolean sniff(ExtractorInput input) throws IOException {
        return true;
    }

    @Override
    public void init(ExtractorOutput output) {
        this.extractorOutput = output;
    }
}



