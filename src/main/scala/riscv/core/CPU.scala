// mycpu is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util.Cat
import riscv.CPUBundle
import riscv.Parameters

class CPU extends Module {
  val io = IO(new CPUBundle)

  val regs       = Module(new RegisterFile)
  val IF         = Module(new InstructionFetch)
  val id         = Module(new InstructionDecode)
  val ex         = Module(new Execute)
  val mem        = Module(new MemoryAccess)
  val wb         = Module(new WriteBack)

  //final project
  val csr_regs   = Module(new CSR)
  val clint      = Module(new CLINT)
  //final end

  io.regs_debug_read_data             := regs.io.debug_read_data
  io.csr_regs_debug_read_data         := csr_regs.io.debug_reg_read_data
  regs.io.debug_read_address          := io.regs_debug_read_address
  csr_regs.io.debug_reg_read_address  := io.csr_regs_debug_read_address

  io.deviceSelect := mem.io.memory_bundle.address(Parameters.AddrBits - 1, Parameters.AddrBits - Parameters.SlaveDeviceCountBits)

  IF.io.jump_address_id       := ex.io.if_jump_address
  IF.io.jump_flag_id          := ex.io.if_jump_flag
  //final project
  IF.io.interrupt_assert          := clint.io.interrupt_assert
  IF.io.interrupt_handler_address := clint.io.interrupt_handler_address
  //final end
  IF.io.instruction_valid     := io.instruction_valid
  IF.io.instruction_read_data := io.instruction
  io.instruction_address      := IF.io.instruction_address

  regs.io.write_enable  := id.io.reg_write_enable
  regs.io.write_address := id.io.reg_write_address
  regs.io.write_data    := wb.io.regs_write_data
  regs.io.read_address1 := id.io.regs_reg1_read_address
  regs.io.read_address2 := id.io.regs_reg2_read_address

  regs.io.debug_read_address := io.regs_debug_read_address
  io.regs_debug_read_data         := regs.io.debug_read_data

  id.io.instruction          := IF.io.instruction

  //final project csr
  csr_regs.io.read_address_id   := id.io.ex_csr_address
  csr_regs.io.write_address_id  := id.io.ex_csr_address
  csr_regs.io.write_enable_id   := id.io.ex_csr_write_enable
  csr_regs.io.write_data_exe    := ex.io.csr_write_data_exe
  //csr_regs.io.exe_csrrd_csr     :=
  csr_regs.io.clint_csr_bundle  <> clint.io.csr_bundle

  //final end
  //final project clint
  clint.io.Interrupt_Flag         := io.Interrupt_Flag
  clint.io.IF_Instruction_Address := IF.io.instruction_address

  clint.io.Instruction            := IF.io.instruction
  clint.io.jump_address           := ex.io.if_jump_address
  clint.io.jump_flag              := ex.io.if_jump_flag

  //final end

  // lab3(cpu) begin
  ex.io.instruction   		:= id.io.instruction
  ex.io.instruction_address	:= IF.io.instruction_address
  ex.io.reg1_data		:= regs.io.read_data1
  ex.io.reg2_data		:= regs.io.read_data2
  ex.io.immediate 		:= id.io.ex_immediate 
  ex.io.aluop1_source		:= id.io.ex_aluop1_source
  ex.io.aluop2_source 		:= id.io.ex_aluop2_source
  // lab3(cpu) end
  // final project
  ex.io.exe_read_data_csr    := csr_regs.io.exe_csrrd_csr
  //final end

  mem.io.alu_result          := ex.io.mem_alu_result
  mem.io.reg2_data           := regs.io.read_data2
  mem.io.memory_read_enable  := id.io.memory_read_enable
  mem.io.memory_write_enable := id.io.memory_write_enable
  mem.io.funct3              := IF.io.instruction(14, 12)

  io.memory_bundle.address := Cat(
    0.U(Parameters.SlaveDeviceCountBits.W),
    mem.io.memory_bundle.address(Parameters.AddrBits - 1 - Parameters.SlaveDeviceCountBits, 0)
  )
  io.memory_bundle.write_enable  := mem.io.memory_bundle.write_enable
  io.memory_bundle.write_data    := mem.io.memory_bundle.write_data
  io.memory_bundle.write_strobe  := mem.io.memory_bundle.write_strobe
  mem.io.memory_bundle.read_data := io.memory_bundle.read_data

  wb.io.instruction_address := IF.io.instruction_address
  wb.io.alu_result          := ex.io.mem_alu_result
  wb.io.memory_read_data    := mem.io.wb_memory_read_data
  wb.io.regs_write_source   := id.io.wb_reg_write_source
  //final project
  wb.io.csr_read_data       := csr_regs.io.exe_csrrd_csr
}
