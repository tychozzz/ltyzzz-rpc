package com.ltyzzz.core.common;

import java.io.Serializable;
import java.util.Arrays;

import static com.ltyzzz.core.constants.RpcConstants.MAGIC_NUMBER;

public class RpcProtocol implements Serializable {

    private static final long serialVersionUID = 5454104442521781820L;

    private short magicNumber = MAGIC_NUMBER;

    private int contentLength;

    private byte[] content;

    public RpcProtocol(byte[] content) {
        this.contentLength = content.length;
        this.content = content;
    }

    @Override
    public String toString() {
        return "RpcProtocol{" +
                ", contentLength=" + contentLength +
                ", content=" + Arrays.toString(content) +
                '}';
    }

    public short getMagicNumber() {
        return magicNumber;
    }

    public void setMagicNumber(short magicNumber) {
        this.magicNumber = magicNumber;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
