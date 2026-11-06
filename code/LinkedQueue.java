import java.util.ArrayList;

/**
 * LinkedQueue<T> - our own generic FIFO queue on singly linked nodes.
 * Used for the turn order and for the Chance and Community decks.
 * enqueue, dequeue, peek, isEmpty and size are O(1).
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public class LinkedQueue<T> {

    /** Singly linked queue node (data, next). */
    private static class QNode<T> {
        T data;
        QNode<T> next;

        QNode(T data) {
            this.data = data;
        }
    }

    private QNode<T> front;
    private QNode<T> rear;
    private int size;

    public void enqueue(T item) {
        QNode<T> n = new QNode<>(item);
        if (rear == null) {
            front = n;
        } else {
            rear.next = n;
        }
        rear = n;
        size++;
    }

    public T dequeue() {
        if (isEmpty()) {
            throw new IllegalStateException("LinkedQueue.dequeue(): cannot dequeue from an empty queue");
        }
        T item = front.data;
        front = front.next;
        if (front == null) {
            rear = null;
        }
        size--;
        return item;
    }

    public T peek() {
        if (isEmpty()) {
            throw new IllegalStateException("LinkedQueue.peek(): cannot peek at an empty queue");
        }
        return front.data;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    /**
     * Removes the first occurrence of item (by reference) and keeps the order of
     * all other elements. Rotates the queue exactly once using only dequeue and
     * enqueue. Recursive: base case remaining == 0. O(n).
     */
    public boolean remove(T item) {
        return removeRotate(item, size, false);
    }

    private boolean removeRotate(T item, int remaining, boolean removed) {
        if (remaining == 0) {
            return removed;
        }
        T x = dequeue();
        if (!removed && x == item) {
            removed = true;
        } else {
            enqueue(x);
        }
        return removeRotate(item, remaining - 1, removed);
    }

    /**
     * Copies the queue contents, front first, into out. The queue is rotated
     * once and ends in its original order. Recursive. O(n).
     */
    public void copyInto(ArrayList<T> out) {
        copyRotate(out, size);
    }

    private void copyRotate(ArrayList<T> out, int remaining) {
        if (remaining == 0) {
            return;
        }
        T x = dequeue();
        out.add(x);
        enqueue(x);
        copyRotate(out, remaining - 1);
    }
}
