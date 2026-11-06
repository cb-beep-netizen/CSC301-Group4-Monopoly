/**
 * QueueTwists (added for twists T1 and T7) - the two recursive queue methods
 * the brief asks for in Appendix A. Both use only the public queue operations
 * enqueue, dequeue and isEmpty. No other list, array or stack is used: the
 * call stack of the recursion is the only temporary storage.
 * AI log: Conversation 03, Prompt 03.02 (Claude). Reviewed and understood by: ______
 */
public final class QueueTwists {

    private QueueTwists() {
    }

    /**
     * T1 Reverse: reverses the order of q.
     * Base case: q is empty - nothing to reverse.
     * Recursive case: take the front element x, reverse the rest, then put x
     * at the back. x was first, so it ends up last; by induction the rest is
     * already reversed in front of it.
     * Terminates because each call removes one element, so the queue reaches
     * empty after size() calls (depth = size, at most 5 players here).
     * Cost: O(n) operations, O(n) call-stack frames.
     */
    public static <T> void reverse(LinkedQueue<T> q) {
        if (q.isEmpty()) {
            return;
        }
        T x = q.dequeue();
        reverse(q);
        q.enqueue(x);
    }

    /**
     * T7 Recall: moves k elements from the front of q to the back, one at a
     * time, keeping their order.
     * Base case: k <= 0 (or an empty queue) - nothing left to move.
     * Recursive case: dequeue the front, enqueue it at the back, then
     * rotate(q, k - 1).
     * Terminates because k falls by 1 each call.
     * Cost: O(k).
     */
    public static <T> void rotate(LinkedQueue<T> q, int k) {
        if (k <= 0 || q.isEmpty()) {
            return;
        }
        q.enqueue(q.dequeue());
        rotate(q, k - 1);
    }
}
