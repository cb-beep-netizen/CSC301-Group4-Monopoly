/**
 * Node<T> - one node of the circular doubly linked list used for the board.
 * Fields data, next and prev are exactly as required by the brief (Section 5).
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public class Node<T> {
    public T data;
    public Node<T> next;
    public Node<T> prev;

    public Node(T data) {
        this.data = data;
    }
}
