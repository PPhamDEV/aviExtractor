package com.example.myapplication;

public class StreamFormatVideo {
    private int biSize;
    private int biWidth;
    private int biHeight;
    private int biPlanes;
    private int biBitCount;
    private String biCompression;
    private int biSizeImage;
    private int biXPelsPerMeter;
    private int biYPelsPerMeter;
    private int biClrUsed;
    private int biClrImportant;
    private byte[] dwDVAAuxSrc;
    private byte[] dwDVAAuxCtl;
    private byte[] dwDVAAuxSrc1;
    private byte[] dwDVAAuxCtl1;
    private byte[] dwDVVAuxSrc;
    private byte[] dwDVVAuxCtl;
    private int dwDVReserved; //dwDVReserved[2] : 0,0

    public StreamFormatVideo() {
    }

    public int getBiSize() {
        return biSize;
    }

    public void setBiSize(final int biSize) {
        this.biSize = biSize;
    }

    public int getBiWidth() {
        return biWidth;
    }

    public void setBiWidth(final int biWidth) {
        this.biWidth = biWidth;
    }

    public int getBiHeight() {
        return biHeight;
    }

    public void setBiHeight(final int biHeight) {
        this.biHeight = biHeight;
    }

    public int getBiPlanes() {
        return biPlanes;
    }

    public void setBiPlanes(final int biPlanes) {
        this.biPlanes = biPlanes;
    }

    public int getBiBitCount() {
        return biBitCount;
    }

    public void setBiBitCount(final int biBitCount) {
        this.biBitCount = biBitCount;
    }

    public String getBiCompression() {
        return biCompression;
    }

    public void setBiCompression(final String biCompression) {
        this.biCompression = biCompression;
    }

    public int getBiSizeImage() {
        return biSizeImage;
    }

    public void setBiSizeImage(final int biSizeImage) {
        this.biSizeImage = biSizeImage;
    }

    public int getBiXPelsPerMeter() {
        return biXPelsPerMeter;
    }

    public void setBiXPelsPerMeter(final int biXPelsPerMeter) {
        this.biXPelsPerMeter = biXPelsPerMeter;
    }

    public int getBiYPelsPerMeter() {
        return biYPelsPerMeter;
    }

    public void setBiYPelsPerMeter(final int biYPelsPerMeter) {
        this.biYPelsPerMeter = biYPelsPerMeter;
    }

    public int getBiClrUsed() {
        return biClrUsed;
    }

    public void setBiClrUsed(final int biClrUsed) {
        this.biClrUsed = biClrUsed;
    }

    public int getBiClrImportant() {
        return biClrImportant;
    }

    public void setBiClrImportant(final int biClrImportant) {
        this.biClrImportant = biClrImportant;
    }

    public byte[] getDwDVAAuxSrc() {
        return dwDVAAuxSrc;
    }

    public void setDwDVAAuxSrc(final byte[] dwDVAAuxSrc) {
        this.dwDVAAuxSrc = dwDVAAuxSrc;
    }

    public byte[] getDwDVAAuxCtl() {
        return dwDVAAuxCtl;
    }

    public void setDwDVAAuxCtl(final byte[] dwDVAAuxCtl) {
        this.dwDVAAuxCtl = dwDVAAuxCtl;
    }

    public byte[] getDwDVAAuxSrc1() {
        return dwDVAAuxSrc1;
    }

    public void setDwDVAAuxSrc1(final byte[] dwDVAAuxSrc1) {
        this.dwDVAAuxSrc1 = dwDVAAuxSrc1;
    }

    public byte[] getDwDVAAuxCtl1() {
        return dwDVAAuxCtl1;
    }

    public void setDwDVAAuxCtl1(final byte[] dwDVAAuxCtl1) {
        this.dwDVAAuxCtl1 = dwDVAAuxCtl1;
    }

    public byte[] getDwDVVAuxSrc() {
        return dwDVVAuxSrc;
    }

    public void setDwDVVAuxSrc(final byte[] dwDVVAuxSrc) {
        this.dwDVVAuxSrc = dwDVVAuxSrc;
    }

    public byte[] getDwDVVAuxCtl() {
        return dwDVVAuxCtl;
    }

    public void setDwDVVAuxCtl(final byte[] dwDVVAuxCtl) {
        this.dwDVVAuxCtl = dwDVVAuxCtl;
    }

    public int getDwDVReserved() {
        return dwDVReserved;
    }

    public void setDwDVReserved(final int dwDVReserved) {
        this.dwDVReserved = dwDVReserved;
    }
}
