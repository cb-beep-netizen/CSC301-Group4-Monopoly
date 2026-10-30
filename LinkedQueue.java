package monopoly;

import java.util.NoSuchElementException;

public class LinkedQueue<T> {
    private static class QNode<T> {
        T data;
        QNode<T> next;
        QNode(T data) { this.data = data; }
    }

    private QNode<T> front = null;
    private QNode<T> rear = null;
    private int size = 0;

    public boolean isEmpty() {
        return front == null;
    }

    public int size() {
        return size;
    }

    public void enqueue(T item) {
        QNode<T> newNode = new QNode<>(item);
        if (isEmpty()) {
            front = newNode;
            rear = newNode;
        } else {
            rear.next = newNode;
            rear = newNode;
        }
        size++;
    }

    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty.");
        }
        T data = front.data;
        front = front.next;
        if (front == null) {
            rear = null;
        }
        size--;
        return data;
    }

    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty.");
        }
        return front.data;
    }
}