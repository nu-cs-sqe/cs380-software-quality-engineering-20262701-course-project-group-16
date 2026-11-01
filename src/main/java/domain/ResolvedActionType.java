package domain;

/** Engine-resolved action semantics; an all-in is not a separate category. */
public enum ResolvedActionType {
    FORCED_POST,
    CHECK,
    CALL,
    BET,
    RAISE,
    FOLD
}
