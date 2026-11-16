package domain.model;

/** Stable persisted raw-counter names; percentages are calculated by PlayStats. */
public enum Statistic {
    HANDS, VPIP, PFR, THREE_BET, THREE_BET_OPPORTUNITY,
    POSTFLOP_BETS, POSTFLOP_RAISES, POSTFLOP_CALLS, POSTFLOP_FOLDS,
    FLOP_CBET, FLOP_CBET_OPPORTUNITY, FOLD_TO_CBET, FACED_CBET,
    SAW_FLOP, SHOWDOWN, SHOWDOWN_PAYOUT
}
