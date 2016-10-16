package com.carjump.agent

import java.lang.instrument.Instrumentation

object Agent {
  private var instrumentation: Option[Instrumentation] = None

  def premain(args: String, inst: Instrumentation): Unit = {
    println("★ ★ ★ ★ ★ ★ Evaluation memory size agent has been activated ★ ★ ★ ★ ★ ★")
    instrumentation = Some(inst)
  }

  def getObjectSize(o: Any): Long =
    instrumentation.map(_.getObjectSize(o)).getOrElse(0l)
}

