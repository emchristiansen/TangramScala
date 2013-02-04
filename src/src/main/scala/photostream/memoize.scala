package photostream

/////////////////////////////////////////////////////////

object Memoize {
  type MemoizeCache[A] = Map[() => A, A]

  implicit class Memoizer[A](cache: MemoizeCache[A]) {
    def memoize(key: => A): Tuple2[A, MemoizeCache[A]] = {
      val result = cache.getOrElse(() => key, key)
      (result, cache + ((() => key) -> result))
    }
  }

  type MemoizeCacheAny = Map[Manifest[_], MemoizeCache[_]]

  implicit class MemoizerAny(cache: MemoizeCacheAny) {
    def memoize[A: Manifest](key: => A): Tuple2[A, MemoizeCacheAny] = {
      val manifest = implicitly[Manifest[A]]
      val innerCache = cache.getOrElse(
        manifest,
        Map[() => Any, Any]()).asInstanceOf[MemoizeCache[A]]
      val (result, newInnerCache) = innerCache.memoize(key)

      (result, cache + (manifest -> newInnerCache))
    }
  }
}