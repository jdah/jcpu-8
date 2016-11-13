; I'm a comment inside of an included file!

; Some procedure locations

#define os_itoa 			0x00 0x02
#define os_print_string 	0x00 0x04

; And some code
#this_is_a_procedure_inside_of_an_included_file_yay:
    push a      ; Swap A and B, for absolutely no reason
    push b
    pop a
    pop b
    ret