; Raspberry Pi interface procedures

; Draws a sprite on the screen
; PARAMS: A = id
; RETURN: None
#rpi_draw_sprite:
	lc f, 0x20					; Command 0x20, DRAW_SPRITE
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, a

	call wait_for_response

	ret

; Generates a sprite ID
; PARAMS: None
; RETURN: A = id, 0xFF if there was an error
#rpi_gen_sprite:
	lc f, 0x01					; Command 0x01, GEN_SPRITE_ID
	mov 0xFF, 0x01, f

	call wait_for_response

	mov a, 0xFF, 0x07			; Place the new sprite ID in A

	ret

; Deletes a sprite ID from the Raspberry Pi
; PARAMS: A = id
; RETURN: None
#rpi_del_sprite:
	lc f 0x02					; Command 0x02, DEL_SPRITE_ID
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, a			; Write the sprite ID to the second output register

	call wait_for_response

	ret

; Start building a sprite
; PARAMS: A = id
; RETURN: None
#rpi_begin_build_sprite:
	lc f, 0x03					; Command 0x03, BUILD_SPRITE
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, a

	call wait_for_response

	ret

; Stop building a sprite
; PARAMS: None
; RETURN: None
#rpi_stop_build_sprite:
	lc f, 0x04					; Command 0x03, END_BUILD_SPRITE
	mov 0xFF, 0x01, f

	call wait_for_response

	ret

; Upload a pixel to the Raspberry Pi for the currently bound sprite
; PARAMS: a = x high, b = x low, c = y high, d = y low, e = color
; RETURN: None
#rpi_upload_pixel:
	lc f, 0x05					; Command 0x05, UPLOAD_PIXEL_LOCX
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, a 			; Start out with the x coordinate command
	mov 0xFF, 0x03, b
	call wait_for_response 		; Wait until our command has been processed

	lc f, 0x06					; Command 0x06, UPLOAD_PIXEL_LOCY
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, c
	mov 0xFF, 0x03, d
	call wait_for_response

	lc f, 0x07					; Command 0x07, UPLOAD_PIXEL_COLOR
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, e 			; Send the color and complete the pixel upload
	call wait_for_response

	ret

; Check if there is anything in the key buffer
; PARAMS: None
; RETURN: A > 0x00 if keyboard buffer is not empty
#rpi_keyboard_has_next:
	lc f, 0x08
	mov 0xFF, 0x01, f
	call wait_for_response

	mov a, 0xFF, 0x07			; Move the response byte into A

	ret

; Get the next key from the key buffer
; PARAMS: None
; RETURN: A = keycode, B = press(0) or release (1)
#rpi_keyboard_next:
	lc f, 0x09
	mov 0xFF, 0x01, f
	call wait_for_response

	mov a, 0xFF, 0x07
	mov b, 0xFF, 0x08

	ret

; Clear the keyboard buffer
; PARAMS: None
; RETURN: None
#rpi_keyboard_clear_buffer:
	lc f, 0x0A
	mov 0xFF, 0x01, f
	call wait_for_response

	ret

; Translates the origin of the screen on the X axis
; PARAMS: A = sign (1 = negative, 0 = positive), B = high, C = low
; RETURN: None
#rpi_translate_x:
	lc f, 0x0E
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, a
	call wait_for_response

	lc f, 0x0B
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, b
	mov 0xFF, 0x03, c
	call wait_for_response

	ret

; Translates the origin of the screen on the Y axis
; PARAMS: A = sign (1 = negative, 0 = positive), B = high, C = low
; RETURN: None
#rpi_translate_y:
	lc f, 0x0E
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, a
	call wait_for_response

	lc f, 0x0C
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, b
	mov 0xFF, 0x03, c
	call wait_for_response

	ret

; Sets both X and Y translations to 0
; PARAMS: None
; RETURN: None
#rpi_translate_reset:
	lc f, 0x0D
	mov 0xFF, 0x01, f
	call wait_for_response

	ret

; Set the high 16 bits of the storage address
; PARAMS: A = high, B = low
; RETURN: None
#rpi_storage_addr_high:
	lc f, 0x0F
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, a
	mov 0xFF, 0x03, b
	call wait_for_response

	ret

; Set the low 16 bits of the storage address
; PARAMS: A = high, B = low
; RETURN: None
#rpi_storage_addr_low:
	lc f, 0x10
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, a
	mov 0xFF, 0x03, b
	call wait_for_response

	ret

; Read 16 bits from the current storage address
; PARAMS: None
; RETURN: A = high byte, B = low byte
#rpi_storage_read:
	lc f, 0x11
	mov 0xFF, 0x01, f
	call wait_for_response

	mov a, 0xFF, 0x07
	mov b, 0xFF, 0x08

	ret

; Write 16 bits to the current storage address
; PARAMS: A = high byte, B = low byte
; RETURN: None
#rpi_storage_write:
	lc f, 0x12
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, a
	mov 0xFF, 0x03, b
	call wait_for_response

	ret

; Get the current storage size in MiB
; PARAMS: None
; RETURN: A = high byte, B = low byte
#rpi_storage_size_mb:
	lc f, 0x13
	mov 0xFF, 0x01, f
	call wait_for_response

	mov a, 0xFF, 0x07
	mov b, 0xFF, 0x08

	ret

; Get the display width
; PARAMS: None
; RETURN: A = high byte, B = low byte
#rpi_graphics_width:
	lc f, 0x17
	mov 0xFF, 0x01, f
	call wait_for_response

	mov a, 0xFF, 0x07
	mov b, 0xFF, 0x08
	
	ret

; Get the display height
; PARAMS: None
; RETURN: A = high byte, B = low byte
#rpi_graphics_height:
	lc f, 0x18
	mov 0xFF, 0x01, f
	call wait_for_response

	mov a, 0xFF, 0x07
	mov b, 0xFF, 0x08
	
	ret

; Clear the display
; PARAMS: None
; RETURN: None
#rpi_graphics_clear:
	lc f, 0x19
	mov 0xFF, 0x01, f
	call wait_for_response
	
	ret

; Override the color of all newly drawn pixels on the display
; PARAMS: A = color
; RETURN: None
#rpi_graphics_color:
	lc f, 0x21
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, a
	call wait_for_response

	ret

; Stop overriding the color of newly drawn pixels
; PARAMS: None
; RETURN: None
#rpi_graphics_end_color:
	lc f, 0x22
	mov 0xFF, 0x01, f
	call wait_for_response
	
	ret

; Move the video memory in pixels on the X axis
; PARAMS: A = sign, B = distance
; RETURN: None
#rpi_graphics_move_x:
	lc f, 0x23
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, a
	mov 0xFF, 0x03, b
	call wait_for_response

	ret

; Move the video memory in pixels on the Y axis
; PARAMS: A = sign, B = distance
; RETURN: None
#rpi_graphics_move_y:
	lc f, 0x24
	mov 0xFF, 0x01, f
	mov 0xFF, 0x02, a
	mov 0xFF, 0x03, b
	call wait_for_response

	ret

; Wait until the byte in the Raspberry Pi response register equals register A
; PARAMS: None
; RETURN: None
#wait_for_response:
	push a						; Get the next response byte and put it in output register 0
	mov a NextResponse
	inc a
	mov NextResponse a
	mov 0xFF, 0x00, a

	push b 						; Save the value of B, as it will be modified to check the response byte
	mov b, 0xFF, 0x06
	equ b, a
	jnz b wait_for_response__done
	jmp wait_for_response

#wait_for_response__done:
	pop b
	pop a
	ret

#db NextResponse 0x00 		; The next byte to be used in response to the Raspberry Pi