; Start of ROM for the JCPU

#org 0x0000

; Add in each function here so that each call is stored in memory here and can be defined in an external file
; Warning: DO NOT MOVE THESE unless everything depending on them is changed as well
#os_calls:
	jmp boot_main					; 0x0000 - This must remain here, as it must be the first instruction in the ROM
	jmp rpi_upload_pixel			; 0x0005
	jmp rpi_gen_sprite				; 0x000A
	jmp rpi_del_sprite				; 0x000F
	jmp rpi_begin_build_sprite		; 0x0014
	jmp rpi_stop_build_sprite		; 0x0019
	jmp rpi_draw_sprite				; 0x001E
	jmp rpi_keyboard_has_next		; 0x0023
	jmp rpi_keyboard_next			; 0x0028
	jmp rpi_keyboard_clear_buffer 	; 0x002D
	jmp rpi_translate_x				; 0x0032
	jmp rpi_translate_y				; 0x0037
	jmp rpi_translate_reset			; 0x003C
	jmp rpi_storage_addr_high		; 0x0041
	jmp rpi_storage_addr_low		; 0x0046
	jmp rpi_storage_read			; 0x004B
	jmp rpi_storage_write			; 0x0050
	jmp rpi_storage_size_mb			; 0x0055
	jmp rpi_graphics_width			; 0x005A
	jmp rpi_graphics_height			; 0x005F
	jmp rpi_graphics_clear			; 0x0064
	jmp rpi_graphics_color			; 0x0069
	jmp rpi_graphics_end_color		; 0x006E
	jmp rpi_graphics_move_x			; 0x0073
	jmp rpi_graphics_move_y			; 0x0078

	jmp int_16_equ					; 0x007D
	jmp int_16_gtn					; 0x0082
	jmp int_16_ltn					; 0x0087
	jmp int_16_inc					; 0x008C
	jmp int_16_dec					; 0x0091
	jmp int_16_add					; 0x0096
	jmp int_16_sub					; 0x009B
	jmp int_16_mul					; 0x00A0
	jmp int_16_div					; 0x00A5
	jmp int_16_mod					; 0x00AA

	jmp graphics_print_str			; 0x00AF

#boot_main:
	lda 0xFF, 0x09				; Start out by setting up the stack
	sw 0x81						; Set the stack up 256 bytes into RAM as the first 256 bytes of RAM are reserved
	lda 0xFF, 0x08
	sw 0xFF

	lc a, 0x02
	lc b, 0xEF
	lc c, 0x00
	lc d, 0x06
	call int_16_mul

	jmp $						; Infinite loop - This should never be reached

#db TestString "Hello, emulator world!"

; -----------------------------
; KERNEL MODULES FROM ./MODULES
; -----------------------------
#include "modules/rpi.asm"
#include "modules/graphics.asm"
#include "modules/math.asm"