.data
    dataset1: .word 0x3fcccccd    #1.6
    dataset2: .word 0xbfc00000    #-1.5
    dataset3: .word 0x3fb33333    #1.4
    dataset4: .word 0xbfa66666    #-1.3
    dataset5: .word 0x3f99999a    #1.2
    dataset6: .word 0xbf8ccccd    #-1.1
    array_size: .word 0x00000006
    
    sign: .word 0x80000000
    exp: .word 0x7F800000
    man: .word 0x007FFFFF
    bf16man: .word 0x007F0000
    bf16format: .word 0xFFFF0000
    _EOL: .string "\n"
.text

main:
    # float array[array_size] = {1.6,-1.5,1.4,-1.3,1.2,-1.1};
    addi sp,sp,-20
    lw s0,dataset1    #1.6
    lw s1,dataset2    #-1.5
    lw s2,dataset3    #1.4
    lw s3,dataset4    #-1.3
    lw s4,dataset5    #1.2
    lw s5,dataset6    #-1.1
    lw a0,sign
    lw a1,exp
    lw a2,man
    lw a3,bf16man
    lw a4,bf16format
    sw s0,0(sp)
    sw s1,4(sp)
    sw s2,8(sp)
    sw s3,12(sp)
    sw s4,16(sp)
    sw s5,20(sp)
    lw s11,array_size
    jal ShellSort


ShellSort:
    lw t0,array_size      
    srli t0,t0,1           # int interval = array_size / 2;

Whileinterval:
    beq t0,zero,Print      # while (interval >0)
    add t1,zero,t0         # i = interval

Foriarraysize:
    beq t1,s11,Interval_div2    # i<interval
    add t2,zero,t1              # j = i
    slli s0,t1,2                # temp = array[i] t5 = temp
    add s1,sp,s0
    lw t5,0(s1)

ReadData_Temp_done:
    sub t3,t2,t0                # j = j - interval
    slli s0,t3,2                # array[j-interval], t4 = j-interval
    add s1,sp,s0
    lw t4,0(s1)
    jal fp32_to_bf16            # Flag = BOS(array[j-interval], temp);

WhilejandFlag:
    beq s10,zero,arrayjtotemp    
    bltu t2,t0,arrayjtotemp        # while (j>= interval && Flag == 1)   
    and t4,t4,a4                   # bf16format 
    slli s0,t2,2                   # array[j] = array[j-interval]
    add s1,sp,s0
    sw t4,0(s1)
    sub t2,t2,t0
    jal ReadData_Temp_done

arrayjtotemp:
    and t5,t5,a4   # bf16format
    slli s0,t2,2   # array[j] = temp
    add s1,sp,s0
    sw t5,0(s1)
    addi t1,t1,1   # i++
    jal Foriarraysize

Interval_div2:
    srli t0,t0,1
    jal Whileinterval

#--------------------------------------------------

BOS:
    blt s0,s1,SMALL
    beq s0,s1,Sigsame
    jal BIG                # if (sig1 < sig2)

Sigsame:
    beq s2,s3,Expsame
    beq s0,zero,Exp1       # if (sig1 == 0 && sig1 == sig2)
    jal Exp2               # if (sig1 != 0 && sig1 == sig2)

Expsame:
    beq s0,zero,Man1       # if (sig1 == 0 && sig1 == sig2 && exp1 == exp2)
    jal Man2

Exp1:
    blt s2,s3,BIG          # exp1 > exp2
    jal SMALL

Exp2:
    blt s2,s3,SMALL        # exp1 < exp2
    jal BIG

Man1:
    blt s6,s7,SMALL        # man1 > man2
    jal BIG

Man2:
    blt s6,s7,BIG          # man1 < man2
    jal SMALL

BIG:                        # return 1;
    addi s10,zero,1
    jal WhilejandFlag

SMALL:                      # return 0
    addi s10,zero,0
    jal WhilejandFlag

fp32_to_bf16:
    and s0,a0,t4
    and s1,a0,t5    # sig
    and s2,a1,t4
    and s3,a1,t5    # exp
    and s4,a2,t4
    and s5,a2,t5    # man
    and s6,a3,t4
    and s7,a3,t5    # bf16man
    jal BOS

#--------------------------------------------------

Print:
    lw s0,0(sp)
    lw s1,4(sp)
    lw s2,8(sp)
    lw s3,12(sp)
    lw s4,16(sp)
    lw s5,20(sp)

End:   
    addi sp,sp,20

