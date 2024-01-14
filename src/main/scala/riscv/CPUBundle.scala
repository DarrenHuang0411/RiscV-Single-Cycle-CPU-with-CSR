// mycpu is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import chisel3._
import peripheral.RAMBundle

// CPUBundle serves as the communication interface for data exchange between
// the CPU and peripheral devices, such as memory.
class CPUBundle extends Bundle {
  val instruction_address = Output(UInt(Parameters.AddrWidth))
  val instruction = Input(UInt(Parameters.DataWidth))
  val instruction_valid = Input(Bool())
  val Interrupt_Flag = Input(UInt(Parameters.InterruptFlagWidth))
  val memory_bundle = Flipped(new RAMBundle)
  val deviceSelect = Output(UInt(Parameters.SlaveDeviceCountBits.W))
  val regs_debug_read_address = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
  val regs_debug_read_data = Output(UInt(Parameters.DataWidth))
  val csr_regs_debug_read_address = Input(UInt(Parameters.CSRRegisterAddrWidth))
  val csr_regs_debug_read_data = Output(UInt(Parameters.DataWidth))
}
