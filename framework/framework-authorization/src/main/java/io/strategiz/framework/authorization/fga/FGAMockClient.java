package io.strategiz.framework.authorization.fga;

import io.strategiz.framework.authorization.fga.model.TupleKey;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-memory mock implementation of {@link FGAClient} for development and testing.
 *
 * <p>This implementation stores tuples in a thread-safe set in memory. It is automatically used
 * when no other FGAClient bean is present.
 *
 * <p>When ready for production, add the {@code client-openfga} module which provides an
 * implementation using the real OpenFGA SDK. That implementation will take precedence over this
 * mock due to @Primary annotation on the production bean.
 *
 * <p><b>Note:</b> This mock does NOT implement relationship inheritance (e.g., owner implies
 * editor). Each relationship must be explicitly granted. The production OpenFGA implementation will
 * handle relationship hierarchies based on the authorization model.
 */
@Component
public class FGAMockClient implements FGAClient {

  private static final Logger log = LoggerFactory.getLogger(FGAMockClient.class);

  private final Set<TupleKey> tuples = ConcurrentHashMap.newKeySet();

  public FGAMockClient() {
    log.info("Using FGA mock client (in-memory). For production, add client-openfga dependency.");
  }

  @Override
  public boolean check(String user, String relation, String object) {
    boolean result = tuples.contains(new TupleKey(user, relation, object));
    log.debug("FGA check: user={} relation={} object={} -> {}", user, relation, object, result);
    return result;
  }

  @Override
  public void write(String user, String relation, String object) {
    TupleKey tuple = new TupleKey(user, relation, object);
    tuples.add(tuple);
    log.debug("FGA write: user={} relation={} object={}", user, relation, object);
  }

  @Override
  public void delete(String user, String relation, String object) {
    TupleKey tuple = new TupleKey(user, relation, object);
    tuples.remove(tuple);
    log.debug("FGA delete: user={} relation={} object={}", user, relation, object);
  }

  @Override
  public List<String> listObjects(String user, String relation, String type) {
    String prefix = type + ":";
    List<String> result =
        tuples.stream()
            .filter(
                t ->
                    t.user().equals(user)
                        && t.relation().equals(relation)
                        && t.object().startsWith(prefix))
            .map(TupleKey::object)
            .toList();
    log.debug(
        "FGA listObjects: user={} relation={} type={} -> {} results",
        user,
        relation,
        type,
        result.size());
    return result;
  }

  /** Clear all tuples (for testing purposes). */
  public void clear() {
    tuples.clear();
    log.debug("FGA mock client cleared");
  }

  /**
   * Get the current number of tuples (for testing purposes).
   *
   * @return the number of stored tuples
   */
  public int size() {
    return tuples.size();
  }
}
