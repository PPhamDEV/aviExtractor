package com.example.myapplication;

public class StreamFormatAudio {
    private int wFormatTag;
    private int nChannels;
    private int nSamplePerSec;
    private int nAvgBytesPerSec;
    private int nBlockAlign;
    private int wBitsPerSample;
    private int cbSize;
    private byte[] extra;

    public StreamFormatAudio() {
    }

    public int getwFormatTag() {
        return wFormatTag;
    }

    public void setwFormatTag(final int wFormatTag) {
        this.wFormatTag = wFormatTag;
    }

    public int getnChannels() {
        return nChannels;
    }

    public void setnChannels(final int nChannels) {
        this.nChannels = nChannels;
    }

    public int getnSamplePerSec() {
        return nSamplePerSec;
    }

    public void setnSamplePerSec(final int nSamplePerSec) {
        this.nSamplePerSec = nSamplePerSec;
    }

    public int getnAvgBytesPerSec() {
        return nAvgBytesPerSec;
    }

    public void setnAvgBytesPerSec(final int nAvgBytesPerSec) {
        this.nAvgBytesPerSec = nAvgBytesPerSec;
    }

    public int getnBlockAlign() {
        return nBlockAlign;
    }

    public void setnBlockAlign(final int nBlockAlign) {
        this.nBlockAlign = nBlockAlign;
    }

    public int getwBitsPerSample() {
        return wBitsPerSample;
    }

    public void setwBitsPerSample(final int wBitsPerSample) {
        this.wBitsPerSample = wBitsPerSample;
    }

    public int getCbSize() {
        return cbSize;
    }

    public void setCbSize(final int cbSize) {
        this.cbSize = cbSize;
    }

    public byte[] getExtra() {
        return extra;
    }

    public void setExtra(final byte[] extra) {
        this.extra = extra;
    }
}
