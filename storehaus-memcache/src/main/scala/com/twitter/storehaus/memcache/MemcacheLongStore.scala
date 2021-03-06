/*
 * Copyright 2013 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.storehaus.memcache

import com.twitter.algebird.Monoid
import com.twitter.bijection.NumericInjections
import com.twitter.util.Duration
import com.twitter.finagle.memcached.Client
import com.twitter.storehaus.ConvertedStore
import com.twitter.storehaus.algebra.MergeableStore
import org.jboss.netty.buffer.ChannelBuffer

/**
 *  @author Doug Tangren
 */
object MemcacheLongStore extends NumericInjections {
  // Long => String => ChannelBuffer <= String <= Long
  private [memcache] implicit val LongInjection =
    long2String.andThen(MemcacheStringStore.StringInjection)

  def apply(client: Client, ttl: Duration = MemcacheStore.DEFAULT_TTL, flag: Int = MemcacheStore.DEFAULT_FLAG) =
    new MemcacheLongStore(MemcacheStore(client, ttl, flag))
}
import MemcacheLongStore._

/** A MergeableStore for Long values backed by memcache */
class MemcacheLongStore(underlying: MemcacheStore) 
  extends ConvertedStore[String, String, ChannelBuffer, Long](underlying)(identity)
  with MergeableStore[String, Long] {

  val monoid = implicitly[Monoid[Long]]

  /** Merges a key by incrementing by a Long value. This operation
   *  has no effect if there was no previous value for the provided key */
  override def merge(kv: (String, Long)) =
    underlying.client.incr(kv._1, kv._2).unit
}

