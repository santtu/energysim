package fi.iki.santtu.energysim.model

/**
  * This file contains code that is copied from other sources and
  * not written by me.
  */

import scala.annotation.tailrec
import scala.math.{exp, log, pow}
import scala.util.Random

object distributions {
  def uniform() = Random.nextDouble()
  def gaussian() = Random.nextGaussian()

  // the code below is adapted from breeze.stats.distributions.Beta, licensed
  // under Apache 2.0 license
  def gamma(shape: Double, scale: Double): Double = {
    if(shape == 1.0) {
      scale * -math.log(uniform)
    } else if (shape < 1.0) {
      // from numpy distributions.c which is Copyright 2005 Robert Kern (robert.kern@gmail.com) under BSD
      @tailrec
      def rec: Double = {
        val u = uniform()
        val v = -math.log(uniform())
        if (u <= 1.0 - shape) {
          val x = pow(u, 1.0 / shape)
          if (x <= v)  x
          else rec
        } else {
          val y = -log((1-u)/shape)
          val x = pow(1.0 - shape + shape*y, 1.0 / shape)
          if (x <= (v + y)) x
          else rec
        }
      }

      scale * rec
    } else {
      // from numpy distributions.c which is Copyright 2005 Robert Kern (robert.kern@gmail.com) under BSD
      val d = shape-1.0/3.0
      val c = 1.0 / math.sqrt(9.0* d)
      var r = 0.0
      var ok = false
      while (!ok) {
        var v = 0.0
        var x = 0.0
        do {
          x = gaussian()
          v = 1.0 + c * x
        } while(v <= 0)

        v = v*v*v
        val x2 = x * x
        val u = uniform()
        if (  u < 1.0 - 0.0331 * (x2 * x2)
          || log(u) < 0.5*x2 + d* (1.0 - v+log(v))) {
          r = (scale*d*v)
          ok = true
        }
      }
      r
    }
  }

  // the code below is adapted from breeze.stats.distributions.Beta, licensed
  // under Apache 2.0 license
  def beta(a: Double, b: Double): Double = {
    // from tjhunter, a corrected version of numpy's rk_beta sampling in mtrand/distributions.c
    if(a <= .5 && b <= .5) {
      while (true) {
        val U = uniform()
        val V = uniform()
        if (U > 0 && V > 0) {
          // Performing the computations in the log-domain
          // The exponentiation may fail if a or b are really small
          //        val X = math.pow(U, 1.0 / a)
          val logX = math.log(U) / a
          //        val Y = math.pow(V, 1.0 / b)
          val logY=  math.log(V) / b
          val logSum = log(exp(logX) + exp(logY))
          if (logSum <= 0.0) {
            return math.exp(logX - logSum)
          }
        } else {
          throw new RuntimeException("Underflow!")
        }
      }
      throw new RuntimeException("Shouldn't be here.")
    } else if(a <= 1 && b <= 1) {
      while (true) {
        val U = uniform()
        val V = uniform()
        if (U > 0 && V > 0) {
          // Performing the computations in the log-domain
          // The exponentiation may fail if a or b are really small
          val X = math.pow(U, 1.0 / a)
          val Y = math.pow(V, 1.0 / b)
          val sum = X + Y
          if (sum <= 1.0) {
            return X / sum
          }
        } else {
          throw new RuntimeException("Underflow!")
        }
      }
      throw new RuntimeException("Shouldn't be here.")
    } else {
      val ad = distributions.gamma(a, 1)
      val bd = distributions.gamma(b, 1)
      ad / (ad + bd)
    }
  }
}