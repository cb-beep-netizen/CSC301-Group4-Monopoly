/**
 * Card effects from Appendix B.2, plus the effects of the group's twists:
 * REVERSE (T1 Reverse) and RECALL (T7 Recall). Neither takes an argument.
 * AI log: Conversation 01, Prompt 01.13; twist effects added in Conversation 03, Prompt 03.02 (Claude). Reviewed and understood by: ______
 */
public enum CardEffect {
    MOVE_TO, MOVE_BY, NEAREST, COLLECT, PAY, PAY_EACH, COLLECT_EACH, GO_TO_JAIL, JAIL_FREE,
    REVERSE, RECALL
}
