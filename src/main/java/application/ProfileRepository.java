package application;

import domain.model.StoredProfile;
import java.io.IOException;
import java.util.List;

/** Atomically stores identities and credentials; the application serializes writer access. */
public interface ProfileRepository {
    List<StoredProfile> load() throws IOException;

    void save(List<StoredProfile> profiles) throws IOException;
}
