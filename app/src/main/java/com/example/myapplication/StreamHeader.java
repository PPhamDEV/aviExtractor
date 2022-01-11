package com.example.myapplication;

public class StreamHeader {
    private String fccType;
    private String fccHandler;
    private int dwFlags;
    private int wPriority;
    private int wLanguage;
    private int dwInitialFrames;
    private int dwScale;
    private int dwRate;
    private int dwStart;
    private int dwLength;
    private int dwSuggestedBufferSize;
    private int dwQuality;
    private int dwSampleSize;
    private int rcFrameLeft;
    private int rcFrameTop;
    private int rcFrameRight;
    private int rcFrameBottom;

    public StreamHeader() {
        fccType = "";
        fccHandler = "";
    }

    public String getFccType() {
        return fccType;
    }

    public void setFccType(final String fccType) {
        this.fccType = fccType;
    }

    public String getFccHandler() {
        return fccHandler;
    }

    public void setFccHandler(final String fccHandler) {
        this.fccHandler = fccHandler;
    }

    public int getDwFlags() {
        return dwFlags;
    }

    public void setDwFlags(final int dwFlags) {
        this.dwFlags = dwFlags;
    }

    public int getwPriority() {
        return wPriority;
    }

    public void setwPriority(final int wPriority) {
        this.wPriority = wPriority;
    }

    public int getwLanguage() {
        return wLanguage;
    }

    public void setwLanguage(final int wLanguage) {
        this.wLanguage = wLanguage;
    }

    public int getDwInitialFrames() {
        return dwInitialFrames;
    }

    public void setDwInitialFrames(final int dwInitialFrames) {
        this.dwInitialFrames = dwInitialFrames;
    }

    public int getDwScale() {
        return dwScale;
    }

    public void setDwScale(final int dwScale) {
        this.dwScale = dwScale;
    }

    public int getDwRate() {
        return dwRate;
    }

    public void setDwRate(final int dwRate) {
        this.dwRate = dwRate;
    }

    public int getDwStart() {
        return dwStart;
    }

    public void setDwStart(final int dwStart) {
        this.dwStart = dwStart;
    }

    public int getDwLength() {
        return dwLength;
    }

    public void setDwLength(final int dwLength) {
        this.dwLength = dwLength;
    }

    public int getDwSuggestedBufferSize() {
        return dwSuggestedBufferSize;
    }

    public void setDwSuggestedBufferSize(final int dwSuggestedBufferSize) {
        this.dwSuggestedBufferSize = dwSuggestedBufferSize;
    }

    public int getDwQuality() {
        return dwQuality;
    }

    public void setDwQuality(final int dwQuality) {
        this.dwQuality = dwQuality;
    }

    public int getDwSampleSize() {
        return dwSampleSize;
    }

    public void setDwSampleSize(final int dwSampleSize) {
        this.dwSampleSize = dwSampleSize;
    }

    public int getRcFrameLeft() {
        return rcFrameLeft;
    }

    public void setRcFrameLeft(final int rcFrameLeft) {
        this.rcFrameLeft = rcFrameLeft;
    }

    public int getRcFrameTop() {
        return rcFrameTop;
    }

    public void setRcFrameTop(final int rcFrameTop) {
        this.rcFrameTop = rcFrameTop;
    }

    public int getRcFrameRight() {
        return rcFrameRight;
    }

    public void setRcFrameRight(final int rcFrameRight) {
        this.rcFrameRight = rcFrameRight;
    }

    public int getRcFrameBottom() {
        return rcFrameBottom;
    }

    public void setRcFrameBottom(final int rcFrameBottom) {
        this.rcFrameBottom = rcFrameBottom;
    }
}
