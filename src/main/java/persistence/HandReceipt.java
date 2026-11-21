package persistence;

import application.HandCompletion;
import domain.model.Statistic;
import java.util.Set;
import java.util.TreeMap;

/** Deterministic saved payload used to distinguish a retry from a conflicting hand. */
final class HandReceipt {
    private HandReceipt() { }

    static String encode(HandCompletion hand) {
        StringBuilder receipt = new StringBuilder();
        field(receipt, hand.getSessionId());
        receipt.append('|');
        for (var entry : new TreeMap<>(hand.getAccounts()).entrySet()) {
            encodeAccount(receipt, hand.getHandId(), entry.getKey(), entry.getValue());
        }
        receipt.append('|');
        for (var entry : new TreeMap<>(hand.getBotChanges()).entrySet()) {
            field(receipt, entry.getKey());
            field(receipt, entry.getValue());
        }
        return receipt.toString();
    }

    private static void encodeAccount(StringBuilder receipt, String handId, String id,
            HandCompletion.AccountChange change) {
        requireAccountResult(handId, id, change);
        field(receipt, id);
        field(receipt, change.getBalanceBefore());
        field(receipt, change.getStartingStack());
        field(receipt, change.getEndingStack());
        for (Statistic statistic : Statistic.values()) {
            field(receipt, change.getStatistics().getCounts().get(statistic));
        }
    }

    private static void requireAccountResult(String handId, String id,
            HandCompletion.AccountChange change) {
        if (id.isBlank() || !id.equals(change.getAccountId())
                || !change.getStatistics().getCompletedHandIds().equals(Set.of(handId))) {
            throw new IllegalArgumentException("Account and completed hand identities must match.");
        }
        if (change.getStartingStack() < 0 || change.getStartingStack() > change.getBalanceBefore()
                || change.getEndingStack() < 0) {
            throw new IllegalArgumentException("Table stacks must fit the account funds.");
        }
    }

    private static void field(StringBuilder receipt, Object value) {
        String text = value.toString();
        receipt.append(text.length()).append(':').append(text);
    }
}
