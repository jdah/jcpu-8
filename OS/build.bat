@ECHO off

echo "Building kernel..."
java -jar Assembler.jar src/kernel.asm -o bin/kernel.bin -v -ds0 dumps/stage0.asm -ds1 dumps/stage1.asm -ds2 dumps/stage2.asm -ds3 dumps/stage3.asm

pause