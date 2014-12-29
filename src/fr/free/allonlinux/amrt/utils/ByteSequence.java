package fr.free.allonlinux.amrt.utils;

public class ByteSequence implements CharSequence
{
    private final byte[] data;
    private final int length;
    private final int offset;

    public ByteSequence(byte[] i__data)
    {
        this(i__data, 0, i__data.length);
    }

    public ByteSequence(byte[] i__data, int i__offset, int i__length)
    {
        this.data = i__data;
        this.offset = i__offset;
        this.length = i__length;
    }

    @Override
    public int length()
    {
        return this.length;
    }

    @Override
    public char charAt(int i__index)
    {
        return (char) (data[offset + i__index] & 0xff);
    }

    @Override
    public CharSequence subSequence(int i__start, int i__end)
    {
        return new ByteSequence(data, offset + i__start, i__end - i__start);
    }

}