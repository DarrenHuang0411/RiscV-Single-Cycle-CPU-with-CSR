package peripheral

import chisel3._
import chisel3.util._
import riscv.Parameters

class Timer extends Module {
  val io = IO(new Bundle {
    val bundle = new RAMBundle

    val Timer_interrupt_Flag = Output(Bool())

    val debug_limit = Output(UInt(Parameters.DataWidth))
    val debug_enabled = Output(Bool())
  })

  val count = RegInit(0.U(32.W))
  val limit = RegInit(100000000.U(32.W))
  val enabled = RegInit(true.B)


  io.debug_limit    := limit
  io.debug_enabled  := enabled

  //final 
  //finish the read-write for count,limit,enabled. And produce appropriate Timer_interrupt_Flag
  when(enabled){
    count :=  count + 1.U
    when(count === limit){
      count := 0.U
      io.Timer_interrupt_Flag := true.B
    }
    .otherwise{
      io.Timer_interrupt_Flag := false.B
    }
  }
  .otherwise{
    io.Timer_interrupt_Flag   := false.B
  }

}