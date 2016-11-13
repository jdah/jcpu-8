# jcpu-8
An 8-bit CPU with a custom architecture, assembler, and emulator. Designed to be implemented with TI 74XX circuits.

Assembler, Raspberry Pi interface, and other source code for the best homebrew minicomputer.

This project includes (or will include) the assembler, emulator, schematic, wiring diagram, and operating system source
code of my homebrew minicomputer. The computer itself will be built out of 7400 series integrated circuits, along with
some RAM and ROM chips not from the 7400 series. It will include a RISC processor complete with 16 instructions,
32 KiB of SRAM, 32 KiB of EEPROM, and a fully functional terminal-style operating system with a whopping 256 colors. 
The processor has a word size of 8 bits and an address bus size of 16 bits. It will use four memory-mapped registers 
to communicate with a Raspberry Pi so that it can output graphics, read data from a disk, accept keyboard input, and
possibly even communicate with other computers over the internet.

# Files and folders
Assember            - JCPU-8 assembler

Emulator            - JCPU-8 emulator

OS                  - JCPU-8 operating system

jcpuasm.tmbundle    - Syntax highlighting files

JCPU.circ           - Logisim .CIRC schematic

jcpudoc.txt         - Original design document