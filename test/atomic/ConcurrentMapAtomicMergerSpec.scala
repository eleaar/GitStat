package atomic

/**
 * Created by Daniel on 2015-01-08.
 */
class ConcurrentMapAtomicMergerSpec extends AtomicMergerSpec {
  override def createMerger = AtomicMerger.newMapMerger
}
