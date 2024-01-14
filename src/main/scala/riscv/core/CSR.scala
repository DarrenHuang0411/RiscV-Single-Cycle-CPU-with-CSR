// mycpu is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

// CSRRegister get
object CSRRegister{    
    val MSTATUS	= 0x300.U(Parameters.CSRRegisterAddrWidth)	
    val MIE     = 0x304.U(Parameters.CSRRegisterAddrWidth)
    val MTVEC   = 0x305.U(Parameters.CSRRegisterAddrWidth)
    val MSCRATCH = 0x340.U(Parameters.CSRRegisterAddrWidth)
    val MEPC    = 0x341.U(Parameters.CSRRegisterAddrWidth)	
    val MCAUSE  = 0x342.U(Parameters.CSRRegisterAddrWidth)
    val CycleL  = 0xC00.U(Parameters.CSRRegisterAddrWidth)
    val CycleH  = 0XC80.U(Parameters.CSRRegisterAddrWidth)
}

class CSR extends Module {
  val io = IO(new Bundle() { 

    val read_address_id   = Input(UInt(Parameters.CSRRegisterAddrWidth)) 
    val write_address_id  = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val write_enable_id   = Input(Bool())
    val write_data_exe    = Input(UInt(Parameters.DataWidth))

    val exe_csrrd_csr     = Output(UInt(Parameters.DataWidth))  

    val clint_csr_bundle  = Flipped(new CSRDirectAccessBundle)

    val debug_reg_read_address  = Input(UInt(Parameters.CSRRegisterAddrWidth))
    val debug_reg_read_data = Output(UInt(Parameters.DataWidth))

  })

//initialization
  val mstatus   = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mie       = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mtvec     = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mscratch  = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mepc      = RegInit(UInt(Parameters.DataWidth), 0.U)
  val mcause    = RegInit(UInt(Parameters.DataWidth), 0.U)
  val cycles    = RegInit(UInt(64.W), 0.U)

// connect to preset register
  val regLUT =
    IndexedSeq(
      CSRRegister.MSTATUS -> mstatus,
      CSRRegister.MIE     -> mie,
      CSRRegister.MTVEC   -> mtvec,
      CSRRegister.MSCRATCH-> mscratch,
      CSRRegister.MEPC    -> mepc,
      CSRRegister.MCAUSE  -> mcause,
      CSRRegister.CycleL  -> cycles(31, 0),
      CSRRegister.CycleH  -> cycles(63, 32)
    )

  cycles := cycles+ 1.U

  // If the pipeline and the CLINT are going to read and write the CSR at the same time, let the pipeline write first.
  // This is implemented in a single cycle by passing (write_data_exe) to clint and writing the data from the CLINT to the CSR.
  io.exe_csrrd_csr := MuxLookup(io.read_address_id, 0.U)(regLUT)

  io.debug_reg_read_data := MuxLookup(io.debug_reg_read_address, 0.U)(regLUT)

  io.clint_csr_bundle.mstatus   := mstatus
  io.clint_csr_bundle.mtvec     := mtvec
  io.clint_csr_bundle.mcause    := mcause
  io.clint_csr_bundle.mepc      := mepc

  when(io.clint_csr_bundle.direct_write_enable) {
    mstatus := io.clint_csr_bundle.mstatus_write_data
    mepc    := io.clint_csr_bundle.mepc_write_data
    mcause  := io.clint_csr_bundle.mcause_write_data
  }
  .elsewhen(io.write_enable_id) {
    when(io.write_address_id === CSRRegister.MSTATUS) {
      mstatus := io.write_data_exe
    }
    .elsewhen(io.write_address_id === CSRRegister.MEPC) {
      mepc := io.write_data_exe
    }
    .elsewhen(io.write_address_id === CSRRegister.MCAUSE) {
      mcause := io.write_data_exe
    }                                                                     
  }

  when(io.write_enable_id) {
    when(io.write_address_id === CSRRegister.MIE) {
      mie := io.write_data_exe
    }
    .elsewhen(io.write_address_id === CSRRegister.MTVEC){
      mtvec := io.write_data_exe
    }
    .elsewhen(io.write_address_id === CSRRegister.MSCRATCH) {
      mscratch := io.write_data_exe
    }
  }
}