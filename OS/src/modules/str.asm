; ASCII string operations

; Compare two strings
; PARAMS: AB = string pointer one, CD = string pointer two
; RETURN: E != 0 if strings are equal
#str_compare:
	push a
	push b
	push c
	push d

	; Use E as our counter
	lc e, 0x00
	jmp str_compare__loop

#str_compare__loop:
	; Load our two string bytes into A and E
	push a
	mw a, h
	mw b, l
	lw a
	mw c, h
	mw b, l
	lw e

	; If they aren't equal, then we know that we're done
	equ a, e
	jz f, str_compare__not_equal

	; Check if A is a null terminator
	equ a, 0x00
	jnz f, str_compare__check_other

	; If A isn't a null terminator and E is a null terminator, then we're done
	equ e, 0x00
	jnz f, str_compare__not_equal

	pop a

	; Increment the string pointers
	call int_16_inc
	push a
	push b
	mw c, a
	mw d, b
	call int_16_inc
	mw a, c
	mw b, d
	pop b
	pop a

	; And go back to the start of the loop
	jmp str_compare__loop

#str_compare__check_other:
	equ e, 0x00
	jnz f, str_compare__equal

	; If they aren't both null terminators, then we're done
	jmp str_compare__not_equal

#str_compare__equal:
	pop a
	pop d
	pop c
	pop b
	pop a
	lc e, 0x01
	ret

#str_compare__not_equal:
	pop a
	pop d
	pop c
	pop b
	pop a
	lc e, 0x00
	ret

; Compute the length of a string
; PARAMS: AB = string pointer
; RETURN: E = length
#str_length:
	push a
	push b
	lc e, 0x00
	jmp str_length__loop

#str_length__loop:
	push a
	mw a, h
	mw b, l
	lw a
	equ a, 0x00
	jnz f, str_length__done
	pop a
	inc e
	jmp str_length__loop

#str_length__done:
	pop a
	pop b
	pop a
	ret

; Find the position of a character in a string
; PARAMS: AB = string pointer, C = char to find
; RETURN: E = 1 + character position, 0 if not found
#str_find_char:
	push a
	push b
	push d
	lc e, 0x00
	jmp str_find_char__loop

#str_find_char__loop:
	mw a, h
	mw b, l
	lw d
	equ d, c
	jnz f, str_find_char__done

	mw a, h
	mw b, l
	lw d
	equ d, 0x00
	jnz f, str_find_char__failed

	inc e
	call int_16_inc
	jmp str_find_char__loop

#str_find_char__failed:
	lc e, 0x00
	pop d
	pop b
	pop a
	ret

#str_find_char__done:
	inc e
	pop d
	pop b
	pop a
	ret