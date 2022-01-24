package com.example.myapplication;

import android.media.MediaFormat;

import androidx.annotation.IntDef;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.SeekPoint;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.ParsableByteArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.android.exoplayer2.C.BUFFER_FLAG_KEY_FRAME;
import static com.google.android.exoplayer2.util.Assertions.checkNotNull;


public class AviExtractor implements Extractor, SeekMap {

    private static final int MAX_AUDIO_STREAMS = 1;
    private boolean isEndOfInput = false;
    private long[] timesUsAudio;
    private final byte[] chunkStartCode = new byte[]{ 0,0,0,1 };

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
        if (audioTrack == null && videoTrack == null) {
            return new SeekPoints(SeekPoint.START);
        }

        return null;
    }


    @IntDef({
            STATE_READING_AVI_HEADER,
            STATE_READING_TAG_DATA
    })
    private @interface States {}

    private static final int STATE_READING_AVI_HEADER = 1;
    private static final int STATE_READING_TAG_DATA = 2;

    private static final int START_CODE_LENGTH = 4;
    private static final int AVI_TAG = 0x41564920;

    //private int chunkSize;

    private AVIHeader aviHeader;
    private long[] timesUsVideo;

    private int currentChunkSize = 0;
    private @States int state;
    private ExtractorOutput extractorOutput;
    private TrackOutput audioTrack;
    private TrackOutput videoTrack;

    private byte[] idx = null;
    private int numberOfAudioChannels = 0;
    private long durationUs = C.TIME_UNSET;

    private int index = 0;
    private int indexAudio = 0;
    private String streamVideoTag;



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
                readFrame(input);

                if(isEndOfInput){
                    return RESULT_END_OF_INPUT;
                }
                return RESULT_CONTINUE;
            default:
                // Never happens.
                throw new IllegalStateException();
            }
        }
    }
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

        while (!initDataFound) {
            input.peek(currentByte, 0, 1);

            if (counter < 5) {
                last5Bytes[counter] = currentByte[0];
                counter++;
                continue;
            }
            boolean startCodeFound = last5Bytes[0] == 0
                                     && last5Bytes[1] == 0
                                     && last5Bytes[2] == 0
                                     && last5Bytes[3] == 1;
            if (!SPSfound) SPSfound = startCodeFound && (last5Bytes[4] & 0x0f) == 7;
            if (!PPSfound) PPSfound = startCodeFound && (last5Bytes[4] & 0x0f) == 8;

            if (SPSfound && SPS.size() == 0) {
                SPS.addAll(Arrays.asList(last5Bytes));

            } else if (SPSfound && SPS.size() != 0 && PPSfound == false) {
                SPS.add(currentByte[0]);
            }
            if (PPS.size() == 0 && PPSfound) {
                int SPSsize = SPS.size();
                for (int i = 1; i <= 5; i++) {
                    SPS.remove(SPSsize - i);
                }

                PPS.addAll(Arrays.asList(last5Bytes));

            } else if (PPS.size() != 0 && PPSfound && !startCodeFound) {
                PPS.add(currentByte[0]);
            } else if (PPS.size() != 0 && PPSfound) {
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
                int totalFrames = aviHeader.getDwTotalFrames();
                int microSecPerFrame = aviHeader.getDwMicroSecPerFrame();
                this.durationUs =
                        (long) totalFrames * microSecPerFrame;
                this.timesUsVideo = new long[totalFrames + 1];
                this.timesUsAudio = new long[1431]; //TODO: WOHER KOMMT DAS?
                for (int j = 0; j < timesUsVideo.length; j++) {
                    timesUsVideo[j] = (long) j * microSecPerFrame;
                }
            }
            if ("strh".equals(command)) {
                lastTagID = 0;
                if ("vids".equals(command2)) {

                    StreamHeader strhVideo = createStreamHeader(hdrl, i);
                    streamVideoTag = getVideoTag(0);


                    lastTagID = 1;
                    Format format = new Format.Builder()
                            .setId(1)
                            .setFrameRate(strhVideo.getDwLength() / (this.durationUs / 1000000f))
                            .setCodecs(null)
                            .setSampleMimeType(MimeTypes.VIDEO_H264)
                            .setWidth(aviHeader.getDwWidth())
                            .setHeight(aviHeader.getDwHeight())
                            .setMaxInputSize(aviHeader.getDwMaxBytesPerSec())
                            .setInitializationData(new ArrayList<>(Arrays.asList(prepareInitData(input))))
                            .build();
                    videoTrack = this.extractorOutput.track(0, C.TRACK_TYPE_VIDEO);
                    videoTrack.format(format);

                }
                if ("auds".equals(command2)) {
                    StreamHeader strhAudio = createStreamHeader(hdrl, i);
                    int strfStartPosition = i + size + 8;
                    int strfSize = convertByteArrayToUInt(hdrl, strfStartPosition + 4);

                    byte[] strf = new byte[strfSize + 8];
                    System.arraycopy(hdrl, strfStartPosition, strf, 0, strfSize + 8);

                    String formatTagHex = Integer.toHexString(strf[9] & 0xff) + Integer.toHexString(strf[8] & 0xff);
                    int formatTag = Integer.decode("0x" + formatTagHex);

                    String mimeType = MimeTypes.AUDIO_UNKNOWN;
                    if(formatTag == 255) mimeType = MimeTypes.AUDIO_AAC;

                    byte[] initializationData = new byte[0];
                    boolean isInitializationDataPresent = strfSize > 16;

                    if(isInitializationDataPresent){
                        String cbSizeHex = Integer.toHexString(strf[25] & 0xff) + Integer.toHexString(strf[24] & 0xff);
                        int cbSize = Integer.decode("0x" + cbSizeHex);
                        initializationData = new byte[cbSize];
                        if (cbSize >= 0) System.arraycopy(strf, 26, initializationData, 0, cbSize);
                    }

                    numberOfAudioChannels = strf[10];

                    for (int j = 0; j < timesUsAudio.length; j++) {
                        timesUsAudio[j] =
                                1000000L * numberOfAudioChannels * j * strhAudio.getDwScale() / strhAudio.getDwRate();
                    }
                    Format.Builder formatBuilder =
                            new Format.Builder()
                                    .setId(2)
                                    .setCodecs("mp4a.40.2") // TODO: Für Kamera-Videos funktioniert der mp4a codec nicht, weil dwFormatTag != 255. Wir müssen herausfinden, was man macht beim Format == 7, weil die Kameras diesen benutzen
                                    .setSampleMimeType(mimeType)
                                    .setChannelCount(numberOfAudioChannels) //
                                    .setEncoderPadding(64) // TODO: HERAUSFINDEN WOHER DIESER WERT KOMMT UND ERSETZEN
                                    .setMaxInputSize(329) // TODO: HERAUSFINDEN WOHER DIESER WERT KOMMT UND ERSETZEN
                                    .setLanguage("und")
                                    .setSampleRate(strhAudio.getDwRate());

                    if (isInitializationDataPresent) {
                        formatBuilder.setInitializationData(new ArrayList<>(Collections.singletonList(
                                initializationData)));
                    }

                    audioTrack = this.extractorOutput.track(numberOfAudioChannels, C.TRACK_TYPE_AUDIO);
                    audioTrack.format(formatBuilder.build());

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

    private void getChunk(ExtractorInput input) throws IOException {
        String command = "";
        try {
            command = new String( readBuffer(input, 4 ), "ASCII" ).toUpperCase();
        } catch (IOException e) {
            isEndOfInput = true;
            return;
        }
        currentChunkSize = readBytes(input,4);
        if(currentChunkSize == C.RESULT_END_OF_INPUT)
        {
            isEndOfInput = true;
            return;
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

            passChunkData(input, currentChunkSize, videoTrack, timesUsVideo, index, true);
            index++;
            return;
        } else if(command.equals( "JUNK" )) {
            input.skipFully(currentChunkSize);
            return;
        } else {
            /**
             * Match Audio strings
             */
            passChunkData(input, currentChunkSize, audioTrack, timesUsAudio, indexAudio, false);
            indexAudio++;
        }

    }

    private final void passChunkData(ExtractorInput input, int currentChunkSize, TrackOutput trackOutput, long[] timestampUs, int chunkIndex, boolean isVideoChunk) throws IOException {
        ParsableByteArray chunk = new ParsableByteArray(readBuffer(input, currentChunkSize));
        if(currentChunkSize == 0) {
            return;
        }

        final int payloadSize;
        if (isVideoChunk){
            payloadSize = currentChunkSize-START_CODE_LENGTH;
            chunk.skipBytes(START_CODE_LENGTH);
        } else {
            payloadSize = currentChunkSize;
        }
        byte[] payload = new byte[payloadSize];

        chunk.readBytes(payload, 0, payloadSize);

        int flag;

        if (isVideoChunk){
            String payloadFirstByteHex = Integer.toHexString(payload[0] & 0xff);
            if(payloadFirstByteHex.length() == 1) payloadFirstByteHex = "0" + payloadFirstByteHex;

            flag = 0;
            if(payloadFirstByteHex.charAt(1) == '5' || chunkIndex == 0) flag = BUFFER_FLAG_KEY_FRAME;
        } else {
            flag = 1;
        }



        trackOutput.sampleData(new ParsableByteArray(this.chunkStartCode), START_CODE_LENGTH);
        trackOutput.sampleData(new ParsableByteArray(payload), payloadSize);

        trackOutput.sampleMetadata(timestampUs[chunkIndex],
                                   flag,
                                   currentChunkSize,
                                   0,
                                   null);

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
    public static final int convertByteArrayToUInt(byte[] data, int offsetInData){
        return convertByteArrayToUInt(data, offsetInData, false);
    }
    public static final int convertByteArrayToUInt(byte[] data, int offsetInData, boolean isByte) {

        if(isByte){
            return    (data[ offsetInData ] & 0xff)  | ((data[ offsetInData + 1 ] & 0xff) << 8 );
        }

        return    (data[ offsetInData ] & 0xff)
                  | ((data[ offsetInData + 1 ] & 0xff) << 8 )
                  | ((data[ offsetInData + 2 ] & 0xff) << 16 )
                  | ((data[ offsetInData + 3 ] & 0xff) << 24 );
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



