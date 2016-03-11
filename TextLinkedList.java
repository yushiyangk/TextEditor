package editor;

/**
 * Created by ngsimyang on 29/2/16.
 */
public class TextLinkedList<T> {
    protected class Node {
        public Node previous;
        public T item;
        public Node next;

        public Node(Node p, T i, Node h){
            previous = p;
            item = i;
            next = h;
        }
    }


    protected Node sentinelA;
    protected Node sentinelB;
    protected Node cursor;
    protected int size;

    /* create an empty linked list */
    public TextLinkedList(){
        size = 0;
        sentinelA = new Node(null,null,null);
        sentinelB = new Node(null,null,null);
        cursor = new Node(sentinelA, null, sentinelB);
        sentinelA.next = sentinelB;
        sentinelB.previous = sentinelA;
    }
    /* Adds an item to the front.*/
    public void addFirst(T x){
        size += 1;
        Node oldFrontNode = sentinelA.next;
        Node newNode = new Node(sentinelA,x,oldFrontNode);
        sentinelA.next = newNode;
        oldFrontNode.previous = newNode;
        cursor.previous = newNode;
        cursor.next = oldFrontNode;
    }
    /*Adds an item to the end. */
    public void addLast(T x){
        size += 1;
        Node OldBackNode = sentinelB.previous;
        Node newNode = new Node(OldBackNode,x,sentinelB);
        sentinelB.previous = newNode;
        newNode.next = sentinelB;
        OldBackNode.next = newNode;
        cursor.previous = newNode;
        cursor.next = sentinelB;
    }

    /*True if empty, false otherwise */
    public boolean isEmpty(){
        if(size == 0){
            return true;
        }
        return false;
    }

    public int size(){
        return size;
    }

    /*Prints the items in the Deque from first to last, separated by a space*/
    public void printDeque(){
        Node p = sentinelA;
        while (p.next != null) {
            System.out.print(p.next.item );
            p.next = p.next.next;
        }

    }

    /*Removes and returns the item at the front of the Deque, if don't exist return null*/
    public T removeFirst(){
        if (sentinelA.next.item == null){
            return null;
        }
        T itemToReturn = sentinelA.next.item;
        sentinelA.next = sentinelA.next.next;
        sentinelA.next.previous = sentinelA;
        cursor.next = sentinelA.next;
        cursor.previous = sentinelA;
        size -= 1;
        return itemToReturn;
    }

    /*{Removes and returns the item at the back of the Deque. If no such item exists, returns null */
    public T removeLast(){
        if (sentinelB.previous.item == null){
            return null;
        }
        T itemToReturn = sentinelB.previous.item;
        sentinelB.previous = sentinelB.previous.previous;
        sentinelB.previous.next = sentinelB;
        cursor.previous = sentinelB.previous;
        cursor.next = sentinelB;
        size -= 1;
        return itemToReturn;
    }

    public T get(int index){
        Node g = sentinelA.next;
        for (int count = 0; count!=index; count += 1) {
            g = g.next;
        }
        T itemToReturn = g.item;
        return itemToReturn;
    }
}
