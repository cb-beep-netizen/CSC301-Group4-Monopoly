package monopoly;

public class Reverse {
    // Mandatory recursive method for T1 Reverse
    public static <T> void reverse(LinkedQueue<T> q) {
        if (q.isEmpty()) {
            return;
        }
        T frontItem = q.dequeue();
        reverse(q);
        q.enqueue(frontItem);
    }
    
}