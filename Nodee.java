package editor;

/**
 * Created by ngsimyang on 7/3/16.
 */
public class Nodee {
    public TextLinkedList.Node next;
    public int count;

    public Nodee(TextLinkedList.Node h, int c) {
        next = h;
        count = c;
    }
}
