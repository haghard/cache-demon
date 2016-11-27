package com.carsharing

import scala.annotation.tailrec

package object compression {

  //Algebra
  sealed trait Compressed[+A]
  case class Single[A](element: A) extends Compressed[A]
  case class Repeat[A](count: Int, element: A) extends Compressed[A]

  /**
   *
   * @tparam A
   */
  trait Compressor[A] {
    def compress(in: Seq[A]): Seq[Compressed[A]]
  }

  /**
   *
   * @tparam A
   */
  trait Decompressor[A] {
    def decompress(in: Seq[Compressed[A]]): Seq[A]
  }

  /**
   *
   */
  object Decompressor {
    def apply[T: Decompressor](in: Seq[Compressed[T]]): Seq[T] = implicitly[Decompressor[T]].decompress(in)

    private[this] def convert[T](in: Seq[Compressed[T]]): Seq[T] = {
      in.foldLeft(Vector[T]()) { (acc, c) ⇒
        c match {
          case Single(str)        ⇒ acc :+ str
          case Repeat(count, str) ⇒ (0 until count).foldLeft(acc)((acc, c) ⇒ acc :+ str)
        }
      }
    }

    implicit def strings = new Decompressor[String] {
      override def decompress(in: Seq[Compressed[String]]): Seq[String] = convert(in)
    }

    implicit def chars = new Decompressor[Char] {
      override def decompress(in: Seq[Compressed[Char]]): Seq[Char] = convert(in)
    }

    implicit def ints = new Decompressor[Int] {
      override def decompress(in: Seq[Compressed[Int]]): Seq[Int] = convert(in)
    }

    implicit def doubles = new Decompressor[Double] {
      override def decompress(in: Seq[Compressed[Double]]): Seq[Double] = convert(in)
    }
  }

  /**
   *
   */
  object Compressor {
    def apply[T: Compressor](in: Seq[T]): Seq[Compressed[T]] = implicitly[Compressor[T]].compress(in)

    @tailrec
    private[this] def loop[T](pos: Int, limit: Int, line: Seq[T], acc: Vector[Compressed[T]]): Seq[Compressed[T]] = {
      if (limit == -1) Vector.empty[Compressed[T]]
      else {
        val ch = line(pos)
        val (count, moreAvailable) = go(ch, line, pos + 1, limit, 0)

        if (moreAvailable) loop(pos + count + 1, limit, line, acc :+ Repeat(count + 1, ch))
        else if (count > 0) acc :+ Repeat(count + 1, ch)
        else acc :+ Single(ch)
      }
    }

    @tailrec
    private[this] def go[T](ch: T, line: Seq[T], pos: Int, limit: Int, count: Int): (Int, Boolean) = {
      if (pos <= limit) {
        if (ch == line(pos)) go(ch, line, pos + 1, limit, count + 1)
        else (count, true)
      } else (count, false)
    }

    //type class instances
    implicit def chars = new Compressor[Char] {
      override def compress(in: Seq[Char]): Seq[Compressed[Char]] =
        loop(0, in.size - 1, in, Vector[Compressed[Char]]())
    }

    implicit def strings = new Compressor[String] {
      override def compress(in: Seq[String]): Seq[Compressed[String]] =
        loop(0, in.size - 1, in, Vector[Compressed[String]]())
    }

    implicit def ints = new Compressor[Int] {
      override def compress(in: Seq[Int]): Seq[Compressed[Int]] =
        loop(0, in.size - 1, in, Vector[Compressed[Int]]())
    }

    implicit def doubles = new Compressor[Double] {
      override def compress(in: Seq[Double]): Seq[Compressed[Double]] =
        loop(0, in.size - 1, in, Vector[Compressed[Double]]())
    }
  }
}