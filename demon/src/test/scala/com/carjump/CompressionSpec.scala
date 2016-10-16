/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carjump

import com.carjump.compression.{ Compressor, Decompressor }
import org.scalatest.{ MustMatchers, WordSpecLike }

//akka.testkit.TestKit(ActorSystem("carjump"))
class CompressionSpec extends WordSpecLike with MustMatchers {

  "compression and decompressor" should {

    "handle strings 0" in {
      val actual = Vector("aaa", "aaa", "aaa")
      val r = Compressor(actual)
      actual.mustBe(Decompressor(r))
    }

    "handle strings 1" in {
      val actual = Vector("aaa", "aaa", "aaa", "bbb")
      val r = Compressor(actual)
      actual mustBe Decompressor(r)
    }

    "handle chars" in {
      val actual = Vector('a', 'a', 'a', 'b')
      val r = Compressor(actual)
      actual mustBe Decompressor(r)
    }

    "handle ints" in {
      val actual = Vector(1, 1, 1, 2, 45)
      val r = Compressor(actual)
      actual mustBe Decompressor(r)
    }

    "handle doubles" in {
      val actual = Vector(1.1, 1.4, 1.89, 2.1, -45.2)
      val r = Compressor(actual)
      actual mustBe Decompressor(r)
    }

    /*"is Vector a IndexedSeq" in {
      val r = Vector(1, 2, 3) match {
        case _: IndexedSeqLike[Int, _] ⇒ true
        case _                         ⇒ false
      }
      r mustBe true
    }*/
  }
}