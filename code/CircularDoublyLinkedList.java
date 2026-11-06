/**
 * CircularDoublyLinkedList<T> - our own generic circular doubly linked list.
 * The last node's next is the head and the head's prev is the last node.
 * All operations below are O(1); there is no index-based access on purpose.
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public class CircularDoublyLinkedList<T> {
    private Node<T> head;
    private int size;

    /** Appends item after the current last node (just before head). O(1). */
    public void addLast(T item) {
        Node<T> n = new Node<>(item);
        if (head == null) {
            n.next = n;
            n.prev = n;
            head = n;
        } else {
            Node<T> tail = head.prev;
            tail.next = n;
            n.prev = tail;
            n.next = head;
            head.prev = n;
        }
        size++;
    }

    /** Inserts item directly after node and returns the new node. O(1). */
    public Node<T> insertAfter(Node<T> node, T item) {
        if (node == null) {
            throw new IllegalArgumentException("insertAfter: node must not be null");
        }
        Node<T> n = new Node<>(item);
        Node<T> after = node.next;
        n.prev = node;
        n.next = after;
        node.next = n;
        after.prev = n;
        size++;
        return n;
    }

    /** Unlinks node from the ring. If it was the head, head moves to the next node. O(1). */
    public void remove(Node<T> node) {
        if (node == null || head == null) {
            throw new IllegalArgumentException("remove: node is null or list is empty");
        }
        if (size == 1) {
            head = null;
        } else {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            if (node == head) {
                head = node.next;
            }
        }
        node.next = null;
        node.prev = null;
        size--;
    }

    public Node<T> getHead() {
        return head;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
