package exercise1

import chisel3._
import chisel3.util._

/** Use serial multiplication to multiply two numbers
  * 
  * The Multiplier uses bit-serial multiplication to implement multiplication using a series
  * of shift and add operations.  This allows for a slow but very low-cost multiplier.
  * 
  * Multiplier assumes that the inputs will be held valid until the operation is complete.
  * The two inputs A and B will assert ready at different times; B asserts ready just after
  * beginning computation, and A will not assert ready until the computation is complete.
  *
  * @param width Bit width of each input port
  */
class Multiplier(width: Int) extends Module {
  val io = IO(new Bundle {
    val a = Flipped(Decoupled(UInt(width.W)))
    val b = Flipped(Decoupled(UInt(width.W)))
    val z = Decoupled(UInt((2*width).W))
  })
  val z_hold = Reg(UInt((2*width).W))
  val b_hold = Reg(UInt(width.W))
  val shift = Reg(UInt(log2Ceil(width).W))
  val nxtShift = Wire(UInt(log2Ceil(width).W))
  val busy = RegInit(init=0.B)
  val done = RegInit(init=0.B)

  val busy_fc = busy | (done & !io.z.ready)
  io.b.ready := !busy
  io.a.ready := 0.B
  io.z.valid := done

  nxtShift := PriorityEncoder(Cat(b_hold(width-1,1), 0.B))

  when (done & io.z.ready) {
    done := 0.B
  }
  when (!busy_fc & io.b.valid) {
    z_hold := 0.U
    nxtShift := PriorityEncoder(io.b.bits)
    b_hold := io.b.bits >> nxtShift
    shift := nxtShift
    busy := 1.B
  }.elsewhen(busy & !done) {
    z_hold := z_hold + (io.a.bits << shift)

    when ((b_hold === 1.U) || (b_hold === 0.U)) {
      busy := 0.U
      done := 1.B
      io.a.ready := 1.B
      when (b_hold === 0.U) {
        z_hold := 0.U
      }
    }.otherwise {
      b_hold := b_hold >> nxtShift
      shift := shift + nxtShift
    }
  }

  io.z.bits := z_hold
}
