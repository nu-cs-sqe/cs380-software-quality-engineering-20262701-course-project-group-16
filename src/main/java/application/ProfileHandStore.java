package application;

import java.io.IOException;

/** Publishes all account effects for a completed hand together, at most once per hand ID. */
public interface ProfileHandStore {
    void saveHand(ProfileHandSettlement hand) throws IOException;
}
