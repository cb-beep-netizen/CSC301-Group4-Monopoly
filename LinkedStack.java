package monopoly;

//Simple Stack implementation using Nodes
public class LinkedStack<T> {
 private Node<T> top;

 public void push(T data) {
     Node<T> newNode = new Node<>(data);
     newNode.next = top;
     top = newNode;
 }

 public T pop() {
     if (top == null) return null;
     T data = top.data;
     top = top.next;
     return data;
 }

 public boolean isEmpty() {
     return top == null;
 }
}