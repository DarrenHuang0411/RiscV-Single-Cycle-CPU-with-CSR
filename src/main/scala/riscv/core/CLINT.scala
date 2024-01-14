package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

object InterruptStatus{
    val None = 0x0.U(8.W)
    val Timer0 = 0x1.U(8.W)
    val Ret = 0xF.U(8.W)
} 

object InterruptEntry{
    val Timer0 = 0x4.U(8.W)
}

object InterruptState{
    val Idle = 0x0.U
    val SyncAssert = 0x1.U
    val AsyncAssert = 0x2.U
    val MRET = 0x3.U
}

object CSRState {
  val Idle      = 0x0.U
  val Traping   = 0x1.U
  val Mret = 0x2.U
}

class CSRDirectAccessBundle extends Bundle{
    val mstatus  = Input(UInt(Parameters.DataWidth))
    val mepc     = Input(UInt(Parameters.DataWidth))
    val mcause   = Input(UInt(Parameters.DataWidth))
    val mtvec    = Input(UInt(Parameters.DataWidth))

    val mstatus_write_data= Output(UInt(Parameters.DataWidth))
    val mepc_write_data= Output(UInt(Parameters.DataWidth))
    val mcause_write_data= Output(UInt(Parameters.DataWidth))

    val direct_write_enable = Output(Bool())
}

class CLINT extends Module{
    val io = IO(new Bundle {
        val Interrupt_Flag = Input(UInt(Parameters.InterruptFlagWidth))

        val Instruction    = Input(UInt(Parameters.InstructionWidth))
        val IF_Instruction_Address = Input(UInt(Parameters.AddrWidth))

        val jump_flag = Input(Bool())
        val jump_address = Input(UInt(Parameters.AddrWidth))

        val interrupt_handler_address = Output(UInt(Parameters.AddrWidth))
        val interrupt_assert = Output(Bool())

        val csr_bundle = new CSRDirectAccessBundle
    })

    // mstatus(3) ==> MIE 
    val interrupt_enable = io.csr_bundle.mstatus(3)

    // Trap: choose Sync/Async
    val instruction_address = Mux(io.jump_flag, io.jump_address, io.IF_Instruction_Address + 4.U)


    when(io.Interrupt_Flag =/= InterruptStatus.None && interrupt_enable){
        io.csr_bundle.mstatus_write_data    := io.csr_bundle.mstatus(31, 4) ## 0.U(1.W) ## io.csr_bundle.mstatus(2, 0)
        io.csr_bundle.mepc_write_data       := io.IF_Instruction_Address + 4.U
        io.csr_bundle.mcause_write_data     := Mux(io.Interrupt_Flag(0), 0x80000007L.U, 0x8000000BL.U)

        io.csr_bundle.direct_write_enable   := true.B
        io.interrupt_assert                 := true.B
        io.interrupt_handler_address        := io.csr_bundle.mtvec
    }
    .elsewhen(io.Instruction === InstructionsRet.mret){
        io.csr_bundle.mstatus_write_data    := io.csr_bundle.mstatus(31, 4) ## io.csr_bundle.mstatus(7) ## io.csr_bundle.mstatus(2, 0)
        io.csr_bundle.mepc_write_data       := io.csr_bundle.mepc
        io.csr_bundle.mcause_write_data     := io.csr_bundle.mcause

        io.csr_bundle.direct_write_enable   := true.B
        io.interrupt_assert                 := true.B
        io.interrupt_handler_address        := io.csr_bundle.mepc
    }
    .otherwise{
        io.csr_bundle.mstatus_write_data    := io.csr_bundle.mstatus
        io.csr_bundle.mepc_write_data       := io.csr_bundle.mepc
        io.csr_bundle.mcause_write_data     := io.csr_bundle.mcause

        io.csr_bundle.direct_write_enable   := false.B
        io.interrupt_assert                 := false.B
        io.interrupt_handler_address        := 0.U
    }

}