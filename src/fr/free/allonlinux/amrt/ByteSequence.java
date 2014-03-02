package fr.free.allonlinux.amrt;

public class ByteSequence implements CharSequence {

    private final byte[] data;
    private final int length;
    private final int offset;

    public ByteSequence(byte[] data) {
        this(data, 0, data.length);
    }

    public ByteSequence(byte[] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public int length() {
        return this.length;
    }

    @Override
    public char charAt(int index) {
        return (char) (data[offset + index] & 0xff);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new ByteSequence(data, offset + start, end - start);
    }

}