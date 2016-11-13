; Compute Fibonacci numbers

; Set up the stack at 0x80FF
LDA 0xFF 0x09
SW 0x80
LDA 0xFF 0x08
SW 0xFF
jmp main

; File includes!
#include "testinclude.asm"

#define ITERATIONS 0x05
#org 0x0000

#main:
	lc c, ITERATIONS 		; C is our counter
	lc a, 0x01 				; Load our starting numbers
	lc b, 0x01
	jmp fib_1

#fib_1:
	dec c
	mw c, d
	equ d 0x00
	lda done
	jnz d 					; If the counter is zero, jump to the end
	add a, b 				; Compute the next fibonacci number
	mw a, e
	jmp fib_2

#fib_2:
	dec c                   ; Do the same as above, except reverse A and B
	mw c, d
	equ d 0x00
	lda done
	jnz d
	add b, a
	mw b, e
	jmp fib_1

#done:
    ; Convert the final result to a string
    mw e, a
    mw b, number.H
    mw c, number.L
    call os_itoa

    ; Print the number that we have calculated
	lc a done_string.H
	lc b done_string.L
	call os_print_string

    ; Call a procedure from an included file
    call this_is_a_procedure_inside_of_an_included_file_yay

	lc a number.H
	lc b number.L
	call os_print_string
	jmp $ 					; Infinite loop!

#db done_string         'Fibonacci number calculated!\n Number: ', 0x00

; Reserve 4 bytes to store the final number.
#resb number 4