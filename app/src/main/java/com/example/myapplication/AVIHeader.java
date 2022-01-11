package com.example.myapplication;

public class AVIHeader {
    private int dwMicroSecPerFrame;
    private int dwMaxBytesPerSec;
    private int dwPaddingGranularity;
    private int dwFlags;
    private int dwTotalFrames;
    private int dwInitialFrames;
    private int dwStreams;
    private int dwSuggestedBufferSize;
    private int dwWidth;
    private int dwHeight;

    public AVIHeader() {
    }

    public int getDwMicroSecPerFrame() {
        return dwMicroSecPerFrame;
    }

    public void setDwMicroSecPerFrame(final int dwMicroSecPerFrame) {
        this.dwMicroSecPerFrame = dwMicroSecPerFrame;
    }

    public int getDwMaxBytesPerSec() {
        return dwMaxBytesPerSec;
    }

    public void setDwMaxBytesPerSec(final int dwMaxBytesPerSec) {
        this.dwMaxBytesPerSec = dwMaxBytesPerSec;
    }

    public int getDwPaddingGranularity() {
        return dwPaddingGranularity;
    }

    public void setDwPaddingGranularity(final int dwPaddingGranularity) {
        this.dwPaddingGranularity = dwPaddingGranularity;
    }

    public int getDwFlags() {
        return dwFlags;
    }

    public void setDwFlags(final int dwFlags) {
        this.dwFlags = dwFlags;
    }

    public int getDwTotalFrames() {
        return dwTotalFrames;
    }

    public void setDwTotalFrames(final int dwTotalFrames) {
        this.dwTotalFrames = dwTotalFrames;
    }

    public int getDwInitialFrames() {
        return dwInitialFrames;
    }

    public void setDwInitialFrames(final int dwInitialFrames) {
        this.dwInitialFrames = dwInitialFrames;
    }

    public int getDwStreams() {
        return dwStreams;
    }

    public void setDwStreams(final int dwStreams) {
        this.dwStreams = dwStreams;
    }

    public int getDwSuggestedBufferSize() {
        return dwSuggestedBufferSize;
    }

    public void setDwSuggestedBufferSize(final int dwSuggestedBufferSize) {
        this.dwSuggestedBufferSize = dwSuggestedBufferSize;
    }

    public int getDwWidth() {
        return dwWidth;
    }

    public void setDwWidth(final int dwWidth) {
        this.dwWidth = dwWidth;
    }

    public int getDwHeight() {
        return dwHeight;
    }

    public void setDwHeight(final int dwHeight) {
        this.dwHeight = dwHeight;
    }
}
