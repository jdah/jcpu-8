; Graphics controller, built on top of Raspberry Pi interface

; Graphics cursor character is _
#define GRAPHICS_CURSOR_CHARACTER 0x5F

; Print a string at the current cursor location
; PARAMS: A = string pointer high, B = string pointer low
; RETURN: None
#graphics_print_str:
	push c
	push d
	push e
	push a
	push b

	; Move the 'cursor' to the appropriate location
	lc a, 0x00
	lc b, 0x00
	lwa c, CursorX.H, CursorX.L
	call rpi_translate_x

	lwa c, CursorY.H, CursorY.L
	call rpi_translate_y

	jmp graphics_print_str__loop

#graphics_print_str__loop:
	mw a, h
	mw b, l
	lw e					; Load the next character of the string

	mw e, c
	equ c, 0x00				; Is it a terminating byte?
	jnz c, graphics_print_str__done

	; Prepare to draw the charcter on screen
	push a
	push b

	; Draw the character
	mw e, a
	call rpi_draw_sprite

	; Add 8 to the cursor location
	lc a, 0x00
	lc b, 0x00
	lc c, 0x08
	call rpi_translate_x

	pop b
	pop a

	; Increment the string pointer
	call int_16_inc
	jmp graphics_print_str__loop

#graphics_print_str__done:
	call rpi_translate_reset

	pop b
	pop a
	pop e
	pop d
	pop c
	ret

; Initialise the graphics (Calculate terminal things)
#graphics_init:
	push c 
	push d

	; Calculate the height and the width of the screen in characters (Size / 8)
	call rpi_graphics_width
	lc c, 0x00
	lc d, 0x08
	call int_16_div
	mov WidthChars.H, WidthChars.L, b

	call rpi_graphics_height
	call int_16_div
	mov HeightChars.H, HeightChars.L, b

	pop c
	pop d
	ret

; Draws a charcter on the screen at the specified location
; PARAMS: A = x coordinate, B = y coordinate, c = character
; RETURN: None
#graphics_draw_char:
	push a
	push b
	push c
	push d

	; Translate by 8 * X coordinate
	push b
	mw a, b
	lc a, 0x00
	lc c, 0x00
	lc d, 0x08
	call int_16_mul

	mw b, c
	mw a, b
	lc a, 0x00
	call rpi_translate_x
	pop b

	; Now translate by 8 * Y coordinate
	lc a, 0x00
	lc c, 0x00
	lc d, 0x08
	call int_16_mul

	mw b, c
	mw a, b
	lc a, 0x00
	call rpi_translate_y

	pop d
	pop c
	pop b
	pop a

	; Draw the charcter on the screen
	push a
	mw c, a
	call rpi_draw_sprite
	pop a

	; Set translation back to 0, 0
	call rpi_translate_reset

	ret

; Scroll the terminal down by one character
; PARAMS: None
; RETURN: None
#graphics_scroll:
	push a
	push b

	; Move the video memory up by 8 pixels, effectively scrolling the terminal up
	lc a, 0x01
	lc b, 0x08
	call rpi_graphics_move_x

	; Set the position of the cursor to be on the last line of the screen
	mov a, HeightChars.H, HeightChars.L
	dec a
	mov CursorY.H, CursorY.L, a

	; And set the X coordinate of the controller to zero
	lc a, 0x00
	mov CursorX.H, CursorX.L, a

	pop b
	pop a
	ret

; Moves the cursor to the right one space, and down if it exceeds the size of the screen
; PARAMS: None
; RETURN: None
#graphics_inc_cursor:
	push b
	push c

	; Increment the cursor X
	mov b, CursorX.H, CursorX.L
	mov c, WidthChars.H, WidthChars.L
	inc b
	mov CursorX.H, CursorX.L, b

	; Check if the cursor is at the edge of the screen
	equ b, c
	jnz b, graphics_inc_cursor__inc_y

	pop c
	pop b
	ret

#graphics_inc_cursor__inc_y:
	lc b, 0x00
	mov CursorX.H, CursorX.L, b

	; Increment the cursor Y
	mov b, CursorY.H, CursorY.L
	mov c, HeightChars.H, HeightChars.L
	inc b
	mov CursorY.H, CursorY.L, b

	; Check if the cursor Y is at the end of the screen
	equ b, c
	jnz b, graphics_inc_cursor__scroll

	pop c
	pop b
	ret

#graphics_inc_cursor__scroll:
	call graphics_scroll
	pop c
	pop b
	ret

; The screen doesn't support terminal sizes greater than 256x256
#db CursorX 0x00
#db CursorY 0x00

; Terminal width and height in characters
#db WidthChars 0x00
#db HeightChars 0x00