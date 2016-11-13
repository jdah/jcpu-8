; Arithmetic functions for 16 and 32 bit integers as well as 32 bit fixed point decimals
; Function format: <type>_<bit width>_<operation>
; All integers are represented as LITTLE ENDIAN!

; TODO: 32-bit math as well as 32-bit fixed point decimal math

; Checks if two 16 bit integers are equal
; PARAMS: A = high byte one, B = low byte one, C = high byte two, D = low byte two
; RETURN: E != 0 if AB == CD
#int_16_equ:
	push c
	push d

	lc e, 0x00				; By default, they aren't equal

	equ c, a				; Check if the high bytes are equal
	jnz c, int_16_equ__check_low

	pop d
	pop c
	ret

#int_16_equ__check_low:
	equ d, b
	mw d, e					; If D != 0x00, that means that they are equal

	pop d
	pop c
	ret

; Check if one 16 bit integer is greater than another
; PARAMS: A = high byte one, B = low byte one, C = high byte two, D = low byte two
; RETURN: E != 0 if AB > CD
#int_16_gtn:
	mw a, e					; Check if A > C
	gtn e, c
	jnz e, int_16_gtn__true

	mw a, e
	ltn e, c				; Check if A < C
	jnz e, int_16_gtn__false

	; If both of those tests failed, that means that we need to test the low bytes (A == C)
	mw b, e					; Check if B > D
	gtn e, d
	jnz e, int_16_gtn__true

	; If we're here, then AB is less than CD
	lc e, 0x00
	ret

#int_16_gtn__true:
	lc e, 0x01
	ret

#int_16_gtn__false:
	lc e, 0x00
	ret

; Check if one 16 bit integer is less than another
; PARAMS: A = high byte one, B = low byte one, C = high byte two, D = low byte two
; RETURN: E != 0 if AB < CD
#int_16_ltn:
	mw a, e					; Check if A > C
	gtn e, c
	jnz e, int_16_gtn__false

	mw a, e
	equ a, c				; Check if A < C
	jnz e, int_16_gtn__true

	; If both of those tests failed, that means that we need to test the low bytes (A == C)
	mw b, e					; Check if B < D
	ltn e, d
	jnz e, int_16_gtn__true

	; If we're here, then AB is greater than CD
	lc e, 0x00
	ret

#int_16_ltn__true:
	lc e, 0x01
	ret

#int_16_ltn__false:
	lc e, 0x00
	ret

; Increment a 16-bit integer
; PARAMS: A = high byte, B = low byte
; RETURN: AB = AB + 1
#int_16_inc:
	push c

	mw b, c
	equ c, 0xFF				; If the lower byte is at 0xFF, that means we need to zero B and increment A
	jnz f, int_16_inc__low_max

	inc b					; Otherwise, just increment b

	pop c
	ret

#int_16_inc__low_max:
	lc b, 0x00
	inc a

	pop c
	ret

; Decrement a 16-bit integer
; PARAMS: A = high byte, B = low byte
; RETURN: AB = AB - 1
#int_16_dec:
	push c

	mw b, c
	equ c, 0x00				; If the lower byte is at 0x00, that means we need to load it with 0xFF and decrement A
	jnz f, int_16_dec__low_min

	dec b					; Otherwise, just decrement b

	pop c
	ret

#int_16_dec__low_min:
	lc b, 0xFF
	dec a

	pop c
	ret

; Add two 16-bit integers
; PARAMS: A = high byte one, B = low byte one, C = high byte two, D = low byte two
; RETURN: AB = AB + CD
#int_16_add:
	add b, d				; Add the two low bytes
	adc a, c				; Add the two high bytes

	ret

; Subtract one 16-bit integer from another
; PARAMS: A = high byte one, B = low byte one, C = high byte two, D = low byte two
; RETURN: AB = AB - CD
#int_16_sub:
	push e

	mw d, e					; Check if D > B. If so, we need to borrow from the 256's place to subtract
	gtn e, b
	jnz e, int_16_sub__borrow

	sub b, d				; Otherwise, just subtract normally
	sub a, c

	pop e
	ret

; If D > B, subtraction is done here
#int_16_sub__borrow:
	; Uses the format 1DB - F9 = DB + (FF - F9) + 01 to perform a borrow with two 8-bit numbers
	lc e, 0xFF
	sub e, d
	add b, e
	inc b

	sub a, c				; Subtract the high bytes
	dec a					; Decrement A, as 01 was borrowed from it

	pop e
	ret

; Multiply two 16-bit numbers (WARNING: OVERFLOW LEADS TO UNDEFINED BEHAVIOR)
; PARAMS: A = high byte one, B = low byte one, C = high byte two, D = low byte two
; RETURN: AB = AB * CD
#int_16_mul:
	; Multiplication here is essentially done in a loop. Keep adding AB to itself while decrementing CD
	push c
	push d
	push e

	jmp int_16_mul__loop

#int_16_mul__loop:
	; Start by checking to see if CD == 0x0000
	push a
	push b
	lc a, 0x00
	lc b, 0x00
	call int_16_equ
	pop b
	pop a

	jnz e, int_16_mul__done	; If CD == 0x0000, then E != 0x00

	push c
	push d

	mw a, c
	mw b, d
	call int_16_add			; Add AB with itself

	pop d
	pop c

	push a					; Decrement CD
	push b

	mw c, a
	mw d, b
	call int_16_dec
	mw a, c
	mw b, d

	pop b
	pop a

	jmp int_16_mul__loop

#int_16_mul__done:
	pop e
	pop d
	pop c
	ret

; Divide two 16-bit integers
; PARAMS: A = high byte one, B = low byte one, C = high byte two, D = low byte two
; RETURN: AB = AB / CD
#int_16_div:
	push e

	; Zero out the counter
	lda DivCounterH.H, DivCounterH.L
	sw 0x00

	lda DivCounterL.H, DivCounterL.L
	sw 0x00

	jmp int_16_div__loop

#int_16_div__loop:
	call int_16_ltn			; Check to see if AB < CD
	jnz e, int_16_div__done	; If AB < CD, that means that we're done here

	call int_16_equ			; Check to see if AB == CD
	jnz e, int_16_div__equ	; If AB == CD, then increment E once more and return

	; Otherwise, subtract CD from AB again and increment our counter
	call int_16_sub
	
	push a					; Increment the counter
	push b
	mov b, DivCounterL.H, DivCounterL.L
	mov a, DivCounterH.H, DivCounterH.L
	call int_16_inc
	mov DivCounterL.H, DivCounterL.L, b
	mov DivCounterH.H, DivCounterH.L, a
	pop b
	pop a

	jmp int_16_div__loop

#int_16_div__done:
	pop e

	mov b, DivCounterL.H, DivCounterL.L		; Load the counter into AB
	mov a, DivCounterH.H, DivCounterH.L

	ret

#int_16_div__equ:
	push a					; Increment the counter by one for the final division
	push b
	mov b, DivCounterL.H, DivCounterL.L
	mov a, DivCounterH.H, DivCounterH.L
	call int_16_inc
	mov DivCounterL.H, DivCounterL.L, b
	mov DivCounterH.H, DivCounterH.L, a
	pop b
	pop a

	pop e
	ret

; Find the remainder dividing two 16-bit integers
; PARAMS: A = high byte one, B = low byte one, C = high byte two, D = low byte two
; RETURN: AB = AB % CD
#int_16_mod:
	push e

	jmp int_16_mod__loop

#int_16_mod__loop:
	call int_16_ltn			; Check to see if AB < CD
	jnz e, int_16_mod__done	; If AB < CD, that means that we're done here

	call int_16_equ			; Check to see if AB == CD
	jnz e, int_16_mod__equ	; If AB == CD, then increment E once more and return

	; Otherwise, subtract CD from AB
	call int_16_sub

	jmp int_16_mod__loop

#int_16_mod__done:
	pop e					; The remainder is already less than AB
	ret

#int_16_mod__equ:			; The remainder is zero if AB == CD
	pop e
	lc a, 0x00
	lc b, 0x00
	ret

; The counter used in 16-bit division operations
#db DivCounterH 0x00
#db DivCounterL 0x00